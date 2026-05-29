# ProductImgPath Storage Fix - Technical Implementation Guide

## Overview

This document provides a detailed technical explanation of how the `productImgPath` bug was fixed, including the complete data flow, architecture, and implementation details.

## Issue Analysis

### Symptom
- Client sends `productImgPath` in POST /api/auctions request
- Server accepts request and returns success (201 Created)
- Database column `product_img_path` in `phien_dau_gia` table contains NULL
- GET /api/auctions/{id} responses have null `productImgPath`

### Root Cause Chain

```
Client sends productImgPath in Request Body
    ↓
AuctionController.handleCreateAuction() reads body with GSON
    ↓
GSON tries to deserialize to TaoPhienRequest.class
    ↓
TaoPhienRequest was missing productImgPath field
    ↓
GSON silently ignores productImgPath in JSON (not an error)
    ↓
req.productImgPath = null (or undefined)
    ↓
AuctionSession never receives the path
    ↓
auctionRepository.save(phienMoi) receives null
    ↓
INSERT ... product_img_path = NULL
    ↓
Database stores NULL
```

## Architecture

### Component Diagram

```
┌─────────────┐
│   Client    │
│ (JavaFX)    │
└──────┬──────┘
       │ POST /api/auctions
       │ { productImgPath: "C:\...\image.jpg" }
       ↓
┌──────────────────────────────────────────────┐
│    AuctionController                         │
│  ┌────────────────────────────────────────┐  │
│  │ handleCreateAuction()                  │  │
│  └────────────────────────────────────────┘  │
└──────┬───────────────────────────────────────┘
       │ 1. GSON.fromJson(body, TaoPhienRequest)
       ↓
┌──────────────────────────────────────────────┐
│    TaoPhienRequest (inner class)             │
│  • tenPhien                                  │
│  • tenSanPham                                │
│  • maSanPham                                 │
│  • danhMuc                                   │
│  • moTa                                      │
│  • giaKhoiDiem                               │
│  • thoiGianGiay                              │
│  • thoiGianBatDau                            │
│  • productImgPath ← FIXED: ADDED THIS        │
└──────┬───────────────────────────────────────┘
       │ 2. create AuctionSession object
       ↓
┌──────────────────────────────────────────────┐
│    AuctionSession (shared model)             │
│  • sessionId                                 │
│  • sessionName                               │
│  • currentPrice                              │
│  • priceStep                                 │
│  • startTime                                 │
│  • endTime                                   │
│  • duration                                  │
│  • status                                    │
│  • seller                                    │
│  • winner                                    │
│  • product                                   │
│  • hasBid                                    │
│  • isClosed                                  │
│  • isAccepted                                │
│  • productImgPath ← FIXED: NOW SET HERE      │
└──────┬───────────────────────────────────────┘
       │ 3. auctionRepository.save(phienMoi)
       ↓
┌──────────────────────────────────────────────┐
│    AuctionRepositorySQLite                   │
│  • save(AuctionSession)                      │
│  • INSERT phien_dau_gia                      │
│  • product_img_path = AuctionSession         │
│    .getProductImgPath()                      │
└──────┬───────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────────┐
│    SQLite Database (hipiti.db)               │
│  Table: phien_dau_gia                        │
│  ┌────────────────────────────────────────┐  │
│  │ ma_phien     │ ... │ product_img_path │  │
│  │──────────────┼─────┼──────────────────│  │
│  │ PH000001     │ ... │ "C:\...\img.jpg" │  │
│  │ PH000002     │ ... │ "D:\...\pic.jpg" │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

## Code Changes Details

### Change 1: TaoPhienRequest DTO

**Before**:
```java
public static class TaoPhienRequest {
    String tenPhien;
    String tenSanPham;
    String maSanPham;
    String danhMuc;
    String moTa;
    double giaKhoiDiem;
    int    thoiGianGiay;
    String thoiGianBatDau;
    // NO productImgPath field!
}
```

**After**:
```java
public static class TaoPhienRequest {
    String tenPhien;
    String tenSanPham;
    String maSanPham;
    String danhMuc;
    String moTa;
    double giaKhoiDiem;
    int    thoiGianGiay;
    String thoiGianBatDau;
    String productImgPath; // ← ADDED
}
```

**Why this works**:
- GSON uses reflection to match JSON field names to Java class fields
- When GSON sees "productImgPath" in JSON, it looks for a matching field
- Now that the field exists, GSON populates it automatically
- **No changes needed to constructor** - GSON uses reflection, not constructors

### Change 2: Set ProductImgPath in AuctionSession

**Before**:
```java
AuctionSession phienMoi = new AuctionSession(
    null, req.tenPhien, sanPham, req.giaKhoiDiem, seller, req.thoiGianGiay);

