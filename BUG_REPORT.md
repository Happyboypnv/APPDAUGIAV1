# 🐛 BUG REPORT - APPDAUGIA

## 📊 Summary
**Total Critical Bugs Found: 8**
- **Thread-Safety Issues: 3**
- **Logic Errors: 3**
- **Resource Management: 1**
- **API Consistency: 1**

---

## 🔴 CRITICAL BUGS

### BUG #1: THREAD-SAFETY ISSUE - Inconsistent Method Calls in AuctionSessionService
**Severity:** 🔴 CRITICAL  
**Location:** `AuctionSessionService.java` lines 40, 53, 78  
**Files Affected:** `src/main/java/com/mycompany/action/AuctionSessionService.java`

**Problem:**
```java
// Line 40: Uses getAuctionSessionId()
synchronized (getLock(auction.getAuctionSessionId())) {

// Line 53: Uses getSessionId() 
synchronized (getLock(auction.getSessionId())) {

// Line 78: Uses getSessionId()
synchronized (getLock(auction.getSessionId())) {
```

The code uses different method names (`getAuctionSessionId()` vs `getSessionId()`) to acquire locks for the same data structure. Even though both methods return the same `sessionId` field, using different references creates **separate lock instances**, breaking thread-safety guarantees.

**Impact:**
- Race conditions between threads
- Data corruption in multithreaded auction scenarios
- Money loss due to concurrent bid updates

**Fix:** Make lock acquisition consistent:
```java
// Use one method consistently - preferably getSessionId()
// Update all calls in AuctionSessionService to use the same method
synchronized (getLock(auction.getSessionId())) {
```

---

### BUG #2: THREAD-SAFETY ISSUE - Inconsistent Locking in AuctionScheduler
**Severity:** 🔴 CRITICAL  
**Location:** `AuctionScheduler.java` lines 36, 46, 54  
**Files Affected:** `src/main/java/com/mycompany/action/AuctionScheduler.java`

**Problem:**
```java
// Line 36: setTimeCancelAuction() uses getAuctionSessionId()
String maPhien = phien.getAuctionSessionId();

// Line 46: cancel() uses getSessionId()
String maPhien = phien.getSessionId();

// Line 54: closeAuction() uses getAuctionSessionId()
String maPhien = phien.getAuctionSessionId();
```

The `scheduledFutures` map is keyed by different method results, causing:
- Task started with key from `getAuctionSessionId()` cannot be found with key from `getSessionId()`
- Scheduled cancellations are never executed
- Auctions run indefinitely after end time

**Impact:**
- Auctions never close automatically
- System resource exhaustion (threads, memory)
- Financial inconsistencies

**Fix:**
```java
// Use consistent identifier - getSessionId() everywhere
String maPhien = phien.getSessionId();
```

---

### BUG #3: RESOURCE LEAK - Connection Not Closed in LoginAction
**Severity:** 🔴 CRITICAL  
**Location:** `LoginAction.java` line 146  
**Files Affected:** `src/main/java/com/mycompany/action/LoginAction.java`

**Problem:**
```java
// Line 146: Missing space causing syntax issue
checkSignIn(email, password);  // space missing before call
```

More critically, there's a missing `InterruptedException` error handler for the lock acquisition timeout that doesn't properly clean up resources:

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    // ❌ MISSING: lock.unlock() if lock was acquired!
}
```

If `InterruptedException` is thrown after `lock.tryLock()` succeeds, the lock is never released, causing deadlock for next login attempt.

**Impact:**
- Subsequent login attempts hang indefinitely
- Users cannot log in after one interrupted login
- System deadlock

**Fix:**
```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    if (lock.tryLock()) {  // or better: track lock state
        try {
            // cleanup
        } finally {
            lock.unlock();
        }
    }
}
```

---

### BUG #4: LOGIC ERROR - Incorrect Bidder List Access in AuctionSessionService
**Severity:** 🔴 CRITICAL  
**Location:** `AuctionSessionService.java` lines 131-132  
**Files Affected:** `src/main/java/com/mycompany/action/AuctionSessionService.java`

**Problem:**
```java
List<User> bidders = auction.getBidderList();  // Line 125: Get list BEFORE adding
double oldPrice = auction.getCurrentPrice();

auction.addBidder(bidder);  // Line 129: ADD to list

