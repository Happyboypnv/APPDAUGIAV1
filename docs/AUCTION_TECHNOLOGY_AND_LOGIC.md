# AUCTION TECHNOLOGY & BID LOGIC — PHÂN TÍCH CHI TIẾT

Phiên bản: 1.1
Ngày cập nhật: 2026-05-31

Mục tiêu: tài liệu này giải thích sâu về công nghệ, mô hình dữ liệu liên quan tới đấu giá, chi tiết thuật toán đặt giá (bidding), cách xử lý balance/freeze, concurrency, anti-sniping, endpoints và bộ test bạn nên viết.

---

CHECKLIST (this doc will provide):
- [x] Công nghệ & rationale
- [x] Các API, WebSocket events chi tiết
- [x] Pseudo-code & SQL chi tiết cho hold/release/deduct
- [x] Concurrency model và invariants
- [x] Edge cases và test cases cụ thể
- [x] Quick commands để chạy tests & local verification

---

1) Stack & design choices
-------------------------
- Java 17: LTS, ổn định cho server & client
- SQLite: đơn giản cho dev/local, ACID nhưng single-writer; commit sớm và giữ transactions ngắn
- WebSocket (Java-WebSocket): low-level, dễ broadcast
- Gson: JSON (LocalDateTime adapters included in `client/src/main/java/com/mycompany/utils/ApiClient.java`)
- Logback + SLF4J: structured logging

Design rationale (short): prefer correctness of money operations over raw throughput. Use frozenBalance pattern to prevent double-spend.

2) Core invariants
------------------
- For any user: availableBalance = actualBalance - frozenBalance >= 0
- For any auction: currentPrice is non-decreasing
- No user may be the immediate previous bidder and bid again (business rule)
- DB state must reflect holds and releases atomically

3) Key method signatures (where they live)
-----------------------------------------
- AuctionSessionService.setPrice(AuctionSession auction, User bidder, double gia)  — server/action/AuctionSessionService.java
- IUserRepository.holdBalance(String userId, String sessionId, double amount)
- IUserRepository.releaseHold(String userId, String sessionId, double amount)
- IUserRepository.deductOnWin(String userId, String sessionId, double amount)

4) Pseudo-code: holdBalance (exact behavior required)
---------------------------------------------------
Purpose: ensure that `amount` can be reserved for `userId` atomically; if not possible, fail and do not alter DB.

Pseudo-code (server/utils/UserRepositorySQLite.java):

public boolean holdBalance(String userId, String sessionId, double amount) {
    Connection conn = DatabaseConnection.getConnection();
    try {
        conn.setAutoCommit(false);

        // 1) Read current amounts
        PreparedStatement ps = conn.prepareStatement(
            "SELECT so_du_thuc_te, so_du_dong_bang FROM nguoi_dung WHERE ma_nguoi_dung = ?");
        ps.setString(1, userId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) { conn.rollback(); return false; }
        double actual = rs.getDouble("so_du_thuc_te");
        double frozen = rs.getDouble("so_du_dong_bang");
        double available = actual - frozen;

        // 2) Check
        if (available < amount) { conn.rollback(); return false; }

        // 3) Insert hold record (giu_tien)
        PreparedStatement ins = conn.prepareStatement(
            "INSERT INTO giu_tien (id, ma_nguoi_dung, ma_phien, so_tien, thoi_gian_giu) VALUES (?, ?, ?, ?, datetime('now'))");
        ins.setString(1, UUID.randomUUID().toString());
        ins.setString(2, userId);
        ins.setString(3, sessionId);
        ins.setDouble(4, amount);
        ins.executeUpdate();

        // 4) Update frozen balance
        PreparedStatement up = conn.prepareStatement(
            "UPDATE nguoi_dung SET so_du_dong_bang = so_du_dong_bang + ? WHERE ma_nguoi_dung = ?");
        up.setDouble(1, amount);
        up.setString(2, userId);
        up.executeUpdate();

        conn.commit();
        return true;
    } catch (SQLException e) {
        conn.rollback();
        log.error("holdBalance failed: {}", e.getMessage());
        return false;
    } finally {
        conn.setAutoCommit(true);
    }
}

Notes:
- Keep transactions short (no network calls inside transaction)
- Consider retry on SQLITE_BUSY with small backoff (if contention observed)

5) Pseudo-code: releaseHold
---------------------------
public boolean releaseHold(String userId, String sessionId, double amount) {
    Connection conn = DatabaseConnection.getConnection();
    try {
        conn.setAutoCommit(false);
        PreparedStatement del = conn.prepareStatement(
            "DELETE FROM giu_tien WHERE ma_nguoi_dung = ? AND ma_phien = ? AND so_tien = ? LIMIT 1");
        del.setString(1, userId);
        del.setString(2, sessionId);
        del.setDouble(3, amount);
        int rows = del.executeUpdate();
        if (rows == 0) { conn.rollback(); return false; }

        PreparedStatement up = conn.prepareStatement(
            "UPDATE nguoi_dung SET so_du_dong_bang = MAX(0, so_du_dong_bang - ?) WHERE ma_nguoi_dung = ?");
        up.setDouble(1, amount);
        up.setString(2, userId);
        up.executeUpdate();

        conn.commit();
        return true;
    } catch (SQLException e) {
        conn.rollback();
        log.error("releaseHold failed: {}", e.getMessage());
        return false;
    } finally {
        conn.setAutoCommit(true);
    }
}

