package com.mycompany.server.sessionmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OnlineUsersManager - Server-side manager for online user sessions
 *
 * MỤC ĐÍCH:
 * - Maintain a server-scale list of currently logged-in users
 * - Track which users are online and on which devices
 * - Prevent multiple simultaneous logins for same user (only 1 active session per user)
 * - Handle session cleanup on logout or disconnect
 * - Broadcast session changes via WebSocket
 *
 * TÍNH NĂNG CHÍNH:
 * - Single-device-per-user: Only one active session per user email
 * - Multi-device support: Previous session gets terminated when new login happens
 * - Session status tracking: ACTIVE, DISCONNECTED, LOGGED_OUT
 * - Automatic cleanup: Remove expired/logged-out sessions
 * - Thread-safe: ConcurrentHashMap for concurrent access
 *
 * LUỒNG HOẠT ĐỘNG:
 * 1. User login on Device A → addOrReplaceSession(email, sessionA)
 *    - Check if user already online on another device
 *    - If yes: Mark old session as "LOGGED_OUT"
 *    - If no: Add new session
 * 2. User tries to login on Device B
 *    - Server detects user already online on Device A
 *    - Check if Device A session is "ACTIVE" or just "DISCONNECTED"
 *    - If ACTIVE: Reject login on Device B (show "Already logged in")
 *    - If DISCONNECTED: Replace with new session (allow login)
 * 3. User logout on Device A → removeSession(email)
 *    - Mark session as LOGGED_OUT
 *    - Remove from online users list
 *    - Broadcast logout event via WebSocket
 * 4. User disconnects (WebSocket close) → handleDisconnect(email)
 *    - Mark session as DISCONNECTED
 *    - Keep session but mark as inactive
 *    - If reconnect within timeout → Re-activate session
 *    - If timeout expires → Remove session
 *
 * DESIGN PATTERN:
 * - Singleton: Only 1 instance per server
 * - Thread-safe: All maps are ConcurrentHashMap
 * - Strategy: Can be extended to add custom session policies
 *
 * CONFIGURATION:
 * - SESSION_TIMEOUT_MS: Time before session is considered expired (default 30 minutes)
 * - CLEANUP_INTERVAL_MS: How often to clean expired sessions (default 5 minutes)
 */
public class OnlineUsersManager {

    private static final Logger logger = LoggerFactory.getLogger(OnlineUsersManager.class);

    // Configuration
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000;  // 5 minutes

    // Singleton instance
    private static volatile OnlineUsersManager instance;
    private static final Object INSTANCE_LOCK = new Object();

    // Map<email, OnlineUserSession>
    // Only ONE session per user email can be ACTIVE at any time
    // ConcurrentHashMap ensures thread-safety for concurrent login requests
    private final Map<String, OnlineUserSession> onlineUsers = new ConcurrentHashMap<>();

    // Map<email, List<OnlineUserSession>>
    // Keep history of all sessions (not currently in use, for audit purposes)
    // Optional: can remove if not needed
    private final Map<String, List<OnlineUserSession>> sessionHistory = new ConcurrentHashMap<>();

    // Thread for periodic cleanup
    private Thread cleanupThread;
    private volatile boolean isRunning = false;

    /**
     * Constructor - Private (Singleton)
     */
    private OnlineUsersManager() {
        startCleanupTask();
    }

