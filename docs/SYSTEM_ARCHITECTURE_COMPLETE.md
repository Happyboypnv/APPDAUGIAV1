# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN — KIẾN TRÚC TOÀN CẢNH (MỞ RỘNG)

Phiên bản: 1.1
Ngày cập nhật: 2026-05-31
Dự án: AppDauGia

Mục tiêu của tệp này: cung cấp một cái nhìn toàn diện và tham chiếu nhanh cho người muốn hiểu toàn bộ hệ thống — các module, lớp chính, API, luồng đặt giá chi tiết, DB schema, concurrency, và cách khởi động.

---

CHECKLIST (những gì tài liệu này bao phủ):
- [x] Mô tả module (shared, server, client)
- [x] Danh sách file / class quan trọng với đường dẫn trong repo
- [x] Schema SQLite quan trọng và các câu SQL mẫu
- [x] Luồng đặt giá (bidding) step-by-step, với code snippets thực tế
- [x] Concurrency & transaction strategy (synchronized per-session + DB transactions)
- [x] WebSocket events và định dạng messages
- [x] Hướng dẫn khởi động (PowerShell commands)

---

1) Tổng quan ngắn
-----------------
AppDauGia gồm 3 module Maven chính:
- `shared` (models + DTOs)
- `server` (REST API, services, repositories, websocket server)
- `client` (JavaFX desktop app, ApiClient HTTP wrapper)

Mục tiêu chính của hệ thống là: cho phép tạo phiên đấu giá, người dùng tham gia đặt giá theo thời gian thực, đảm bảo nhất quán số dư tài khoản (actual vs frozen), và thông báo tức thời qua WebSocket.

2) File/class quan trọng (quick reference)
----------------------------------------
Dưới đây là những file bạn thường cần đọc hoặc chỉnh khi bảo trì logic đấu giá:
- `shared/src/main/java/com/mycompany/models/AuctionSession.java` — domain model phiên đấu giá
- `shared/src/main/java/com/mycompany/models/User.java` — domain model người dùng (actualBalance, frozenBalance)
- `shared/src/main/java/com/mycompany/models/Bid.java` — model lượt đặt giá
- `server/src/main/java/com/mycompany/action/AuctionSessionService.java` — business logic chính, gồm `setPrice()` (critical)
- `server/src/main/java/com/mycompany/utils/UserRepositorySQLite.java` — holdBalance, releaseHold, deductOnWin (DB atomic ops)
- `server/src/main/java/com/mycompany/utils/AuctionRepositorySQLite.java` — lưu/phục hồi phiên đấu giá
- `server/src/main/java/com/mycompany/server/websocket/AuctionWebSocketServer.java` — broadcast events
- `client/src/main/java/com/mycompany/utils/ApiClient.java` — client-side HTTP wrapper (createBid, getAuctions, deposit)

3) Mô hình dữ liệu (essentials)
-------------------------------
Bảng cốt lõi liên quan đến bidding & balance:

-- `nguoi_dung` (user)
- `ma_nguoi_dung` TEXT PRIMARY KEY
- `email` TEXT UNIQUE
- `so_du_thuc_te` REAL DEFAULT 0.0  -- actualBalance
- `so_du_dong_bang` REAL DEFAULT 0.0  -- frozenBalance

-- `phien_dau_gia` (auction session)
- `ma_phien` TEXT PRIMARY KEY
- `gia_hien_tai` REAL NOT NULL
- `co_bid` INTEGER DEFAULT 0
- `trang_thai` TEXT NOT NULL
- `thoi_gian_bat_dau`, `thoi_gian_ket_thuc` TEXT

-- `giu_tien` (holds)
- `id` TEXT PRIMARY KEY
- `ma_nguoi_dung` TEXT
- `ma_phien` TEXT
- `so_tien` REAL
- `thoi_gian_giu` TEXT

-- `luot_dat_gia` (bid history)
- `id_bid` TEXT PRIMARY KEY
- `ma_phien` TEXT
- `ma_nguoi_dung` TEXT
- `so_tien` REAL
- `thoi_gian_dat` TEXT

