# Multi-Device Session Validation System - Implementation Guide

## Overview

This document describes the complete implementation of a server-scale session validation system that prevents multiple simultaneous logins for the same user across different devices/PCs.

## Architecture

### Components

1. **OnlineUserSession.java** (Server-side)
   - Data model representing a single user session on a device
   - Tracks: email, token, device ID, IP address, status, timestamps
   - Status values: "ACTIVE", "DISCONNECTED", "LOGGED_OUT"

2. **OnlineUsersManager.java** (Server-side)
   - Singleton that manages online users list
   - Enforces: ONLY ONE ACTIVE session per user email
   - Thread-safe (uses ConcurrentHashMap)
   - Auto-cleanup of expired/logged-out sessions

3. **SessionSyncManager.java** (Client-side)
   - Utility for checking session conflict responses
   - User-friendly error message generation
   - Status checking for login responses

4. **UserController.java** (Server)
   - **handleLogin**: Validates credentials + checks session conflict
   - **handleLogout**: Removes user from online list
   - Helper methods: IP address extraction, device ID generation

5. **ApiClient.java** (Client)
   - **login**: Calls /api/users/login
   - **logout**: Calls /api/users/logout
   - LoginResponse now includes sessionStatus field

## Flow Diagrams

### Scenario 1: First Login (Device A)
```
Device A: User clicks Login
    ↓
ApiClient.login(email, password)
    ↓
POST /api/users/login → UserController.handleLogin()
    ↓
Check credentials ✓
    ↓
Check: isAlreadyInUse(email)? → NO
    ↓
OnlineUsersManager.addOrReplaceSession(email, token, deviceId, ip)
    ↓
Response: { "token": "...", "sessionStatus": "SUCCESS" }
    ↓
Client: SessionManager.setSession(user, token)
    ↓
✅ Login successful
```

### Scenario 2: Second Login (Device B) - BLOCKED
```
Device B: User clicks Login with same email
    ↓
ApiClient.login(email, password)
    ↓
POST /api/users/login → UserController.handleLogin()
    ↓
Check credentials ✓
    ↓
Check: isAlreadyInUse(email)? → YES
    ↓
OnlineUsersManager.getSession(email) → returns existing session
    ↓
Response: {
  "sessionStatus": "ALREADY_IN_USE",
  "thongBao": "Tài khoản đang đăng nhập trên thiết bị khác",
  "existingDeviceId": "192.168.1.100_12345",
  "existingIpAddress": "192.168.1.100"
}
    ↓
Client: SessionSyncManager.isSessionConflict(response)? → YES
    ↓
Show error: "Already logged in on: IP 192.168.1.100"
    ↓
❌ Login blocked
```

### Scenario 3: Logout (Device A)
```
Device A: User clicks Logout
    ↓
ApiClient.logout(token)
    ↓
POST /api/users/logout → UserController.handleLogout()
    ↓
Extract email from token
    ↓
OnlineUsersManager.removeSession(email)
    ↓
Session marked LOGGED_OUT + removed from online list
    ↓
Response: { "thongBao": "Đăng xuất thành công" }
    ↓
Client: SessionManager.logout()
    ↓
✅ Logout successful
    ↓
Device B: Can now login with same email
```

### Scenario 4: Connection Loss (Device A)
```
Device A: WebSocket connection drops / App crashes
    ↓
WebSocket disconnects (no explicit logout)
    ↓
Server detects disconnect after timeout (30 minutes)
    ↓
OnlineUsersManager.handleDisconnect(email)
    ↓
Session status: ACTIVE → DISCONNECTED
    ↓
Session stays in memory for 30 minutes
    ↓
Cleanup task removes after timeout
    ↓
Device B: Can login after 30 minutes (or immediately if wait)
```

## API Endpoints

### 1. User Login
**POST /api/users/login**

Request:
```json
{
  "email": "user@example.com",
  "matKhau": "password123"
}
```

Response - Success (HTTP 200):
```json
{
  "token": "USER_user@example.com_1714123456789",
  "email": "user@example.com",
  "hoTen": "Nguyen Van A",
  "thongBao": "Đăng nhập thành công",
  "sessionStatus": "SUCCESS"
}
```

Response - Session Conflict (HTTP 409):
```json
{
  "sessionStatus": "ALREADY_IN_USE",
  "thongBao": "⚠️ Tài khoản của bạn đang đăng nhập trên thiết bị khác. Vui lòng đăng xuất thiết bị kia trước hoặc chờ kết nối mất (30 phút).",
  "existingDeviceId": "192.168.1.100_12345",
  "existingIpAddress": "192.168.1.100"
}
```

Response - Wrong Credentials (HTTP 401):
```json
{
  "thongBao": "Sai email hoặc mật khẩu"
}
```

### 2. User Logout
**POST /api/users/logout**

