# Place Bid Bug Fix - Quick Reference & Testing Guide

## TL;DR - The Bug and Fix

### The Problem
Place Bid button always fails with: **"Giá không hợp lệ hoặc phiên đã kết thúc"**

### The Root Cause
Server only looks for auctions in memory (Registry), not in database. When client first loads bidding room and tries to bid, auction isn't in Registry → error.

### The Fix
Server now falls back to database if auction not found in Registry:
```
Registry.find(auctionId)
  ↓ not found
Database.findById(auctionId)  ← NEW
  ↓ found
Add to Registry
  ↓
Process bid successfully ✅
```

## What Changed

### File: AuctionWebSocketServer.java (Lines 247-330)

#### Change 1: Added Import
```java
import com.mycompany.models.SessionStatus;
```

#### Change 2: Database Fallback Logic
```java
if (phienHienTai == null) {
    // Try database if not in registry
    AuctionSession phienFromDB = auctionRepositorySQLite.findById(phienId);
    if (phienFromDB != null) {
        phienHienTai = phienFromDB;
        AuctionSessionRegistry.getInstance().add(phienHienTai);  // Cache in registry
    } else {
        sendError(conn, "Phiên đấu giá không tồn tại");
        return;
    }
}
```

#### Change 3: Better Error Messages
Instead of generic "Giá không hợp lệ hoặc phiên đã kết thúc", now shows:
- "Phiên đấu giá đã đóng" — if auction is closed
- "Phiên không ở trạng thái IN_PROGRESS (WAITING/PAID/etc)" — if wrong status
- "Giá đặt phải ≥ 150,000,000 (giaHienTai=145,000,000 + buocGia=5,000,000)" — if price too low
- "Số dư không đủ hoặc bạn vừa đặt giá..." — if balance issue

## Testing Scenarios

### ✅ Scenario 1: First Bid on Newly Loaded Auction (Main Bug Fix)
**Steps:**
1. Open application
2. Navigate to any auction (loads via REST API)
3. Verify current price displays correctly
4. Enter valid bid amount (higher than minimum)
5. Click "Place Bid"

**Expected:**
- ✅ Bid succeeds
- UI updates with new price
- User appears in bid history

**Before Fix:** ❌ Error "Giá không hợp lệ hoặc phiên đã kết thúc"
**After Fix:** ✅ Success

---

### ✅ Scenario 2: Second Bid (Uses Cached Auction from Registry)
**Steps:**
1. After successful first bid
2. Enter higher bid amount
3. Click "Place Bid"

**Expected:**
- ✅ Bid succeeds immediately
- No database lookup needed (found in Registry)
- Fast response

---

### ✅ Scenario 3: Bid Below Minimum Price
**Steps:**
1. Current price = 145 million
2. Minimum = 145M + (145M × 6%) = ~153.7M
3. Enter 150 million (too low)
4. Click "Place Bid"

**Expected:**
- ❌ Error message shows specific requirement:
  - "Giá đặt phải ≥ 153,700,000 (giaHienTai=145,000,000 + buocGia=8,700,000)"

**Before:** ❌ Generic message, confusing
**After:** ✅ Clear explanation of minimum required

---

### ✅ Scenario 4: Insufficient Balance
**Steps:**
1. User balance = 50 million
2. Current price = 145 million
3. Enter 200 million (user can't afford)
4. Click "Place Bid"

**Expected:**
- ❌ Error: "Số dư không đủ hoặc bạn vừa đặt giá..."
- Suggests checking frozen balance

---

### ✅ Scenario 5: Consecutive Bids (Same User, Back-to-Back)
**Steps:**
1. User A bids 150M → succeeds
2. User A (still leading) tries to bid 160M immediately
3. Click "Place Bid"

**Expected:**
- ❌ Error: "Bạn không được đặt giá 2 lần liên tiếp. Hãy chờ người khác đặt giá cao hơn."

---

### ✅ Scenario 6: Auction Already Closed/Ended
**Steps:**
1. Auction has already ended (status = PAID or CANCELLED)
2. Try to bid

**Expected:**
- ❌ Error: "Phiên đấu giá đã đóng"

---

### ✅ Scenario 7: Auction Status = WAITING (Not Started)
**Steps:**
1. Auction created but not yet started (status = WAITING)
2. Try to bid

**Expected:**
- ❌ Error: "Phiên không ở trạng thái IN_PROGRESS (trạng thái hiện tại: WAITING)"

---

## Debugging Guide

### Check Server Logs

#### Success Log
```
✅ Loaded auction từ DB vào Registry: PH000001
💬 Sent BID: {"action":"BID",...}
✅ Cập nhật balance DB cho bidder: John Doe
```

#### Failure Logs
```
❌ BID REJECTED - phien=PH000001, user=john@gmail.com, gia=150000000, reason=Giá đặt phải ≥ ...
```

### Check Database

Verify auction exists in database:
```sql
SELECT ma_phien, ten_phien, trang_thai, gia_hien_tai, is_closed 
FROM phien_dau_gia 
WHERE ma_phien = 'PH000001';
```

Expected:
```
ma_phien  | ten_phien          | trang_thai   | gia_hien_tai | is_closed
----------|-------------------|--------------|--------------|----------
PH000001  | Đấu giá Laptop     | IN_PROGRESS  | 145000000    | 0
```

### Check Registry

Look for log entries when auction is added:
```
✅ Loaded auction từ DB vào Registry: PH000001
```

This indicates:
- Auction was loaded from database
- Successfully added to in-memory registry
- Subsequent bids won't need DB lookup

## Common Issues & Solutions

### Issue 1: Still Getting Error "Phiên đấu giá không tồn tại"
**Possible causes:**
1. Auction doesn't exist in database
2. Wrong phienId being sent

**Solution:**
- Check SQL query result above ↑
- If no rows, auction truly doesn't exist
- Reload application to get correct phienId

---

### Issue 2: Error "Phiên không ở trạng thái IN_PROGRESS"
**Possible causes:**
1. Auction status is WAITING (not started yet)
2. Auction status is PAID or CANCELLED (already ended)

**Solution:**
- Wait for auction to start if it's WAITING
- Cannot bid on ended auctions

---

### Issue 3: Error about "Giá đặt phải ≥ ..."
**Possible causes:**
1. Bid amount is too low
2. Price step calculation wrong

**Solution:**
- Use the "+/- Buttons" to auto-calculate correct minimum
- Click "+" button to increase by one price step
- The field should auto-update with correct amount

---

### Issue 4: Error "Số dư không đủ"
**Possible causes:**
1. Available balance too low
2. Too much balance frozen in other auctions

**Solution:**
- Check account balance
- If frozen balance is high, consider canceling or waiting for other auctions to end
- Close other auction tabs if participating in multiple

---

## Performance Notes

### First Bid
- **Before:** Error immediately (bug)
- **After:** One DB query + Registry add (still fast)

### Subsequent Bids
- **Before:** Not possible (feature was broken)
- **After:** Registry lookup only (very fast, no DB access)

### Optimal Scenario
- Open bidding room (REST API loads auction)
- Place first bid (triggers DB fallback, caches in Registry)
- Place additional bids (all use Registry cache, no DB)

## Rollback Instructions

If needed to rollback to previous version:

1. Remove import: `import com.mycompany.models.SessionStatus;`
2. Revert handleBid() method to look only at Registry
3. Recompile and redeploy

But this would bring back the original bug, so not recommended unless critical issue discovered.

---

**Last Updated**: May 27, 2026
**Tested On**: Java, SQLite, WebSocket (Java-WebSocket library)
**Status**: ✅ Ready for Production