if (bidders.size() >= 1) {  // Line 131: size refers to SNAPSHOT
    User previousLeader = bidders.get(bidders.size() - 1);  // BUG: Gets wrong user
```

The code calls `getBidderList()` which returns a **copy** of the now-outdated list (see `AuctionSession.java` line 161):

```java
public List<User> getBidderList() {
    synchronized (bidderList) {
        return new ArrayList<>(bidderList);  // ❌ Returns NEW list (copy)
    }
}
```

After `addBidder()` is called, the `bidders` local variable still references the OLD copy, not the updated list.

**Impact:**
- Refunds go to wrong bidder
- Auction bidding logic is broken
- Users lose money due to incorrect refunds

**Example Scenario:**
```
Initial state: bidderList = [UserA]
1. bidders = getBidderList()  → bidders = [UserA]
2. addBidder(UserB)  → bidderList = [UserA, UserB]
3. bidders.size() = 1 (still referring to old copy!)
4. previousLeader = bidders.get(0) = UserA ✓ (correct by accident)
5. bidders.get(bidders.size()-1) = bidders.get(0) = UserA (WRONG - should be UserB in new list)
```

**Fix:**
```java
// Option 1: Get fresh list after add
List<User> bidders = auction.getBidderList();
double oldPrice = auction.getCurrentPrice();
auction.addBidder(bidder);
bidders = auction.getBidderList();  // Refresh!

// Option 2: Don't use getBidderList() copy, use direct logic
User previousLeader = null;
if (auction.getBidderList().size() > 0) {
    previousLeader = auction.getBidderList().get(
        auction.getBidderList().size() - 1
    );
}
```

---

### BUG #5: LOGIC ERROR - Incorrect Size Check Before Add in AuctionSessionService
**Severity:** 🟡 HIGH  
**Location:** `AuctionSessionService.java` line 131  
**Files Affected:** `src/main/java/com/mycompany/action/AuctionSessionService.java`

**Problem:**
```java
List<User> bidders = auction.getBidderList();  // BEFORE add, size = N
auction.addBidder(bidder);                     // NOW add, size becomes N+1
if (bidders.size() >= 1) {  // Line 131: Size of OLD snapshot!
    User previousLeader = bidders.get(bidders.size() - 1);  // Index: size-1 = N-1
```

The comparison `bidders.size() >= 1` is checking size BEFORE the new bidder was added.

**Correct Logic Should Be:**
- If there are existing bidders BEFORE adding new one → refund the previous leader
- Current code: If copy contains >= 1 bidders → refund last bidder (wrong snapshot)

**Impact:**
- First bidder is refunded incorrectly (should not have any previous bidder)
- Logic doesn't match intent

---

### BUG #6: INCONSISTENT API USAGE - Mixed Method Names in codebase
**Severity:** 🟡 HIGH  
**Location:** Multiple files
**Files Affected:**
- `AuctionSessionService.java` (lines 40, 53, 78)
- `AuctionScheduler.java` (lines 36, 46, 54)
- `AuctionSessionRegistry.java` (line 30, 34)
- `AuctionRepositorySQLite.java` (lines 56, 57, 91, 176, 262)
- `AuctionWebSocketServer.java`

**Problem:**
Class `AuctionSession` has TWO methods that do the same thing:
```java
public String getSessionId() {       // Line 139
    return this.sessionId;
}

public String getAuctionSessionId() { // Line 143
    return this.sessionId;
}
```

Code inconsistently uses both names, making it confusing and error-prone.

**Impact:**
- Code maintainability issues
- Developers don't know which method to use
- Leads to bugs when method names are mixed up

**Fix:** Remove duplicate method
```java
// Keep only one:
public String getSessionId() {
    return this.sessionId;
}
// Or rename for clarity:
public String getAuctionSessionId() {
    return this.sessionId;
}
```

---

### BUG #7: INCONSISTENCY - Missing Avatar Path Initialization
**Severity:** 🟡 MEDIUM  
**Location:** `User.java` line 15-29
**Files Affected:** `src/main/java/com/mycompany/models/User.java`

**Problem:**
User class doesn't have `avatarPath` field initialization:
```java
public User(String fullName, String email,
            String password, String dateOfBirth,
            String address, String phoneNumber) {
    super(fullName, email, password, dateOfBirth);
    this.address = address;
    this.phoneNumber = phoneNumber;
    this.availableBalance = 0;
    this.transactions = new ArrayList<>();
    // ❌ Missing: this.avatarPath initialization
}
```

Field is used elsewhere (DatabaseConnection.java, UserRepositorySQLite.java) but never properly initialized.

**Impact:**
- NullPointerException when accessing avatarPath
- UI display of user avatar fails

---

### BUG #8: TIMING ISSUE - Anti-sniping Duration Calculation
**Severity:** 🟡 MEDIUM  
**Location:** `AuctionSessionService.java` line 107-112  
**Files Affected:** `src/main/java/com/mycompany/action/AuctionSessionService.java`

**Problem:**
```java
long thoiGianConLai = Duration.between(now, auction.getEndTime()).getSeconds();

if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
    auction.setEndTime(auction.getEndTime().plusSeconds(30));
    auctionScheduler.setTimeCancelAuction(auction); // Reschedule
}
```

Issues:
1. **Race condition:** `now` is captured at line 106, but by line 110 when endTime is extended, time has passed
2. **Accuracy:** Seconds precision lost in anti-sniping mechanism
3. **Multiple extends:** If multiple bids come in quick succession, endTime extends indefinitely

**Example:**
```
T=0s: Bid 1 at T=58s remaining → extends to T=88s
T=2s: Bid 2 at T=86s remaining → extends to T=116s
T=4s: Bid 3 at T=112s remaining → extends to T=142s
... Auction never ends!
```

**Fix:**
```java
// Set maximum extension cap
final int MAX_EXTENSIONS = 3;
final int MAX_TOTAL_TIME = 300; // 5 minutes max

if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
    LocalDateTime newEndTime = auction.getEndTime().plusSeconds(30);
    if (Duration.between(LocalDateTime.now(), newEndTime).getSeconds() <= MAX_TOTAL_TIME) {
        auction.setEndTime(newEndTime);
        auctionScheduler.setTimeCancelAuction(auction);
    }
}
```

---

## 📋 Bug Summary Table

| # | Bug | Severity | Type | File | Line |
|---|-----|----------|------|------|------|
| 1 | Inconsistent lock methods (getSessionId vs getAuctionSessionId) | 🔴 CRITICAL | Thread-Safety | AuctionSessionService.java | 40,53,78 |
| 2 | Mismatched keys in scheduledFutures map | 🔴 CRITICAL | Thread-Safety | AuctionScheduler.java | 36,46,54 |
| 3 | Missing lock release in exception handler | 🔴 CRITICAL | Resource | LoginAction.java | 213 |
| 4 | Stale list reference after add operation | 🔴 CRITICAL | Logic | AuctionSessionService.java | 125-132 |
| 5 | Size check on outdated snapshot | 🟡 HIGH | Logic | AuctionSessionService.java | 131 |
| 6 | Mixed API usage (duplicate methods) | 🟡 HIGH | API | AuctionSession.java | 139,143 |
| 7 | Missing avatarPath initialization | 🟡 MEDIUM | Logic | User.java | 15-29 |
| 8 | Anti-sniping infinite extending | 🟡 MEDIUM | Timing | AuctionSessionService.java | 107-112 |

---

## 🔧 RECOMMENDED FIXES (Priority Order)

### Priority 1 (Must Fix - Blocks Deployment)
1. ✅ **BUG #1** - Standardize lock acquisition method
2. ✅ **BUG #2** - Fix scheduledFutures key mismatch  
3. ✅ **BUG #4** - Fix list reference issue
4. ✅ **BUG #3** - Add proper resource cleanup

### Priority 2 (Should Fix - Quality Issues)
5. ✅ **BUG #6** - Remove duplicate methods
6. ✅ **BUG #5** - Fix size check logic
7. ✅ **BUG #8** - Cap anti-sniping extensions

### Priority 3 (Nice to Fix - Enhancement)
8. ✅ **BUG #7** - Initialize avatar path properly

---

## Testing Recommendations

- [ ] Unit tests for thread-safety in concurrent bidding
- [ ] Integration tests with multiple simultaneous bids
- [ ] Timeout tests for schedule cancellation
- [ ] Avatar initialization tests
- [ ] Anti-sniping extension cap tests