6) Pseudo-code: deductOnWin (finalize payment)
-----------------------------------------------
Goal: move frozen funds into seller's actualBalance and clear frozen for buyer.

public boolean deductOnWin(String buyerId, String sessionId, double finalPrice) {
    Connection conn = DatabaseConnection.getConnection();
    try {
        conn.setAutoCommit(false);

        // 1) Verify there's a hold for buyer
        PreparedStatement sel = conn.prepareStatement(
            "SELECT so_tien FROM giu_tien WHERE ma_nguoi_dung = ? AND ma_phien = ?");
        sel.setString(1, buyerId);
        sel.setString(2, sessionId);
        ResultSet rs = sel.executeQuery();
        // sum holds, etc.

        // 2) Delete hold(s) and update buyer: so_du_thuc_te -= finalPrice; so_du_dong_bang -= finalPrice
        // 3) Update seller: so_du_thuc_te += finalPrice
        // 4) Insert into giao_dich

        conn.commit();
        return true;
    } catch (SQLException e) {
        conn.rollback();
        return false;
    } finally {
        conn.setAutoCommit(true);
    }
}

7) setPrice full flow (pseudo)
------------------------------
public boolean setPrice(AuctionSession auction, User bidder, double gia) {
    synchronized (getLock(auction.getSessionId())) {
        // validation checks
        if (!in progress...) return false;
        if (seller) return false;
        if (consecutive bid) return false;
        if (gia < minPrice) return false;

        // 1) Try holdBalance(bidder, session, gia)
        if (!userRepository.holdBalance(...)) return false;

        // 2) releaseHold(previousLeader, session, oldPrice) if exists and previousLeader != bidder
        // 3) update in-memory objects (bidderList, currentPrice, priceStep)
        // 4) anti-sniping extend endTime if needed
        // 5) persist auction (auctionRepository.update)
        // 6) broadcast via websockets
        return true;
    }
}

8) Concurrency tests to write
----------------------------
- Unit test: placeBid_whenBalanceSufficient_shouldReturnTrue
- Unit test: placeBid_whenBalanceInsufficient_shouldReturnFalse
- Integration test: concurrent bids on same auction — spawn 10 threads calling setPrice with the same auctionId and assert no DB invariants broken (balances >=0, only one winner if times overlap)

Example (JUnit + Executors):
ExecutorService es = Executors.newFixedThreadPool(10);
List<Future<Boolean>> fs = new ArrayList<>();
for (int i=0;i<10;i++) {
    final int idx=i;
    fs.add(es.submit(() -> service.setPrice(auction, users.get(idx), amount[idx])));
}
// wait & validate

9) Edge cases & business decisions
---------------------------------
- Should seller be allowed to bid? Currently NO.
- Consecutive bids by same user: business enforces rejection (fairness). If product requires different rule, update `AuctionSessionService`.
- What if holdBalance() returns false but bidder.getAvailableBalance() in memory shows enough? Server DB is source of truth — reject.

10) Replace `System.out.println` with logger (concrete files)
------------------------------------------------------------
Files found with `System.out.println` that should be converted:
- `shared/src/main/java/com/mycompany/models/User.java` (methods buy/sell/leaveRoom)

Suggested patch pattern (manual or scripted):
- Add `private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TheClass.class);` at top of file
- Replace `System.out.println("...")` with `logger.info("...")` or appropriate level

PowerShell simple helper (preview-only):
```powershell
# Print occurrences (do not replace automatically)
Get-ChildItem -Path . -Filter "*.java" -Recurse | Select-String -Pattern "System\.out\.println" | Format-Table Path, LineNumber, Line -AutoSize
```

11) Quick verification steps (local dev)
---------------------------------------
1) Build & run server
```powershell
cd D:\UET\PhongcodeJava\LTNC\APPDAUGIA
mvn -pl server clean package
mvn -pl server exec:java -Dexec.mainClass="com.mycompany.server.ServerMain"
```
2) Start client
```powershell
mvn -pl client javafx:run
```
3) Use ApiClient to place a bid from UI OR via simple curl (example):
```powershell
curl -X POST "http://localhost:8080/api/bids" -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"maPhien":"PH000001","gia":550.0}'
```
4) Monitor DB state with `sqlite3` CLI to ensure holds and releases were executed.

12) Next recommended automation (I can implement if you want)
-----------------------------------------------------------
- Integration test that launches server in-process and runs 2-3 threads simulating bids.
- Small Python/Java harness to simulate N concurrent bidders and produce a report (success/fail counts, timing).
- PlantUML sequence diagram for `createBid` → `setPrice` → `holdBalance` → `broadcast`.

---

Nếu bạn muốn, tôi sẽ:
- (1) Tạo test integration tự động mô phỏng 2 clients bid song song và commit code test vào `shared/src/test` hoặc `server/src/test`.
- (2) Tự động apply thay System.out → logger cho những file đã xác định và chạy `mvn test` để kiểm tra.

Chọn 1 hoặc cả 2 và tôi sẽ tiếp tục thực hiện các thay đổi code + chạy test (trong workspace) và báo lỗi nếu thấy.
