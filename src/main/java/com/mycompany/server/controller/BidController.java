package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.models.*;
import com.mycompany.utils.IUserRepository;
import com.mycompany.utils.IAuctionRepository;
import com.mycompany.utils.UserRepositorySQLite;
import com.mycompany.utils.AuctionRepositorySQLite;
import com.sun.net.httpserver.HttpExchange;
import com.mycompany.server.dto.DatGiaRequest;
import com.mycompany.server.dto.DatGiaResponse;
import com.mycompany.server.dto.LichSuDatGiaResponse;
import com.mycompany.server.dto.LuotDatGia;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * BidController — Xử lý các HTTP request liên quan đến đặt giá (bid).
 *
 * Hai API endpoint:
 *  POST /api/bids               → Đặt giá (validate + lưu thông qua PhienDauGiaService)
 *  GET  /api/bids/{phienId}     → Lịch sử đặt giá của một phiên
 *
 * Tái sử dụng:
 *  - PhienDauGiaService.setPrice()   (singleton, có sẵn) — toàn bộ validate + cập nhật state
 *  - KhoLuuTruPhienDauGiaSQLite    (có sẵn)            — đọc / ghi phiên
 *  - KhoLuuTruNguoiDungSQLite      (có sẵn)            — tra cứu người đặt giá
 *
 * Validate do PhienDauGiaService.setPrice() đảm nhiệm:
 *  ✔ Phiên phải đang diễn ra (DANG_DIEN_RA)
 *  ✔ Người đặt giá không phải người bán
 *  ✔ Giá mới ≥ giaHienTai + buocGia
 *  ✔ Tự động gia hạn nếu còn < 60s
 *
 * Authentication:
 *  - POST yêu cầu Authorization: Bearer <token>
 *  - GET không yêu cầu token (public)
 */
public class BidController {

    private static final org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(BidController.class);

    // ===== PHỤ THUỘC =====

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /** Kho phiên đấu giá — tái sử dụng implementation SQLite đã có */
    private final IAuctionRepository auctionRepository = new AuctionRepositorySQLite();

    /** Kho người dùng — để tra cứu người đặt giá từ token */
    private final IUserRepository userRepository = new UserRepositorySQLite();

    /** Service đấu giá — tái sử dụng singleton đã có, KHÔNG tạo mới */
    private final AuctionSessionService auctionSessionService = AuctionSessionService.getInstance();

    /** Registry phiên đang chạy trong RAM — dùng để lấy object thật thay vì load lại từ DB */
    private final AuctionSessionRegistry auctionSessionRegistry = AuctionSessionRegistry.getInstance();

    // =========================================================
    // ROUTING
    // =========================================================

    /**
     * Điểm vào duy nhất cho mọi request đến /api/bids
     *
     * Routing:
     *   POST /api/bids            → handleDatGia()
     *   GET  /api/bids/{phienId}  → handleLayLichSu()
     */
    public void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path   = exchange.getRequestURI().getPath();

        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        // POST /api/bids → đặt giá
        if (method.equals("POST") && path.equals("/api/bids")) {
            handleSetPrice(exchange);
            return;
        }

        // GET /api/bids/{phienId} → lịch sử đặt giá
        if (method.equals("GET") && path.startsWith("/api/bids/")) {
            String maPhien = path.substring("/api/bids/".length());
            handleLayLichSu(exchange, maPhien);
            return;
        }

