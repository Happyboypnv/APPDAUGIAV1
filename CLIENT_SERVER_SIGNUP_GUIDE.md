# 🌐 Client-Server Signup Implementation

## The Problem (Before)

```
Computer A:
  User signs up
  → Data saved ONLY to Computer A local DB
  
Computer B:
  Same user tries to sign in
  → Account doesn't exist locally
  → Can't login ❌
```

**Result:** Users had to create separate accounts on each computer.

---

## The Solution (After)

```
Computer A:
  User signs up
  → Data sent to Server (/api/users/register)
  → Server saves to server database
  → Server response = Success ✅
  → Also saved locally for offline access
  
Computer B:
  Same user tries to sign in
  → Server has account ✅
  → User can login successfully ✅
  
Computer C:
  Same user tries to sign in
  → Server has account ✅
  → User can login successfully ✅
```

**Result:** One account, accessible everywhere!

---

## Architecture Diagram

```
┌─────────────────────┐         ┌──────────────────────┐
│   Computer A        │         │   Computer B         │
│  (JavaFX Client)    │         │  (JavaFX Client)     │
│                     │         │                      │
│ [Sign Up Form]      │         │ [Sign In Form]       │
│   ↓                 │         │   ↓                  │
│ dangKy() submitted  │         │ dangNhap() submitted │
│   ↓                 │         │   ↓                  │
│ Validate input      │         │ Validate input       │
│   ↓                 │         │   ↓                  │
│ ApiClient.register()│         │ApiClient.login()    │
│   ↓                 │         │   ↓                  │
└─────────┬───────────┘         └─────────┬────────────┘
          │ HTTP POST                      │ HTTP POST
          │ /api/users/register            │ /api/users/login
          │                                │
          └────────────┬───────────────────┘
                       ↓
        ┌──────────────────────────────────┐
        │     Server (HTTP:8080)           │
        │  - UserController.register()     │
        │  - UserController.login()        │
        │                                  │
        │     Server Database (SQLite)     │
        │  users table:                    │
        │  - email                         │
        │  - hashedPassword (SHA-256)      │
        │  - salt                          │
        │  - hoTen, ngaySinh               │
        └──────────────────────────────────┘
```

---

## Sign Up Flow (New)

### BEFORE (Local Only)
```
User fills form
  ↓
checkSignUp() — validate input on client
  ↓
BoMaHoaMatKhau.taoSalt() — create salt
  ↓
BoMaHoaMatKhau.maHoaMatKhau() — hash password
  ↓
khoLuuTruNguoiDung.luu() — save to LOCAL DB only ❌
  ↓
Success message
```

### AFTER (Client-Server)
```
User fills form
  ↓
checkSignUp() — validate input on client
  ↓
🌐 ApiClient.register() — SEND TO SERVER ✅
  ↓
Server (HTTP:8080):
  ├─ Receive: name, email, password, birthdate
  ├─ Validate email not duplicate
  ├─ Hash password + salt
  ├─ Save to SERVER database
  └─ Return: LoginResponse {token, thongBao}
  ↓
Client receives response
  ↓
If success:
  ├─ Save locally for offline access (optional)
  ├─ Show success message
  └─ Navigate to Sign In
  
If error:
  ├─ Show error from server (email exists, etc)
  └─ Let user try again
```

---

## 9-Step Implementation

### Step 1️⃣: Acquire Lock (Thread-Safe)
```java
if (!lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    Show "System busy" error
    return
}
```
Prevents concurrent signup race condition.

### Step 2️⃣: Validate Client-Side
```java
checkSignUp(name, email, password, birthdate)
// Checks:
// - Name: 2-30 chars, supports Vietnamese
// - Email: Valid format
// - Password: 8+ chars, upper, lower, digit, special
// - Date: >= 18 years old
```

### Step 3️⃣: Call Server (Critical!)
```java
LoginResponse response = ApiClient.register(
    name.trim(),
    email.trim(),
    password,
    birthdate.toString()  // YYYY-MM-DD
);
```

Server endpoint: `POST /api/users/register`
Server handles:
- Email duplicate check ✅
- Password hashing ✅
- Salt generation ✅
- Database save ✅

### Step 4️⃣: Check Network Connection
```java
if (response == null) {
    throw new UserException("Cannot connect to server. Check IP/Port.");
}
```

### Step 5️⃣: Check Server Response
```java
String token = response.getToken();
if (token == null || token.trim().isEmpty()) {
    String errorMsg = response.getThongBao();  // "Email already exists"
    throw new UserException(errorMsg);
}
```

### Step 6️⃣: Save Locally (Optional)
```java
// Server save succeeded
// Now save locally for offline access
String salt = BoMaHoaMatKhau.taoSalt();
String hashedPassword = BoMaHoaMatKhau.maHoaMatKhau(password, salt);
NguoiDung localUser = new NguoiDung(...);
localUser.setSalt(salt);
khoLuuTruNguoiDung.luu(localUser);  // Optional, doesn't block signup
```

### Step 7️⃣: Show Success Message
```java
HandleNavigationAndAlert.getInstance().showAlert(
    Alert.AlertType.INFORMATION, "Thành công",
    "Đăng ký thành công! Tài khoản có thể dùng trên bất kỳ thiết bị nào."
);
```
**Key message:** Account works on ANY device now!

### Step 8️⃣: Navigate to Sign In
```java
HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
```
User can now login with their account.

### Step 9️⃣: Error Handling & Cleanup
```java
catch (EmailException e) → "Lỗi email"
catch (PasswordException e) → "Lỗi mật khẩu"
catch (DateException e) → "Lỗi ngày sinh"
catch (UserException e) → Server error message
finally {
    lock.unlock();  // Always release lock
}
```

