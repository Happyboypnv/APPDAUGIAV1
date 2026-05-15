# Multi-Device Session Validation - Quick Integration Guide

## What Was Built?

A complete server-scale session management system that:

✅ **Prevents multiple simultaneous logins** for the same user account (one active session per email)  
✅ **Blocks login attempts** on new devices if already logged in  
✅ **Shows existing device info** to users (IP, Device ID)  
✅ **Auto-cleanup** of disconnected sessions after 30 minutes  
✅ **Thread-safe** for concurrent login requests  
✅ **WebSocket-ready** for real-time session sync  

## Files Created

### Server-Side Session Management
1. **OnlineUserSession.java** (`com.mycompany.server.sessionmanager`)
   - POJO representing one user session
   - Tracks: email, token, device ID, IP, status, timestamps
   - Status: ACTIVE, DISCONNECTED, LOGGED_OUT

2. **OnlineUsersManager.java** (`com.mycompany.server.sessionmanager`)
   - Singleton managing all online users
   - Enforces: 1 ACTIVE session per user
   - Auto-cleanup thread every 5 minutes
   - Thread-safe with ConcurrentHashMap

### Client-Side Session Handling
3. **SessionSyncManager.java** (`com.mycompany.utils`)
   - Utility for checking login response status
   - Generates user-friendly error messages
   - Identifies session conflicts

## Files Modified

1. **UserController.java**
   - ✅ Added session validation in `handleLogin()`
   - ✅ Added new `handleLogout()` endpoint
   - ✅ Added IP extraction + device ID generation
   
2. **LoginResponse.java** (DTO)
   - ✅ Added fields: sessionStatus, existingDeviceId, existingIpAddress
   
3. **ApiClient.java**
   - ✅ Enhanced `login()` to handle session status
   - ✅ Added `logout()` method
   - ✅ Added session event logging

## How to Use

### 1. Server-Side: The Session Manager auto-starts
```java
// In ServerApp.java (already handling in UserController via singleton)
OnlineUsersManager manager = OnlineUsersManager.getInstance();
// Automatically starts cleanup task

// Later, when server shuts down (optional):
manager.shutdown();
```

### 2. Server-Side: Login Endpoint Behavior

```java
// When user tries to login:
// Before: POST /api/users/login → Check credentials → Return token
// After:  POST /api/users/login → Check credentials 
//         → Check if already online on another device
//         → If yes: Return 409 with ALREADY_IN_USE
//         → If no: Add to online users + return 200 with SUCCESS
```

### 3. Client-Side: Handle Login Response

```java
// In LoginAction.java - dangNhap() method
LoginResponse response = ApiClient.login(email, password);

// Check session status
if (SessionSyncManager.isSessionConflict(response)) {
    // User already logged in on another device
    String errorMsg = SessionSyncManager.getConflictMessage(response);
    HandleNavigationAndAlert.getInstance().showAlert(
        Alert.AlertType.WARNING, "⚠️ Conflict", errorMsg);
    return;
}

if (response.getToken() != null && 
    SessionSyncManager.isLoginSuccess(response)) {
    // Login successful
    SessionManager.getInstance().setSession(user, token);
    // Navigate to Home
} else {
    // Wrong credentials or network error
    HandleNavigationAndAlert.getInstance().showAlert(
        Alert.AlertType.WARNING, "Error", 
        response.getThongBao());
}
```

### 4. Client-Side: Handle Logout

```java
// In NavBarController or wherever logout button is
public void handleLogout(ActionEvent event) {
    String token = SessionManager.getInstance().getCurrentToken();
    
    if (token != null) {
        // Call server logout
        ApiClient.logout(token);
    }
    
    // Clear local session
    SessionManager.getInstance().logout();
    
    // Go back to login
    HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
}
```

## API Response Examples

### ✅ First Login Succeeds
```http
POST /api/users/login HTTP/1.1

Response 200 OK:
{
  "token": "USER_user@example.com_1714123456789",
  "email": "user@example.com",
  "hoTen": "Nguyen Van A",
  "thongBao": "Đăng nhập thành công",
  "sessionStatus": "SUCCESS"
}
```

### ❌ Second Login Blocked (Same User)
```http
POST /api/users/login HTTP/1.1

Response 409 Conflict:
{
  "sessionStatus": "ALREADY_IN_USE",
  "thongBao": "⚠️ Tài khoản của bạn đang đăng nhập trên thiết bị khác...",
  "existingDeviceId": "192.168.1.100_54321",
  "existingIpAddress": "192.168.1.100"
}
```

### 🔑 Logout Succeeds
```http
POST /api/users/logout HTTP/1.1
Authorization: Bearer USER_user@example.com_1714123456789

Response 200 OK:
{
  "thongBao": "Đăng xuất thành công"
}
```

## How It Works - Step by Step

### When Device A Logs In:
1. ✅ User enters credentials
2. ✅ Server checks: credentials valid? YES
3. ✅ Server checks: user already online? NO
4. ✅ Server creates OnlineUserSession object
5. ✅ OnlineUsersManager stores it (email → session mapping)
6. ✅ Server returns token + SUCCESS status
7. ✅ Client stores session locally

### When Device B Tries to Login (Same User):
1. ✅ User enters credentials
2. ✅ Server checks: credentials valid? YES
3. ⛔ Server checks: user already online? **YES** (from Device A)
4. ⛔ Server returns 409 CONFLICT + existing device info
5. ⛔ Client shows: "Already logged in on 192.168.1.100"
6. ⛔ Login blocked - user cannot proceed