    /**
     * getInstance() - Get singleton instance
     *
     * @return OnlineUsersManager singleton
     */
    public static OnlineUsersManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new OnlineUsersManager();
                }
            }
        }
        return instance;
    }

    /**
     * CHECK: Is user already online?
     *
     * @param email User's email
     * @return true if user is online (in ACTIVE state), false if not
     */
    public boolean isUserOnline(String email) {
        OnlineUserSession session = onlineUsers.get(email);
        return session != null && session.getSessionStatus().equals("ACTIVE");
    }

    /**
     * CHECK: Is user already in use (logged in on another device)?
     *
     * LOGIC:
     * - Return true if user is ACTIVE on ANY device
     * - Return false if no active session (user can login)
     * - Return false if only DISCONNECTED/LOGGED_OUT sessions
     *
     * @param email User's email
     * @return true if "alreadyInUse" (active on another device)
     */
    public boolean isAlreadyInUse(String email) {
        OnlineUserSession session = onlineUsers.get(email);
        if (session == null) return false;
        return session.getSessionStatus().equals("ACTIVE");
    }

    /**
     * ADD OR REPLACE: User login
     *
     * LOGIC:
     * 1. Check if user is already online in ACTIVE state
     * 2. If yes: Return OLD session (don't allow new login yet)
     * 3. If no: Create new session and store
     *
     * THREAD-SAFETY:
     * - No global lock, per-user sync to minimize contention
     * - Multiple threads can login different users simultaneously
     * - Only one thread can login same user at a time
     *
     * @param email User's email
     * @param token Session token
     * @param deviceId Unique device identifier
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return Existing ACTIVE session if already logged in, or new session if creating
     */
    public OnlineUserSession addOrReplaceSession(String email, String token,
                                                 String deviceId, String ipAddress,
                                                 String userAgent) {
        // 🔹 STEP 1: Check if user already online (quick non-blocking check)
        OnlineUserSession existingSession = onlineUsers.get(email);

        if (existingSession != null && existingSession.getSessionStatus().equals("ACTIVE")) {
            // User is ACTIVE on another device
            logger.warn("[OnlineUsersManager] ⚠️ User {} already online on device {}. Rejecting new login.",
                    email, existingSession.getDeviceId());
            return existingSession;  // Return existing session (signal to reject login)
        }

        // 🔹 STEP 2: Synchronize only for this user to allow concurrent logins for different users
        synchronized (email.intern()) {  // Using interned string for per-user lock
            // Double-check after acquiring lock (another thread might have added this user)
            existingSession = onlineUsers.get(email);

            if (existingSession != null && existingSession.getSessionStatus().equals("ACTIVE")) {
                logger.warn("[OnlineUsersManager] ⚠️ User {} already online on device {} (2nd check).",
                        email, existingSession.getDeviceId());
                return existingSession;
            }

            // Log out old session if exists (but not active)
            if (existingSession != null) {
                logger.info("[OnlineUsersManager] 🔄 Replacing old session for user {}: {} → {}",
                        email, existingSession.getSessionStatus(), "LOGGED_OUT");
                existingSession.logout();
                // Add to history
                addToHistory(email, existingSession);
            }

            // Create new session
            OnlineUserSession newSession = new OnlineUserSession(email, token, deviceId, ipAddress, userAgent);
            onlineUsers.put(email, newSession);

            logger.info("[OnlineUsersManager] ✅ User {} logged in on device {}",
                    email, deviceId);

            return newSession;
        }
    }

    /**
     * LOGOUT: User explicitly logged out
     *
     * LOGIC:
     * 1. Mark session as LOGGED_OUT
     * 2. Remove from online users map
     * 3. Add to history
     *
     * @param email User's email
     */
    public void removeSession(String email) {
        synchronized (email.intern()) {  // Per-user lock
            OnlineUserSession session = onlineUsers.remove(email);

            if (session != null) {
                session.logout();
                addToHistory(email, session);
                logger.info("[OnlineUsersManager] 🚪 User {} logged out", email);
            }
        }
    }

    /**
     * DISCONNECT: User lost connection (WebSocket closed)
     *
     * LOGIC:
     * 1. Mark session as DISCONNECTED
     * 2. Keep in online map (don't remove yet)
     * 3. After timeout → automatically clean up
     *
     * @param email User's email
     */
    public void handleDisconnect(String email) {
        synchronized (email.intern()) {  // Per-user lock
            OnlineUserSession session = onlineUsers.get(email);

            if (session != null) {
                session.disconnect();
                logger.info("[OnlineUsersManager] 📡 User {} disconnected (session marked DISCONNECTED)", email);
            }
        }
    }

    /**
     * GET: Current session info for a user
     *
     * @param email User's email
     * @return OnlineUserSession or null if not online
     */
    public OnlineUserSession getSession(String email) {
        return onlineUsers.get(email);
    }

    /**
     * GET ALL: List of all currently online users
     *
     * @return List of OnlineUserSession objects
     */
    public List<OnlineUserSession> getAllOnlineUsers() {
        return new ArrayList<>(onlineUsers.values());
    }

    /**
     * COUNT: Total online users
     *
     * @return Number of online users
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * UPDATE ACTIVITY: Mark session as recently active
     *
     * @param email User's email
     */
    public void updateActivity(String email) {
        OnlineUserSession session = onlineUsers.get(email);
        if (session != null) {
            session.updateLastActivity();
        }
    }

    /**
     * CLEANUP: Remove expired sessions
     *
     * Runs periodically (every 5 minutes)
     * Removes sessions that are:
     * - LOGGED_OUT for any amount of time
     * - DISCONNECTED for more than SESSION_TIMEOUT_MS
     */
    private void cleanupExpiredSessions() {
        List<String> toRemove = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, OnlineUserSession> entry : onlineUsers.entrySet()) {
            OnlineUserSession session = entry.getValue();

            // Remove LOGGED_OUT sessions immediately
            if (session.getSessionStatus().equals("LOGGED_OUT")) {
                toRemove.add(entry.getKey());
            }
            // Remove DISCONNECTED sessions after timeout
            else if (session.getSessionStatus().equals("DISCONNECTED")) {
                long timeSinceDisconnect = now - session.getLastActivityTime();
                if (timeSinceDisconnect > SESSION_TIMEOUT_MS) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        // Remove expired sessions
        for (String email : toRemove) {
            OnlineUserSession removed = onlineUsers.remove(email);
            if (removed != null) {
                logger.info("[OnlineUsersManager] 🗑️ Cleaned up expired session for user {}: {}",
                        email, removed.getSessionStatus());
            }
        }

        if (!toRemove.isEmpty()) {
            logger.info("[OnlineUsersManager] 🧹 Cleanup removed {} expired sessions", toRemove.size());
        }
    }

    /**
     * Start periodic cleanup task
     */
    private void startCleanupTask() {
        if (isRunning) return;

        isRunning = true;
        cleanupThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    cleanupExpiredSessions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("[OnlineUsersManager] ❌ Error in cleanup task: " + e.getMessage());
                }
            }
        });
        cleanupThread.setName("SessionCleanup-Thread");
        cleanupThread.setDaemon(true);
        cleanupThread.start();

        logger.info("[OnlineUsersManager] ✅ Cleanup task started (interval: {} ms)", CLEANUP_INTERVAL_MS);
    }

    /**
     * Stop cleanup task (graceful shutdown)
     */
    public void shutdown() {
        isRunning = false;
        if (cleanupThread != null) {
            cleanupThread.interrupt();
        }
        logger.info("[OnlineUsersManager] 🛑 Shutdown called");
    }

    /**
     * Add session to history (for audit purposes)
     */
    private void addToHistory(String email, OnlineUserSession session) {
        sessionHistory.computeIfAbsent(email, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(session);
    }

    /**
     * GET HISTORY: All sessions for a user (for audit)
     *
     * @param email User's email
     * @return List of all sessions for this user
     */
    public List<OnlineUserSession> getSessionHistory(String email) {
        return sessionHistory.getOrDefault(email, new ArrayList<>());
    }

    /**
     * CLEAR HISTORY: Clear session history (use with caution)
     */
    public void clearHistory() {
        sessionHistory.clear();
        logger.warn("[OnlineUsersManager] ⚠️ Session history cleared");
    }

    /**
     * DEBUG: Print current online users
     */
    public void printDebug() {
        logger.info("[OnlineUsersManager] 📊 Online Users Report:");
        logger.info("  Total online users: {}", onlineUsers.size());

        for (Map.Entry<String, OnlineUserSession> entry : onlineUsers.entrySet()) {
            OnlineUserSession session = entry.getValue();
            logger.info("  - {}: {} (device: {}, status: {})",
                    entry.getKey(),
                    session.getIpAddress() != null ? session.getIpAddress() : "unknown",
                    session.getDeviceId(),
                    session.getSessionStatus());
        }
    }
}