// ... time calculations ...

auctionRepository.save(phienMoi); // phienMoi.productImgPath is still null
```

**After**:
```java
AuctionSession phienMoi = new AuctionSession(
    null, req.tenPhien, sanPham, req.giaKhoiDiem, seller, req.thoiGianGiay);

// ... time calculations ...

// FIX: Set productImgPath từ request vào AuctionSession trước khi save
if (req.productImgPath != null && !req.productImgPath.isBlank()) {
    phienMoi.setProductImgPath(req.productImgPath);
    logger.info("✅ Đặt productImgPath: " + req.productImgPath);
}

auctionRepository.save(phienMoi);
```

**Why this is needed**:
- AuctionSession constructor doesn't accept productImgPath parameter
- Must use setter method to assign the value
- Check for null/blank to avoid storing empty strings
- Log info for debugging

### Change 3: Response DTOs

**TomTatPhien (summary view)**:
```java
TomTatPhien(AuctionSession p) {
    // ... existing fields ...
    this.productImgPath = p.getProductImgPath(); // ← ADDED
}
```

**ChiTietPhien (detail view)**:
```java
ChiTietPhien(AuctionSession p) {
    // ... existing fields ...
    this.productImgPath = p.getProductImgPath(); // ← ADDED
}
```

**Why response DTOs need this**:
- When client calls GET /api/auctions or GET /api/auctions/{id}
- Server needs to serialize AuctionSession to JSON response
- If productImgPath is not in DTO, it won't be in JSON response
- Client wouldn't receive the path even if it was stored

## Database State

### Schema (Correct from the start)

```sql
CREATE TABLE IF NOT EXISTS phien_dau_gia (
    ma_phien TEXT PRIMARY KEY,
    ten_phien TEXT NOT NULL,
    gia_hien_tai REAL NOT NULL,
    buoc_gia REAL DEFAULT 0,
    thoi_gian_bat_dau TEXT,
    thoi_gian_ket_thuc TEXT,
    trang_thai TEXT NOT NULL,
    ma_nguoi_ban TEXT,
    ma_nguoi_thang_cuoc TEXT,
    ma_san_pham TEXT,
    is_closed INTEGER DEFAULT 0,
    is_accepted INTEGER DEFAULT -1,
    product_img_path TEXT,  -- ← This column existed!
    FOREIGN KEY (ma_nguoi_ban) REFERENCES nguoi_dung(ma_nguoi_dung),
    FOREIGN KEY (ma_nguoi_thang_cuoc) REFERENCES nguoi_dung(ma_nguoi_dung),
    FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
);
```

### Migration (Correct from the start)

```java
// In DatabaseConnection.initialize()
stmt.execute(sqlPhien);
addColumnIfMissing(conn, "phien_dau_gia", "product_img_path", "TEXT");
```

### Insert Statement (Correct in AuctionRepositorySQLite)

```java
String sql = "INSERT INTO phien_dau_gia " +
    "(ma_phien, ten_phien, gia_hien_tai, buoc_gia, thoi_gian_bat_dau, " +
     "thoi_gian_ket_thuc, trang_thai, ma_nguoi_ban, ma_san_pham, " +
     "ma_nguoi_thang_cuoc, is_closed, is_accepted, product_img_path) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
    // ... parameter bindings ...
    ps.setString(13, AuctionSession.getProductImgPath()); // ← Correct!
    ps.executeUpdate();
    ps.getConnection().commit();
}
```

### Update Statement (Correct in AuctionRepositorySQLite)

```java
String sql = "UPDATE phien_dau_gia SET " +
    "... product_img_path = ? WHERE ma_phien = ?";