Headers:
```
Authorization: Bearer USER_user@example.com_1714123456789
```

Response (HTTP 200):
```json
{
  "thongBao": "Đăng xuất thành công"
}
```

## Client-Side Implementation

### Using SessionSyncManager in LoginAction

```java
public void dangNhap(ActionEvent event, String email, String password) {
    try {
        // ... validation code ...
        
        // Call server login
        LoginResponse response = ApiClient.login(email, password);
        
        // 🔹 NEW: Check session status
        String status = SessionSyncManager.handleLoginResponse(response);
        
        if (status.equals("LOGIN_SUCCESS")) {
            // Login successful - create session
            NguoiDung user = khoLuuTruNguoiDung.layTheoEmail(email);
            String token = TokenUtil.generateToken(user);
            SessionManager.getInstance().setSession(user, token);
            
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.INFORMATION, "Thành công",
                "Đăng nhập thành công!");
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
            
        } else if (status.equals("SESSION_CONFLICT")) {
            // User already logged in on another device
            String conflictMessage = SessionSyncManager.getConflictMessage(response);
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.WARNING, "Lỗi đăng nhập", conflictMessage);
                
        } else if (status.equals("LOGIN_FAILED")) {
            // Wrong credentials
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.WARNING, "Lỗi đăng nhập", 
                response.getThongBao());
        }
        
    } catch (Exception e) {
        logger.error("Login error: " + e.getMessage());
    }
}
```

### Using logout in your controller

```java
public void handleLogout(ActionEvent event) {
    try {
        String token = SessionManager.getInstance().getCurrentToken();
        
        if (token != null) {
            // Call server logout
            ApiClient.logout(token);
        }
        
        // Clear local session
        SessionManager.getInstance().logout();
        
        // Navigate back to login
        HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
        
    } catch (Exception e) {
        logger.error("Logout error: " + e.getMessage());
    }
}
```

## Server-Side Configuration

### OnlineUsersManager Settings

Located in `OnlineUsersManager.java`:

```java
// Maximum time a DISCONNECTED session stays in memory before cleanup
private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes

// Interval for cleanup task to run
private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000;  // 5 minutes
```

**Recommendation**: 
- Increase `SESSION_TIMEOUT_MS` if you want more grace period for reconnection
- Decrease `SESSION_TIMEOUT_MS` if you want faster device availability

## WebSocket Integration (Optional)

The system can broadcast session events in real-time via WebSocket (not fully implemented yet pending WebSocket integration):

```java
// Broadcast login event (in UserController)
broadcastLoginEvent(email, deviceId, ipAddress);

// Sample WebSocket message
{
  "event": "USER_LOGIN",
  "email": "user@example.com",
  "deviceId": "192.168.1.100_12345",
  "timestamp": 1714123456789
}

// Sample logout event
{
  "event": "USER_LOGOUT",
  "email": "user@example.com",
  "timestamp": 1714123456789
}
```

## Database Considerations

**Note**: This implementation does NOT modify the database schema. 

The `OnlineUsersManager` maintains an in-memory list only. If you want persistent session tracking across server restarts:

1. Create a table: `active_sessions(email, token, device_id, ip_address, login_time, status)`
2. Save sessions to DB when added
3. Load sessions from DB on server startup
4. Update DB when sessions change status

## Thread Safety

All components are **thread-safe**:

1. **OnlineUsersManager**: Uses `ConcurrentHashMap` + `synchronized` blocks
2. **OnlineUserSession**: All fields are thread-safe (read/write)
3. **ApiClient**: Uses blocking HTTP (thread-safe by nature)
4. **SessionSyncManager**: Stateless utility (thread-safe)

## Error Handling

### Client-Side Errors

| Error | Cause | Solution |
|-------|-------|----------|
| TOKEN_NULL | Network error | Show: "Cannot connect to server" |
| SESSION_CONFLICT | Already logged in | Show device info + suggestions |
| CREDENTIALS_INVALID | Wrong password | Show: "Invalid email/password" |
| NETWORK_ERROR | Server unreachable | Show: "Check IP/Port of server" |

### Server-Side Errors

| Error | Cause | Action | Response |
|-------|-------|--------|----------|
| Email not found | Invalid email | Return 401 | "Wrong email/password" |
| Wrong password | Invalid password | Return 401 | "Wrong email/password" |
| Already logged in | Same user on another device | Return 409 | Include existing device info |
| Session expired | No activity for 30 mins | Remove from online list | Allow new login |

## Testing Checklist

- [ ] Solo login on Device A → Success
- [ ] Try login on Device B with same account → BLOCKED with error
- [ ] Show existing device info in error message
- [ ] Logout on Device A → Device B can now login
- [ ] Logout on Device B → Can logout via API endpoint
- [ ] Wait 30 minutes for auto-cleanup → Device B can login
- [ ] Crash/kill Device A app → Wait timeout or manual logout needed
- [ ] Multiple concurrent login attempts → Only 1 succeeds
- [ ] Session status in DB updates correctly (if using persistent storage)
- [ ] WebSocket broadcast works (when integrated)