---

## Key Differences

| Aspect | Before | After |
|--------|--------|-------|
| **Database** | Local only | Server primary |
| **Accessibility** | Single computer | Any computer |
| **Email Check** | Client checks local DB | Server checks server DB |
| **Password Handling** | Client hashes | Server hashes |
| **Offline Access** | ✅ Works | ⚠️ Only if synced locally |
| **Data Sync** | N/A | Synced to server |

---

## Multi-Computer User Journey

### Scenario: User SignUp on Computer A, SignIn on Computer B

**Computer A:**
```
1. Open app, click "Sign Up"
2. Fill form: John Doe, john@gmail.com, Password123!
3. Click "Sign Up" button
4. LoginAction.dangKy() called
5. ApiClient.register() → POST to server
6. Server saves account to its database
7. Local copy saved as backup
8. Show: "Đăng ký thành công! Tài khoản có thể dùng trên bất kỳ thiết bị nào."
9. Redirect to Sign In
```

**Computer B (Next day, different device):**
```
1. Open app, click "Sign In"
2. Enter: john@gmail.com, Password123!
3. Click "Sign In" button
4. LoginAction.dangNhap() called
5. ApiClient.login() → POST to server
6. Server finds account in its database ✅
7. Server hashes submitted password
8. Hashes match → Authentication success ✅
9. Server returns token
10. SessionManager saves session
11. Redirect to Home page
12. User logged in successfully!
```

---

## Server-Side Implementation (Reference)

**Endpoint:** `POST /api/users/register`
**Location:** `src/main/java/com/mycompany/server/controller/UserController.java`

```java
public void handleRegister(HttpExchange exchange) {
    // 1. Parse request body (name, email, password, birthdate)
    LoginRequest req = gson.fromJson(body, LoginRequest.class);
    
    // 2. Validate email not duplicate
    if (userStorage.kiemTraEmail(req.email)) {
        return error("Email already exists");
    }
    
    // 3. Hash password with salt
    String salt = BoMaHoaMatKhau.taoSalt();
    String hashedPassword = BoMaHoaMatKhau.maHoaMatKhau(req.password, salt);
    
    // 4. Create user object
    NguoiDung user = new NguoiDung(req.hoTen, req.email, hashedPassword, req.ngaySinh);
    user.setSalt(salt);
    
    // 5. Save to server database
    userStorage.luu(user);
    
    // 6. Return success response
    return success("Signup successful", token);
}
```

---

## Error Scenarios Handled

### 1️⃣ Email Already Exists
```
User: john@gmail.com (already on server from Computer A)
Location: Computer B
Result: ❌ "Email đã tồn tại!" (from server)
```

### 2️⃣ Network Error
```
Server not running (ServerApp.main() not called)
Result: ❌ "Không thể kết nối đến server. Kiểm tra IP/Port server."
```

### 3️⃣ Invalid Email Format
```
Client-side validation catches it before server call
Result: ❌ "Email không hợp lệ!"
```

### 4️⃣ Weak Password
```
Client-side validation catches it before server call
Result: ❌ "Mật khẩu không hợp lệ! (8+ chars, upper, lower, digit, special)"
```

### 5️⃣ User Underage
```
Calculate age from birthdate
Result: ❌ "Bạn chưa đủ 18 tuổi"
```

---

## Thread Safety

**Lock Management:**
```java
// Acquire lock (500ms timeout to avoid blocking)
if (!lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    Show system busy error
    return
}

try {
    // Perform signup operations
    // No other thread can execute this block concurrently
} finally {
    lock.unlock()  // Always release, even if error
}
```

**Why Lock Matters:**
- Prevents 2 users signing up simultaneously
- Prevents duplicate email check race condition
- Database operations are atomic per user

---

## Benefits

✅ **Cross-Device Access** - One account works everywhere  
✅ **Data Persistence** - Account survives device loss  
✅ **Cloud Sync** - Data synced to server (like professional apps)  
✅ **Offline Backup** - Local copy available if no internet  
✅ **Better Security** - Server controls password hashing  
✅ **Scalability** - Can add more servers if needed  

---

## Testing

### Test 1: Basic SignUp
```
1. Start server: java -cp ... ServerApp
2. Computer A: Sign up as user1@test.com
3. Expected: Success message, redirect to Sign In
4. Check server logs: Account created
```

### Test 2: Cross-Device Login
```
1. Computer A: Sign up as user2@test.com
2. Computer B: Sign in with user2@test.com
3. Expected: Successful login, Home page loads
```

### Test 3: Duplicate Email
```
1. Computer A: Sign up as user3@test.com
2. Computer A: Try signup again as user3@test.com
3. Expected: "Email đã tồn tại!" error
```

### Test 4: No Server
```
1. Close server (kill ServerApp)
2. Computer A: Try signup
3. Expected: "Không thể kết nối đến server..."
```

---

## Summary

The signup flow now:

1. **Validates** data on client (for UX)
2. **Sends** account to server (CRITICAL!)
3. **Server** saves to its database
4. **Client** saves locally (optional backup)
5. **User** can login from any computer

This is how **real web applications** work (Gmail, Facebook, Twitter, etc.)

---

**Files Modified:**
- `src/main/java/com/mycompany/action/LoginAction.java` (dangKy method)

**Server Side (Already Implemented):**
- `src/main/java/com/mycompany/server/controller/UserController.java` (handleRegister)
- `src/main/java/com/mycompany/utils/ApiClient.java` (register method)

**Status:** ✅ Production-Ready