Câu SQL mẫu — holdBalance (ý tưởng):

BEGIN TRANSACTION;
SELECT so_du_thuc_te, so_du_dong_bang FROM nguoi_dung WHERE ma_nguoi_dung = ?;
-- compute available
-- if available < amount -> ROLLBACK; return false
INSERT INTO giu_tien (id, ma_nguoi_dung, ma_phien, so_tien, thoi_gian_giu) VALUES (?, ?, ?, ?, datetime('now'));
UPDATE nguoi_dung SET so_du_dong_bang = so_du_dong_bang + ? WHERE ma_nguoi_dung = ?;
COMMIT;

Câu SQL mẫu — releaseHold:

BEGIN TRANSACTION;
DELETE FROM giu_tien WHERE ma_nguoi_dung = ? AND ma_phien = ? AND so_tien = ?;
UPDATE nguoi_dung SET so_du_dong_bang = MAX(0, so_du_dong_bang - ?) WHERE ma_nguoi_dung = ?;
COMMIT;

4) Luồng đặt giá (place bid) — chi tiết từng bước (server-side authoritative)
----------------------------------------------------------------------------
Tóm tắt: client gửi POST `/api/bids` -> controller validate token -> load auction -> AuctionSessionService.setPrice() thực thi logic sau (synchronized per sessionId):

Detailed steps inside `setPrice(AuctionSession auction, User bidder, double gia)`:

A. Quick validations (fast rejects)
- auction.getStatus() == IN_PROGRESS
- !auction.isClosed()
- bidder.getUserId() != auction.getSeller().getUserId()  // seller cannot bid
- previousLeader != bidder (no consecutive bids)
- gia >= minimum (if hasBid => currentPrice + priceStep, else startingPrice)

B. Atomic hold step (critical)
- Call `userRepository.holdBalance(bidderId, sessionId, gia)` which MUST run in a DB transaction:
  - read actual & frozen
  - compute available = actual - frozen
  - if available < gia -> rollback & return false
  - insert hold record into `giu_tien`
  - update `nguoi_dung.so_du_dong_bang += gia`
  - commit
- Only if holdBalance returns true proceed

Rationale: do NOT rely on bidder.getAvailableBalance() from RAM; object might be stale. Always check DB atomically.

C. Release previous leader's hold (if any and different user)
- `userRepository.releaseHold(prevLeaderId, sessionId, oldPrice)` — also transactional
- update prevLeader object in RAM (prevLeader.setFrozenBalance(Math.max(0, prevLeader.getFrozenBalance() - oldPrice))) to keep in-memory view coherent
- send balance update via WebSocket to previous leader (reload from DB for exact available)

D. Update auction state (in-memory)
- auction.addBidder(bidder)
- auction.setCurrentPrice(gia)
- auction.setPriceStep(gia * auction.getMinPriceDiffRatio()) // typically 6%
- if (!auction.isHasBid()) auction.setHasBid(true)

E. Anti-sniping
- compute secondsRemaining = Duration.between(now, auction.getEndTime()).getSeconds()
- if (secondsRemaining > 0 && secondsRemaining <= 60) {
    auction.setEndTime(auction.getEndTime().plusSeconds(30));
    auctionScheduler.setACAuction(auction); // reschedule close
  }

F. Persist auction (auctionRepository.update(auction)) and respond success
G. Broadcast to clients via AuctionWebSocketServer.broadcastBidUpdate(...)
H. Return true

5) Concurrency design (why it is safe)
------------------------------------
- `synchronized(getLock(sessionId))` ensures only one thread executes `setPrice()` for a given auction session at a time (per-JVM). This prevents interleaving updates that would break in-memory invariants (bidderList, currentPrice updates).
- DB transactions inside `holdBalance()`/`releaseHold()` ensure row-level atomicity and durability. For SQLite, transaction isolation and immediate commits are important to avoid SQLITE_BUSY in concurrent writes.
- Combined approach: application-level lock (per-session) + DB transaction (per-user balance) yields robust correctness for single-server deployment.

