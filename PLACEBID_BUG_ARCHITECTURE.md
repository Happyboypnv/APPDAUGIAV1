# Place Bid Bug - Technical Architecture & Prevention Guide

## Bug Analysis

### Architecture Pattern Used
The application uses a **dual-store pattern**:
1. **In-Memory Registry** (AuctionSessionRegistry) — fast, real-time state
2. **Persistent Database** (SQLite) — source of truth, survives restarts

### The Information Gap

```
┌─────────────────────────────────────────────────────────┐
│                     USER SESSION                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  REST API Call                                          │
│  ↓                                                      │
│  ApiClient.getAuctionById()      [Reads from DB]       │
│  ↓                                                      │
│  Loads into local DTO (PhienDauGiaDTO)  [RAM]          │
│  ↓                                                      │
│  Displays in UI                                         │
│  │                                                      │
│  │ User clicks "Place Bid"                             │
│  ↓                                                      │
│  WebSocket Message                                      │
│  ↓                                                      │
│  AuctionSessionRegistry.find()    [NOT FOUND!]  ← BUG   │
│  ↓                                                      │
│  Error: "Phiên không tìm thấy"    [WRONG!]             │
│                                                         │
└─────────────────────────────────────────────────────────┘
                          VS
┌─────────────────────────────────────────────────────────┐
│                 SERVER (ANOTHER SESSION)                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  AuctionController.handleCreateAuction()                │
│  ↓                                                      │
│  AuctionRepository.save() [Write to DB]                │
│  ↓                                                      │
│  AuctionSessionRegistry.add() [Write to Registry] ✓    │
│  ↓                                                      │
│  Auction accessible via WebSocket                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Root Cause

The bug occurs because:
1. **REST API** (stateless) loads auction from DB but doesn't populate Registry
2. **WebSocket Handler** assumes auction must be in Registry to be "active"
3. **Gap**: Registry and DB become out of sync

**Why this happens:**
- REST API is designed for simple read operations (no state assumption)
- WebSocket assumes "live" auctions are always in Registry
- No synchronization mechanism between the two paths

## Solution Design Pattern

### Pattern: "Check-Then-Fallback"

```java
AuctionSession getAuction(String id) {
    // STEP 1: Try fast path (in-memory)
    AuctionSession session = registry.find(id);
    
    // STEP 2: Fallback to DB if not found
    if (session == null) {
        session = database.findById(id);
        
        // STEP 3: Populate fast path for future access
        if (session != null) {
            registry.add(session);
        }
    }
    
    return session;
}
```

**Benefits:**
- ✅ Handles auctions from any source
- ✅ Populations Registry gradually (lazy loading)
- ✅ No network overhead if already cached
- ✅ Thread-safe (Registry and DB both thread-safe)

### Applied Implementation

**File:** `AuctionWebSocketServer.java`

**Before:**
```java
AuctionSession phien = registry.find(phienId);  // Hard stop if not found
if (phien == null) {
    sendError(conn, "Auction not found");
    return;  // ❌ WRONG
}
```

**After:**
```java
AuctionSession phien = registry.find(phienId);
if (phien == null) {
    AuctionSession phienFromDB = database.findById(phienId);  // ✅ Fallback
    if (phienFromDB == null) {
        sendError(conn, "Auction not found");
        return;
    }
    phien = phienFromDB;
    registry.add(phien);  // ✅ Cache for next time
}
```

## Prevention Strategies

### Strategy 1: Consistent Patterns Across All Access Points

All methods that access AuctionSession should follow the same pattern:

```java
// Pattern Template
private AuctionSession getOrLoadAuction(String id) {
    return registry.find(id) != null 
        ? registry.find(id)
        : loadAndCacheFromDB(id);
}

private AuctionSession loadAndCacheFromDB(String id) {
    AuctionSession session = database.findById(id);
    if (session != null) {
        registry.add(session);
        logger.info("Loaded and cached: " + id);
    }
    return session;
}
```

**Current violations:**
- ✅ Fixed: `AuctionWebSocketServer.handleBid()`
- ⚠️  Could improve: `AuctionWebSocketServer.handlePaymentDecision()` (already has fallback, but not logging)
- ⚠️  Consider for: Future message handlers

### Strategy 2: Extract Utility Method

Create a helper in a shared utility class:

```java
// File: AuctionAccessUtil.java
public class AuctionAccessUtil {
    private static final AuctionSessionRegistry registry = AuctionSessionRegistry.getInstance();
    private static final AuctionRepositorySQLite repository = new AuctionRepositorySQLite();
    private static final Logger logger = LoggerFactory.getLogger(AuctionAccessUtil.class);
    