## Troubleshooting

### "Already logged in" error persists even after logout

**Cause**: Session not removed from OnlineUsersManager

**Solution**: 
1. Check that `ApiClient.logout()` is called
2. Verify token format is correct
3. Check logs: `[LoginController] 🚪 User {} logging out...`

### Server returns HTTP 409 but no device info

**Cause**: Device registration failed to capture IP/device ID

**Solution**:
1. Check `getClientIpAddress()` is working (look at logs)
2. Verify HTTP headers contain Client IP
3. Check for proxy setup (may need X-Forwarded-For header)

### Sessions not cleaned up after 30 minutes

**Cause**: Cleanup task not running

**Solution**:
1. Check logs for: `[OnlineUsersManager] ✅ Cleanup task started`
2. Verify cleanup thread is not interrupted
3. Check system resources (memory, threads)

## Performance Notes

- **Memory**: Each session uses ~500 bytes
  - 1000 users = ~500 KB
  - 10000 users = ~5 MB
- **CPU**: Cleanup task runs every 5 minutes (minimal impact)
- **Scalability**: ConcurrentHashMap supports high concurrency

## Security Considerations

1. **Token Format**: Current format `USER_<email>_<timestamp>` is simple but NOT cryptographically secure
   - **Recommendation**: Use JWT with HMAC signature for production
   
2. **IP Spoofing**: Client IP is extracted from headers
   - **Risk**: Can be spoofed via proxy headers
   - **Mitigation**: Validate X-Forwarded-For only from trusted proxies
   
3. **Session Hijacking**: Token visible in memory
   - **Mitigation**: Store token in encrypted form (if needed)

4. **Denial of Service**: No rate limiting on login attempts
   - **Recommendation**: Add rate limiting (e.g., 5 attempts per minute per IP)

## Migration Path

If you want to add this to existing system:

1. **Step 1**: Add `OnlineUserSession` + `OnlineUsersManager` classes
2. **Step 2**: Modify `UserController.handleLogin()` to use manager
3. **Step 3**: Add logout endpoint `handleLogout()`
4. **Step 4**: Modify `ApiClient` to call logout
5. **Step 5**: Update `LoginAction.dangNhap()` to use `SessionSyncManager`
6. **Step 6**: Test all scenarios in testing checklist
7. **Step 7** (Optional): Integrate with WebSocket for real-time updates

## Files Created/Modified

### New Files
- `com.mycompany.server.sessionmanager.OnlineUserSession`
- `com.mycompany.server.sessionmanager.OnlineUsersManager`
- `com.mycompany.utils.SessionSyncManager`

### Modified Files
- `com.mycompany.server.controller.UserController`
  - Added imports, logger, onlineUsersManager field
  - Modified `handleLogin()` with session validation
  - Added `handleLogout()`
  - Added helper methods: `getClientIpAddress()`, `generateDeviceId()`, `broadcastLoginEvent()`, `broadcastLogoutEvent()`

- `com.mycompany.server.dto.LoginResponse`
  - Added fields: sessionStatus, existingDeviceId, existingIpAddress
  - Added getters/setters

- `com.mycompany.utils.ApiClient`
  - Modified `login()` to handle sessionStatus
  - Added `logout()` method
  - Added logging for session events

## Support and Maintenance

### Monitoring

Monitor these logs:
```
[OnlineUsersManager] ✅ User {} logged in on device {}
[OnlineUsersManager] ⚠️ User {} already online on device {}
[OnlineUsersManager] 🚪 User {} logged out
[OnlineUsersManager] 📡 User {} disconnected
[OnlineUsersManager] 🗑️ Cleaned up expired session for user {}
```

### Debug Commands

```java
// Print current online users
OnlineUsersManager.getInstance().printDebug();

// Check if user online
boolean isOnline = OnlineUsersManager.getInstance().isUserOnline("user@example.com");

// Get session history
List<OnlineUserSession> history = 
    OnlineUsersManager.getInstance().getSessionHistory("user@example.com");
```

## Future Enhancements

1. **Device Management UI**
   - Show all logged-in devices
   - Allow remote logout from any device
   
2. **Login Notifications**
   - Notify user when new device logs in
   - Request approval for new logins
   
3. **Geolocation Tracking**
   - Show location of logged-in devices
   - Flag suspicious locations
   
4. **Rate Limiting**
   - Prevent brute-force attacks
   - Temporary account lockout
   
5. **Two-Factor Authentication**
   - Require OTP for new device login
   - Trusted device list

---

**Version**: 1.0  
**Last Updated**: May 15, 2026  
**Author**: AI Assistant