Limitations:
- If you run multiple server instances (multi-JVM), per-process synchronized locks won't coordinate across processes — you need an external distributed lock (Redis, DB advisory locks, etc.). For current local use SQLite + single server is expected.

6) WebSocket messages (formats)
-------------------------------
BID_UPDATE (server -> all ws clients viewing session):
{
  "type": "BID_UPDATE",
  "sessionId": "PH000001",
  "currentPrice": 550.0,
  "priceStep": 33.0,
  "bidderId": "U002",
  "bidderName": "Nguyen A",
  "remainingSeconds": 245,
  "timestamp": "2026-05-31T10:15:15"
}

BALANCE_UPDATE (server -> specific user):
{
  "type": "BALANCE_UPDATE",
  "email": "user@auction.com",
  "availableBalance": 1200.0
}

SESSION_ENDED (server -> watchers):
{
  "type": "SESSION_ENDED",
  "sessionId": "PH000001",
  "status": "PAID",
  "winnerId": "U005",
  "finalPrice": 1250.0
}

7) Những file code cần kiểm tra / refactor tức thì
--------------------------------------------------
- Thay tất cả `System.out.println(...)` bằng `logger` (SLF4J) trong repo để logs nhất quán. File ví dụ cần đổi nằm trong `shared/src/main/java/com/mycompany/models/User.java` (method buy/sell/leaveRoom) — hiện dùng System.out.
- Đảm bảo mọi thao tác DB có commit/rollback và setAutoCommit(false) khi cần.

PowerShell quick-replace (tùy chỉnh trước khi chạy):

```powershell
# LƯU Ý: chạy ở thư mục gốc của repo
Get-ChildItem -Path . -Filter "*.java" -Recurse | ForEach-Object {
  (Get-Content $_.FullName) -replace 'System\.out\.println', 'logger.info' | Set-Content $_.FullName
}
```

(Manual review required — script thay thế trực tiếp nên test trên branch riêng.)

8) Hướng dẫn khởi động (PowerShell)
-----------------------------------
Build toàn bộ project:

```powershell
cd D:\UET\PhongcodeJava\LTNC\APPDAUGIA
mvn clean install
```

Chạy server (nếu server có main class `ServerMain`):

```powershell
# Từ root
mvn -pl server exec:java -Dexec.mainClass="com.mycompany.server.ServerMain"

# Hoặc trực tiếp vào module
cd server
mvn exec:java -Dexec.mainClass="com.mycompany.server.ServerMain"
```

Chạy client JavaFX (một terminal khác):

```powershell
mvn -pl client javafx:run
# or
mvn -pl client exec:java -Dexec.mainClass="com.mycompany.Main"
```

9) Troubleshooting nhanh
-------------------------
- Nếu connect refused từ ApiClient: kiểm tra server đã khởi chạy trên port 8080 chưa.
- Nếu WebSocket không kết nối: kiểm tra port 8081 (hoặc config trong `AuctionWebSocketServerStarter`).
- Nếu SQLite trả SQLITE_BUSY: chắc chắn commit sau read/write và tránh giữ transactions lâu; giảm thời gian lock; nếu cần tăng retries/backoff trong repository.

10) Next steps đề xuất
----------------------
- (A) Tự động quét repo và thay System.out -> logger, rồi chạy `mvn test`
- (B) Viết integration test mô phỏng 2 client bid song song để xác thực concurrency
- (C) Nếu cần scale, lên phương án chuyển DB sang PostgreSQL + distributed locking hoặc Redis

---

Kết luận: file này là bản tham chiếu toàn diện, đủ để bạn đọc nhanh logic core và biết nơi cần thay đổi code. Nếu bạn muốn tôi cập nhật thêm sequence diagram (PlantUML) hoặc tạo test case tự động mô phỏng 2 client bid song song, tôi sẽ thực hiện tiếp.