    /**
     * Get auction from registry or load from database if needed.
     * Automatically populates registry cache for future access.
     */
    public static AuctionSession getAuctionOrNull(String auctionId) {
        AuctionSession session = registry.find(auctionId);
        if (session != null) {
            return session;
        }
        
        // Fallback to database
        AuctionSession dbSession = repository.findById(auctionId);
        if (dbSession != null) {
            registry.add(dbSession);
            logger.info("✅ Loaded and cached auction: " + auctionId);
        }
        return dbSession;
    }
}
```

**Usage:**
```java
AuctionSession phien = AuctionAccessUtil.getAuctionOrNull(phienId);
if (phien == null) {
    sendError(conn, "Auction not found");
    return;
}
```

**Advantages:**
- ✅ Single source of truth for access pattern
- ✅ Consistent logging
- ✅ Easy to unit test
- ✅ Changes benefit all callers

### Strategy 3: API-Level Synchronization

Ensure REST API results are synced to Registry:

**Current (REST API):**
```java
// BidController.handleSetPrice()
AuctionSession phien = auctionRepository.findById(id);
if (phien == null) {
    return 404;  // BUG: Should add to registry too
}
```

**Should be:**
```java
AuctionSession phien = auctionRepository.findById(id);
if (phien != null) {
    // Ensure registry is updated
    if (!AuctionSessionRegistry.getInstance().has(id)) {
        AuctionSessionRegistry.getInstance().add(phien);
    }
}
```

### Strategy 4: Testing for Registry Misses

Add test cases that specifically test Registry cache misses:

```java
@Test
void testBidWhenAuctionNotInRegistry() {
    // Setup: Auction in DB but NOT in Registry
    // (simulate first-time bidder scenario)
    AuctionSession auctionInDB = createTestAuction("PH001");
    repository.save(auctionInDB);
    // Note: NOT adding to registry
    
    // Act: User places bid
    JsonObject bidMessage = createBidMessage("PH001", "user@mail.com", 150000000);
    handleBid(conn, bidMessage);
    
    // Assert: Should succeed (fallback to DB)
    assertTrue(responseHasStatus("SUCCESS"));
    // And auction should be in registry now
    assertNotNull(registry.find("PH001"));
}
```

## Related Code Locations

### Files Using AuctionSessionRegistry

1. **AuctionWebSocketServer.java** (already fixed)
   - `handleBid()` — ✅ Now has fallback
   - `handlePaymentDecision()` — ⚠️  Has fallback but minimal logging
   
2. **AuctionSessionService.java**
   - Uses registry for thread locking
   - Synchronization on `locks.computeIfAbsent()`
   - Safe as-is since it's synchronized

3. **BidController.java**
   - ⚠️  Only checks Registry, doesn't fallback
   - Should be updated to use `AuctionAccessUtil`

4. **AuctionController.java**
   - Registration called after save: ✅ Good
   - Clear separation of concerns

### Files Accessing Database Directly

1. **AuctionRepositorySQLite.java**
   - Implements CRUD operations
   - Should remain as-is (low-level abstraction)

2. **UserRepositorySQLite.java**
   - User balance operations
   - No registry involved (design is clean)

## Best Practices Going Forward

### ✅ DO:
- Always check Registry **first** for performance
- Always fall back to Database if needed
- Always log when loading from DB (aids debugging)
- Always update Registry after DB load (caches result)
- Use consistent patterns across all access points
- Synchronize API changes across REST and WebSocket endpoints

### ❌ DON'T:
- Rely exclusively on Registry without DB fallback
- Return 404/error when resource exists in DB but not cache
- Forget to log DB access (makes debugging impossible)
- Store stale data in Registry (always reload from DB if changed)
- Use different access patterns in different parts of code

## Performance Implications

### Worst Case Scenario
```
N = number of concurrent users
M = average number of auctions per session

Time Complexity:
- First bid: O(1) Registry access + O(1) DB lookup = O(2)
- Subsequent bids: O(1) Registry access = O(1)

Space Complexity:
- Registry: O(N) — grows with active auctions loaded
- No multi-loading: Fixed max size = number of active auctions
```

### Real-World Impact
- **Database**: 1 extra SELECT query per auction (amortized)
- **Memory**: Minimal (auction object already in RAM on first load anyway)
- **Network**: None (local DB, no network overhead)
- **User Experience**: Invisible (millisecond difference)

## Monitoring & Alerts

### Metrics to Track
```
1. Registry miss rate (audit logs)
   - Should be > 0 on first bid of new auction
   - Should be → 0 as more users bid (caching effect)
   
2. DB fallback success rate
   - Should be 100% (if auction exists, DB always has it)
   - < 100% means data integrity issue
   
3. Error rate by type
   - "Phiên không tìm thấy" — shouldn't happen (bug)
   - "Giá quá thấp" — normal, expected
   - "Số dư không đủ" — normal, expected
```

### Log Patterns to Monitor
```
Warning pattern:
❌ BID REJECTED - reason=Phiên đấu giá không tồn tại

Success pattern:
✅ Loaded auction từ DB vào Registry: PH001

Good sign (caching working):
[No log] — auction already in Registry
```

---

**Architecture Review Date**: May 27, 2026
**Pattern Used**: Check-Then-Fallback (Lazy Loading + Caching)
**Risk Level Before Fix**: HIGH (feature broken)
**Risk Level After Fix**: LOW (fallback implemented)