        guiPhanHoi(exchange, 404, bug("Endpoint không tồn tại: " + method + " " + path));
    }

    // =========================================================
    // API 1: POST /api/bids  →  đặt giá
    // =========================================================

    /**
     * Đặt giá cho một phiên đấu giá đang diễn ra.
     *
     * Yêu cầu: Authorization: Bearer <token>
     *
     * Request body:
     * {
     *   "maPhien": "PH000001",
     *   "gia": 750000
     * }
     *
     * Quy trình:
     *  1. Xác thực token → lấy NguoiDung người đặt giá
     *  2. Tìm phiên theo maPhien
     *  3. Gọi PhienDauGiaService.setPrice() — validate toàn bộ và cập nhật state
     *  4. Nếu setPrice thành công, lưu lại phiên vào SQLite
     *
     * Response 200: { "thongBao": "Đặt giá thành công", "giaHienTai": 750000 }
     * Response 400: { "bug": "Giá đặt phải ≥ giaKhoiDiem + buocGia" }
     * Response 401: { "bug": "Cần đăng nhập trước" }
     * Response 404: { "bug": "Không tìm thấy phiên: PH999999" }
     * Response 409: { "bug": "Phiên không ở trạng thái DANG_DIEN_RA" }
     */
    private void handleSetPrice(HttpExchange exchange) throws IOException {
        // Bước 1: Xác thực token → lấy người đặt giá
        User nguoiDat = checkToken(exchange);
        if (nguoiDat == null) return;

        // Bước 2: Đọc và parse body
        String body = readBody(exchange);
        DatGiaRequest req = gson.fromJson(body, DatGiaRequest.class);

        if (req == null || req.getMaPhien() == null || req.getGia() <= 0) {
            guiPhanHoi(exchange, 400, bug("Thiếu hoặc sai thông tin: maPhien, gia (>0)"));
            return;
        }

        // Bước 3: Lấy phiên ĐANG CHẠY từ Registry (object thật trong RAM).
        // QUAN TRỌNG: KHÔNG dùng auctionRepository.findById() vì nó load bản sao từ DB —
        // khi gọi setPrice() trên bản sao đó, Registry vẫn giữ giá cũ, bid tiếp theo sẽ sai.
        AuctionSession phien = auctionSessionRegistry.find(req.getMaPhien());
        if (phien == null) {
            // Phiên không có trong Registry → kiểm tra DB để phân biệt 404 vs 409
            AuctionSession phienDB = auctionRepository.findById(req.getMaPhien());
            if (phienDB == null) {
                guiPhanHoi(exchange, 404, bug("Không tìm thấy phiên: " + req.getMaPhien()));
            } else {
                guiPhanHoi(exchange, 409, bug(
                    "Phiên không ở trạng thái DANG_DIEN_RA. Trạng thái hiện tại: " + phienDB.getStatus().name()));
            }
            return;
        }

        // Bước 4: Kiểm tra nhanh trạng thái (object từ Registry là live state)
        if (phien.getStatus() != SessionStatus.IN_PROGRESS) {
            guiPhanHoi(exchange, 409, bug(
                    "Phiên không ở trạng thái DANG_DIEN_RA. Trạng thái hiện tại: " + phien.getStatus().name()));
            return;
        }

        // Bước 5: Kiểm tra người bán tự đặt giá
        if (nguoiDat.getUserId().equals(phien.getSeller().getUserId())) {
            guiPhanHoi(exchange, 400, bug("Người bán không thể tự đặt giá cho phiên của mình"));
            return;
        }

        // Bước 6: Kiểm tra giá hợp lệ trước khi gọi service
        double giaToiThieu = phien.getCurrentPrice() + phien.getPriceStep();
        if (req.getGia() < giaToiThieu) {
            guiPhanHoi(exchange, 400, bug(String.format(
                    "Giá đặt phải ≥ %.0f (giaHienTai=%.0f + buocGia=%.0f)",
                    giaToiThieu, phien.getCurrentPrice(), phien.getPriceStep())));
            return;
        }
        // Bước 8: Kiểm tra setPrice có thực sự thành công không
        // (service silently return nếu fail — ta so sánh giaHienTai trước/sau)
        boolean success = auctionSessionService.setPrice(phien, nguoiDat, req.getGia());
        if (!success) {
            guiPhanHoi(exchange, 400, bug("Đặt giá thất bại. Vui lòng kiểm tra lại giá hoặc trạng thái phiên."));
            return;
        }
        // Bước 9: Lưu trạng thái phiên đã cập nhật vào SQLite
        auctionRepository.update(phien);

        guiPhanHoi(exchange, 200, gson.toJson(new DatGiaResponse(
                "Đặt giá thành công",
                phien.getCurrentPrice(),
                phien.getEndTime() != null ? phien.getEndTime().toString() : null
        )));
    }

    // =========================================================
    // API 2: GET /api/bids/{phienId}  →  lịch sử đặt giá
    // =========================================================

    /**
     * Trả về lịch sử đặt giá của một phiên (danh sách người đã đặt giá).
     *
     * Lưu ý: danhSachNguoiTraGia trong PhienDauGia hiện lưu theo thứ tự thêm vào.
     *        Người cuối cùng trong danh sách = người đang thắng.
     *        Dữ liệu trong memory — SQLite chưa có bảng riêng cho từng lượt đặt giá.
     *
     * Response 200:
     * {
     *   "maPhien": "PH000001",
     *   "giaHienTai": 750000,
     *   "trangThai": "DANG_DIEN_RA",
     *   "soLuotDatGia": 3,
     *   "nguoiDangThang": "Nguyen Van B",
     *   "lichSu": [
     *     { "stt": 1, "tenNguoiDat": "Nguyen Van B" },
     *     { "stt": 2, "tenNguoiDat": "Tran Thi C" },
     *     { "stt": 3, "tenNguoiDat": "Nguyen Van B" }
     *   ]
     * }
     *
     * Response 404: { "bug": "Không tìm thấy phiên: PH999999" }
     */
    private void handleLayLichSu(HttpExchange exchange, String maPhien) throws IOException {
        if (maPhien == null || maPhien.isBlank()) {
            guiPhanHoi(exchange, 400, bug("Thiếu mã phiên trong URL"));
            return;
        }

        AuctionSession phien = auctionRepository.findById(maPhien);
        if (phien == null) {
            guiPhanHoi(exchange, 404, bug("Không tìm thấy phiên: " + maPhien));
            return;
        }

        // ĐỌC LỊCH SỬ TỪ DB (bảng nguoi_tra_gia) thay vì bidderList trong RAM
        List<LuotDatGia> lichSu = new ArrayList<>();
        String tenNguoiDangThang = null;

        String sql = "SELECT ntg.ma_nguoi_dung, ntg.gia_tra, ntg.thoi_gian, nd.ho_ten " +
            "FROM nguoi_tra_gia ntg " +
            "JOIN nguoi_dung nd ON ntg.ma_nguoi_dung = nd.ma_nguoi_dung " +
            "WHERE ntg.ma_phien = ? " +
            "ORDER BY ntg.thoi_gian ASC";

        try (java.sql.PreparedStatement ps =
                 com.mycompany.utils.DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maPhien);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    String tenNguoiDat = rs.getString("ho_ten");
                    double giaTra      = rs.getDouble("gia_tra");
                    String thoiGianRaw = rs.getString("thoi_gian");
                    // Format: "2026-05-22T12:47:50.123" → "22/05/2026 12:47:50"
                    String thoiGianHienThi = "";
                    try {
                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
                            thoiGianRaw.length() > 19 ? thoiGianRaw.substring(0, 19) : thoiGianRaw);
                        thoiGianHienThi = ldt.format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    } catch (Exception ignored) {
                        thoiGianHienThi = thoiGianRaw != null ? thoiGianRaw : "";
                    }
                    lichSu.add(new LuotDatGia(stt++, tenNguoiDat + " — " +
                        String.format("%,.0f", giaTra) + " VNĐ",
                        rs.getString("ma_nguoi_dung"),
                        thoiGianHienThi));
                    tenNguoiDangThang = tenNguoiDat; // người cuối = người đang thắng
                }
            }
        } catch (java.sql.SQLException e) {
            logger.error("Lỗi đọc nguoi_tra_gia: " + e.getMessage());
        }

        LichSuDatGiaResponse response = new LichSuDatGiaResponse(
            maPhien,
            phien.getCurrentPrice(),
            phien.getStatus().name(),
            lichSu.size(),
            tenNguoiDangThang,
            lichSu
        );

        guiPhanHoi(exchange, 200, gson.toJson(response));
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // =========================================================

    /**
     * Xác thực Bearer token và trả về NguoiDung tương ứng.
     * Token format: "USER_<email>_<timestamp>"
     *
     * @return NguoiDung nếu hợp lệ, null nếu không (đã gửi response lỗi)
     */
    private User checkToken(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            guiPhanHoi(exchange, 401, bug("Cần đăng nhập trước (Authorization: Bearer <token>)"));
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (!token.startsWith("USER_")) {
            guiPhanHoi(exchange, 401, bug("Token không hợp lệ"));
            return null;
        }

        String phan = token.substring("USER_".length());
        int lastUnderscore = phan.lastIndexOf('_');

        if (lastUnderscore < 0) {
            guiPhanHoi(exchange, 401, bug("Token không hợp lệ"));
            return null;
        }

        String email = phan.substring(0, lastUnderscore);

        User nguoiDung = userRepository.findByEmail(email);

        if (nguoiDung == null) {
            guiPhanHoi(exchange, 401, bug("Token không hợp lệ hoặc tài khoản không tồn tại"));
            return null;
        }

        return nguoiDung;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String bug(String thongBao) {
        return gson.toJson(new sendBug(thongBao));
    }

    private void guiPhanHoi(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        // FIX: Gọi exchange.close() để tránh EOF phía client
        exchange.close();
    }
    private static class sendBug {
        String bug;
        sendBug(String bug) { this.bug = bug; }
    }
}