try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
    // ... parameter bindings ...
    ps.setString(12, AuctionSession.getProductImgPath()); // ← Correct!
    ps.executeUpdate();
    ps.getConnection().commit();
}
```

### Select Statement (Correct in AuctionRepositorySQLite)

```java
String sql = "SELECT p.*, ... FROM phien_dau_gia p ...";

// In findAll():
AuctionSession.setProductImgPath(rs.getString("product_img_path")); // ← Correct!

// In findById():
auction.setProductImgPath(rs.getString("product_img_path")); // ← Correct!
```

## HTTP Request/Response Flow

### Original Problem Request

```http
POST /api/auctions HTTP/1.1
Authorization: Bearer USER_example@test.com_1622548800
Content-Type: application/json

{
  "tenPhien": "Đấu giá Laptop",
  "tenSanPham": "Laptop Dell XPS",
  "maSanPham": "SP001",
  "danhMuc": "Electronics",
  "moTa": "Laptop cao cấp",
  "thoiGianBatDau": "2026-05-27T10:00:00",
  "giaKhoiDiem": 10000000,
  "thoiGianGiay": 3600,
  "productImgPath": "C:\\Users\\seller\\image\\laptop.jpg"  ← Sent by client
}
```

**Database Before Fix**:
```
INSERT ... product_img_path = NULL  ← productImgPath was null
```

**Response** (same before and after):
```json
{
  "maPhien": "PH000001",
  "thongBao": "Tạo phiên thành công"
}
```

### After Fix - GET Response

```http
GET /api/auctions/PH000001 HTTP/1.1

Response: 200 OK
```

**Before Fix**:
```json
{
  "maPhien": "PH000001",
  "tenPhien": "Đấu giá Laptop",
  "giaHienTai": 10000000,
  "buocGia": 0,
  "trangThai": "PENDING",
  "tenNguoiBan": "Seller Name",
  "tenSanPham": "Laptop Dell XPS",
  "moTa": "Laptop cao cấp",
  "danhMucSanPham": "Electronics",
  "thoiGianBatDau": "2026-05-27T10:00:00",
  "thoiGianKetThuc": "2026-05-27T11:00:00",
  "soNguoiTraGia": 0
  // productImgPath was missing!
}
```

**After Fix**:
```json
{
  "maPhien": "PH000001",
  "tenPhien": "Đấu giá Laptop",
  "giaHienTai": 10000000,
  "buocGia": 0,
  "trangThai": "PENDING",
  "tenNguoiBan": "Seller Name",
  "tenSanPham": "Laptop Dell XPS",
  "moTa": "Laptop cao cấp",
  "danhMucSanPham": "Electronics",
  "thoiGianBatDau": "2026-05-27T10:00:00",
  "thoiGianKetThuc": "2026-05-27T11:00:00",
  "soNguoiTraGia": 0,
  "productImgPath": "C:\\Users\\seller\\image\\laptop.jpg"  ← NOW INCLUDED
}
```

## Testing Scenarios

### Scenario 1: Normal Create with Image Path

```java
// Client sends
{
  "productImgPath": "C:\\Users\\john\\Documents\\product.jpg"
}

// Expected behavior:
// 1. TaoPhienRequest.productImgPath = "C:\\Users\\john\\Documents\\product.jpg"
// 2. phienMoi.setProductImgPath("C:\\Users\\john\\Documents\\product.jpg")
// 3. db: INSERT ... product_img_path = "C:\\Users\\john\\Documents\\product.jpg"
// 4. Response includes productImgPath in DTO
```

### Scenario 2: Create without Image Path

```java
// Client sends (productImgPath omitted)
{
  "tenPhien": "Auction",
  // no productImgPath
}

// Expected behavior:
// 1. TaoPhienRequest.productImgPath = null (GSON default)
// 2. if (req.productImgPath != null) check fails, so setProductImgPath NOT called
// 3. db: INSERT ... product_img_path = NULL (from initial state)
// 4. Response includes "productImgPath": null
```

### Scenario 3: Empty String Image Path

```java
// Client sends
{
  "productImgPath": ""
}

