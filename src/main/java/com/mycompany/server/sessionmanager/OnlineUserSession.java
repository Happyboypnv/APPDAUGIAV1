package com.mycompany.server.sessionmanager;

/**
 * OnlineUserSession - Represents a user's login session on a specific device
 *
 * MỤC ĐÍCH:
 * - Track each user's session across multiple devices
 * - Store device-specific information (IP, user agent, token)
 * - Know when user logged in and when last activity occurred
 * - Identify the "alreadyInUse" state for multi-device detection
 *
 * TÍNH NĂNG:
 * - email: user's email (unique identifier)
 * - token: session token returned to client
 * - deviceId: unique device identifier (IP + port + timestamp)
 * - loginTime: timestamp when user logged in
 * - lastActivity: timestamp of last activity (bid, request, etc)
 * - isOnline: whether this session is still active (not logged out)
 * - sessionStatus: "ACTIVE", "DISCONNECTED", "LOGGED_OUT"
 */
public class OnlineUserSession {

    private String email;              // User's email (unique per user)
    private String token;              // JWT token for this session
    private String deviceId;           // Unique device identifier (IP:port or mac address)
    private long loginTime;            // When user logged in (milliseconds)
    private long lastActivityTime;     // Last activity timestamp
    private boolean isOnline;          // Is this session currently online?
    private String sessionStatus;      // "ACTIVE", "DISCONNECTED", "LOGGED_OUT"
    private String userAgent;          // Browser/client info (optional)
    private String ipAddress;          // Client IP address (optional)

    /**
     * Constructor - Full details
     */
    public OnlineUserSession(String email, String token, String deviceId,
                            String ipAddress, String userAgent) {
        this.email = email;
        this.token = token;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = System.currentTimeMillis();
        this.lastActivityTime = System.currentTimeMillis();
        this.isOnline = true;
        this.sessionStatus = "ACTIVE";
    }

    // ===== GETTERS =====

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    // ===== SETTERS =====

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setSessionStatus(String status) {
        this.sessionStatus = status;
    }

    public void updateLastActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Mark session as logged out
     */
    public void logout() {
        this.isOnline = false;
        this.sessionStatus = "LOGGED_OUT";
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Mark session as disconnected (WebSocket connection lost)
     */
    public void disconnect() {
        this.sessionStatus = "DISCONNECTED";
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Check if session is still valid (active or recently disconnected)
     *
     * @param timeoutMs Time in milliseconds before considering session expired
     * @return true if session is still valid
     */
    public boolean isValidSession(long timeoutMs) {
        if (sessionStatus.equals("LOGGED_OUT")) {
            return false;
        }

        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceLastActivity < timeoutMs;
    }

    @Override
    public String toString() {
        return "OnlineUserSession{" +
                "email='" + email + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", sessionStatus='" + sessionStatus + '\'' +
                ", loginTime=" + loginTime +
                ", lastActivityTime=" + lastActivityTime +
                ", isOnline=" + isOnline +
                '}';
    }
}