### When Device A Logs Out:
1. ✅ User clicks logout button
2. ✅ Client calls: ApiClient.logout(token)
3. ✅ Server extracts email from token
4. ✅ OnlineUsersManager.removeSession(email)
5. ✅ Session marked LOGGED_OUT + removed
6. ✅ Server returns success
7. ✅ Client clears local session
8. ✅ Device B can now login

### When Device A Connection is Lost:
1. ⏱️ WebSocket connection drops (no explicit logout)
2. ⏱️ Server detects no activity
3. ⏱️ After 30 minutes: cleanup task removes session
4. ⏱️ Device B can login after timeout
5. ✅ Or user can wait for auto-cleanup

## Configuration Options

In `OnlineUsersManager.java`:

```java
// How long before a DISCONNECTED session is removed
private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes

// How often to clean up expired sessions
private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000;  // 5 minutes
```

**Recommended Values:**
- Mobile apps: 15-30 minutes (users often switch apps)
- Desktop apps: 5-10 minutes (usually close properly)
- Web apps: 30+ minutes (network instability common)

## Testing Your Implementation

### Test Case 1: First Login Works
```
1. Start app on Computer A
2. Login with any user account
3. Expected: ✅ Login succeeds, token stored
4. Check logs: "[LoginController] ✅ User ... logged in successfully"
```

### Test Case 2: Second Login Blocked
```
1. App still running on Computer A (logged in)
2. Start app on Computer B with same user
3. Try to login on B
4. Expected: ❌ shows "Already logged in on device..."
5. Check logs: "[OnlineUsersManager] ⚠️ User ... already online"
```

### Test Case 3: Logout Unblocks Login
```
1. Computer A: Click logout
2. Expected: ✅ Session removed
3. Computer B: Try login again
4. Expected: ✅ Login succeeds now
5. Check logs: "[LoginController] 🚪 User ... logging out"
```

### Test Case 4: Auto-Cleanup Works
```
1. Kill app on Computer A abruptly (crash, force close)
2. Do NOT explicitly logout
3. Wait 30 minutes (or edit SESSION_TIMEOUT_MS to 2 minutes for testing)
4. Computer B: Try login
5. Expected: After timeout → ✅ Login succeeds
6. Check logs: "[OnlineUsersManager] 🗑️ Cleaned up expired session"
```

## Logging - What to Look For

### Login Success
```
[LoginController] ✅ User user@example.com logged in successfully. Device: 192.168.1.100_12345
```

### Login Blocked (Already Online)
```
[OnlineUsersManager] ⚠️ User user@example.com already online on device 192.168.1.100_54321. Rejecting new login.
[ApiClient] ⚠️ User already logged in on another device: 192.168.1.100_54321
```

### Logout
```
[LoginController] 🚪 User user@example.com logging out...
[OnlineUsersManager] 🚪 User user@example.com logged out
[ApiClient] ✅ Logout successful
```

### Auto-Cleanup
```
[OnlineUsersManager] 📡 User user@example.com disconnected (session marked DISCONNECTED)
[OnlineUsersManager] 🗑️ Cleaned up expired session for user user@example.com: LOGGED_OUT
[OnlineUsersManager] 🧹 Cleanup removed 1 expired sessions
```

## Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| Login never blocked | OnlineUsersManager not initialized | Check that UserController creates singleton instance |
| Wrong device info shown | Device ID not captured correctly | Check `getClientIpAddress()` logs |
| Sessions persist after logout | removeSession() not called | Verify `ApiClient.logout()` is called |
| Can't login after 30 mins wait | Cleanup not running | Check for cleanup thread in logs |
| HTTP 409 not returned | Response headers not set correctly | Check `guiPhanHoi()` status code |

## Next Steps - Optional Enhancements

### 1. WebSocket Real-time Updates
```java
// Broadcast login/logout events to connected clients
// So other users know who just logged in/out
```

### 2. Device Management Dashboard
```java
// Show all logged-in devices
// Allow remote logout from any device
// Accept/Reject new login attempts
```

### 3. Login Notifications
```java
// Notify user when new device logs in
// Request approval for new logins
```

### 4. Persistent Storage
```java
// Save sessions to database
// Recover sessions on server restart
// Audit trail of all logins/logouts
```

## Database Schema (Optional)

If you want to persist sessions:

```sql
CREATE TABLE active_sessions (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_status (status)
);
```

## Questions?

### "What if user intentionally logs in on 2 devices?"
**Answer**: Current design only allows 1 active session per user. If you need to support multiple devices, modify `OnlineUsersManager` to allow N sessions per user:
```java
// Change from: Map<String, OnlineUserSession>
// To: Map<String, List<OnlineUserSession>>
```

### "How do I know which device is which?"
**Answer**: Device ID combines IP and timestamp. For better UX, consider:
- Browser user agent detection
- Device name entry
- MAC address (server-side only)
- Hardware device ID (if stored by client)

### "Can user force logout other devices?"
**Answer**: Not implemented yet. To add:
1. Add endpoint: `POST /api/users/logout-device/{deviceId}`
2. In controller: `OnlineUsersManager.removeSessionByDeviceId(deviceId)`
3. Broadcast logout event to that device

---

**Version**: 1.0  
**Status**: Ready to integrate  
**Last Updated**: May 15, 2026