// Expected behavior:
// 1. TaoPhienRequest.productImgPath = ""
// 2. if (req.productImgPath != null && !req.productImgPath.isBlank()) fails
// 3. setProductImgPath NOT called
// 4. db: INSERT ... product_img_path = NULL (from initial state)
```

### Scenario 4: Retrieve Auction with Image Path

```
GET /api/auctions/PH000001

// Expected flow:
// 1. AuctionRepositorySQLite.findById("PH000001")
// 2. SELECT ... product_img_path FROM phien_dau_gia WHERE ma_phien = "PH000001"
// 3. rs.getString("product_img_path") = "C:\\Users\\john\\Documents\\product.jpg"
// 4. auction.setProductImgPath("C:\\Users\\john\\Documents\\product.jpg")
// 5. ChiTietPhien DTO includes: "productImgPath": "C:\\Users\\john\\Documents\\product.jpg"
// 6. GSON serializes to JSON response
```

## Security Considerations

### Data Validation
- **Current**: No validation on path format (by design - different OSes have different path formats)
- **Recommendation**: Validate that path is not excessively long (buffer overflow) or contains invalid characters

### SQL Injection
- **Status**: ✅ SAFE - Uses PreparedStatement with ? placeholders
- ```java
  ps.setString(13, AuctionSession.getProductImgPath());
  // No string concatenation or unsafe concatenation
  ```

### Path Traversal
- **Status**: ⚠️ NOT VALIDATED - Currently stores any path string
- **Recommendation**: Add validation to prevent path traversal attacks:
  ```java
  // Before storing, validate:
  if (productImgPath.contains("..") || productImgPath.startsWith("/")) {
    // Reject or sanitize
  }
  ```

### Data Exposure
- **Status**: ✅ SAFE - File path is just a string, no file content is transmitted
- Uses authentication (Bearer token) for POST requests

## Performance Analysis

### Impact of Changes
1. **Additional Field Deserialization**: Negligible (one string field)
2. **Additional Setter Call**: Negligible (one method call)
3. **Database I/O**: No change (column already existed)
4. **Response Size**: Minimal increase (~50 bytes per auction in response)

### Query Performance
- All existing indexes maintained
- No new database queries added
- Benefit: Can now filter/search by productImgPath if needed

```sql
-- Now possible if index added:
CREATE INDEX idx_product_img_path ON phien_dau_gia(product_img_path);
```

## Rollback Procedure

If this fix causes issues and needs to be reverted:

1. Revert changes to `AuctionController.java`
2. Existing auctions with NULL `product_img_path` unaffected
3. New auctions created before fix have NULL path (optional field)
4. No database cleanup needed
5. Clients will receive null for `productImgPath` field

## Summary of Complete Integration

| Component | Status | Notes |
|-----------|--------|-------|
| Database Schema | ✅ Working | Column `product_img_path` exists |
| DB Migration | ✅ Working | AUTO-added during init |
| AuctionRepositorySQLite.save() | ✅ Working | INSERT includes field |
| AuctionRepositorySQLite.findById() | ✅ Working | SELECT retrieves field |
| AuctionRepositorySQLite.update() | ✅ Working | UPDATE sets field |
| AuctionSession Model | ✅ Working | Has getter/setter |
| Shared TaoPhienRequest DTO | ✅ Working | Has productImgPath |
| Server TaoPhienRequest DTO | ✅ FIXED | NOW has productImgPath |
| handleCreateAuction() | ✅ FIXED | NOW sets productImgPath |
| TomTatPhien Response DTO | ✅ FIXED | NOW includes productImgPath |
| ChiTietPhien Response DTO | ✅ FIXED | NOW includes productImgPath |
| PhienDauGiaDTO (shared) | ✅ Working | Has productImgPath |
| Client Integration | ✅ Working | Sends productImgPath |

---

**Technical Review**: May 27, 2026
**By**: AI Programming Assistant
**Status**: ✅ Complete

