package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.action.PhienDauGiaService;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.TrangThaiPhien;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.IKhoLuuTruPhienDauGia;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import com.mycompany.utils.KhoLuuTruPhienDauGiaSQLite;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BidController — Xử lý các HTTP request liên quan đến đặt giá (bid).
 *
 * Hai API endpoint:
 *  POST /api/bids               → Đặt giá (validate + lưu thông qua PhienDauGiaService)
 *  GET  /api/bids/{phienId}     → Lịch sử đặt giá của một phiên
 *
 * Tái sử dụng:
 *  - PhienDauGiaService.datGia()   (singleton, có sẵn) — toàn bộ validate + cập nhật state
 *  - KhoLuuTruPhienDauGiaSQLite    (có sẵn)            — đọc / ghi phiên
 *  - KhoLuuTruNguoiDungSQLite      (có sẵn)            — tra cứu người đặt giá
 *
 * Validate do PhienDauGiaService.datGia() đảm nhiệm:
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

    // ===== PHỤ THUỘC =====

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /** Kho phiên đấu giá — tái sử dụng implementation SQLite đã có */
    private final IKhoLuuTruPhienDauGia khoPhien = new KhoLuuTruPhienDauGiaSQLite();

    /** Kho người dùng — để tra cứu người đặt giá từ token */
    private final IKhoLuuTruNguoiDung khoNguoiDung = new KhoLuuTruNguoiDungSQLite();

    /** Service đấu giá — tái sử dụng singleton đã có, KHÔNG tạo mới */
    private final PhienDauGiaService phienDauGiaService = PhienDauGiaService.getInstance();

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
            handleDatGia(exchange);
            return;
        }

        // GET /api/bids/{phienId} → lịch sử đặt giá
        if (method.equals("GET") && path.startsWith("/api/bids/")) {
            String maPhien = path.substring("/api/bids/".length());
            handleLayLichSu(exchange, maPhien);
            return;
        }

        guiPhanHoi(exchange, 404, loi("Endpoint không tồn tại: " + method + " " + path));
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
     *  3. Gọi PhienDauGiaService.datGia() — validate toàn bộ và cập nhật state
     *  4. Nếu datGia thành công, lưu lại phiên vào SQLite
     *
     * Response 200: { "thongBao": "Đặt giá thành công", "giaHienTai": 750000 }
     * Response 400: { "loi": "Giá đặt phải ≥ giaKhoiDiem + buocGia" }
     * Response 401: { "loi": "Cần đăng nhập trước" }
     * Response 404: { "loi": "Không tìm thấy phiên: PH999999" }
     * Response 409: { "loi": "Phiên không ở trạng thái DANG_DIEN_RA" }
     */
    private void handleDatGia(HttpExchange exchange) throws IOException {
        // Bước 1: Xác thực token → lấy người đặt giá
        NguoiDung nguoiDat = xacThucToken(exchange);
        if (nguoiDat == null) return;

        // Bước 2: Đọc và parse body
        String body = docBody(exchange);
        DatGiaRequest req = gson.fromJson(body, DatGiaRequest.class);

        if (req == null || req.maPhien == null || req.gia <= 0) {
            guiPhanHoi(exchange, 400, loi("Thiếu hoặc sai thông tin: maPhien, gia (>0)"));
            return;
        }

        // Bước 3: Tìm phiên
        PhienDauGia phien = khoPhien.layPhienDauGia(req.maPhien);
        if (phien == null) {
            guiPhanHoi(exchange, 404, loi("Không tìm thấy phiên: " + req.maPhien));
            return;
        }

        // Bước 4: Kiểm tra nhanh trạng thái trước khi gọi service
        // (service cũng kiểm tra bên trong, nhưng ta cần phân biệt lỗi để trả HTTP code đúng)
        if (phien.getTrangThai() != TrangThaiPhien.DANG_DIEN_RA) {
            guiPhanHoi(exchange, 409, loi(
                    "Phiên không ở trạng thái DANG_DIEN_RA. Trạng thái hiện tại: " + phien.getTrangThai().name()));
            return;
        }

        // Bước 5: Kiểm tra người bán tự đặt giá
        if (nguoiDat.layMaNguoiDung().equals(phien.getNguoiBan().layMaNguoiDung())) {
            guiPhanHoi(exchange, 400, loi("Người bán không thể tự đặt giá cho phiên của mình"));
            return;
        }

        // Bước 6: Kiểm tra giá hợp lệ trước khi gọi service
        double giaToiThieu = phien.getGiaHienTai() + phien.getBuocGia();
        if (req.gia < giaToiThieu) {
            guiPhanHoi(exchange, 400, loi(String.format(
                    "Giá đặt phải ≥ %.0f (giaHienTai=%.0f + buocGia=%.0f)",
                    giaToiThieu, phien.getGiaHienTai(), phien.getBuocGia())));
            return;
        }

        // Bước 7: Gọi PhienDauGiaService.datGia() — tái sử dụng logic đã có
        // datGia() xử lý: cập nhật giaHienTai, thêm vào danhSachNguoiTraGia, gia hạn nếu cần
        double giaLucDau = phien.getGiaHienTai();
        phienDauGiaService.datGia(phien, nguoiDat, req.gia);

        // Bước 8: Kiểm tra datGia có thực sự thành công không
        // (service silently return nếu fail — ta so sánh giaHienTai trước/sau)
        if (phien.getGiaHienTai() == giaLucDau) {
            guiPhanHoi(exchange, 400, loi("Đặt giá thất bại. Vui lòng kiểm tra lại giá hoặc trạng thái phiên."));
            return;
        }

        // Bước 9: Lưu trạng thái phiên đã cập nhật vào SQLite
        khoPhien.capNhatPhienDauGia(phien);

        guiPhanHoi(exchange, 200, gson.toJson(new DatGiaResponse(
                "Đặt giá thành công",
                phien.getGiaHienTai(),
                phien.getThoiGianKetThuc() != null ? phien.getThoiGianKetThuc().toString() : null
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
     * Response 404: { "loi": "Không tìm thấy phiên: PH999999" }
     */
    private void handleLayLichSu(HttpExchange exchange, String maPhien) throws IOException {
        if (maPhien == null || maPhien.isBlank()) {
            guiPhanHoi(exchange, 400, loi("Thiếu mã phiên trong URL"));
            return;
        }

        PhienDauGia phien = khoPhien.layPhienDauGia(maPhien);
        if (phien == null) {
            guiPhanHoi(exchange, 404, loi("Không tìm thấy phiên: " + maPhien));
            return;
        }

        List<NguoiDung> danhSach = phien.getDanhSachNguoiTraGia();

        // Xây dựng lịch sử — mỗi entry là một lượt đặt giá
        List<LuotDatGia> lichSu = new ArrayList<>();
        for (int i = 0; i < danhSach.size(); i++) {
            lichSu.add(new LuotDatGia(i + 1, danhSach.get(i).layHoTen(), danhSach.get(i).layMaNguoiDung()));
        }

        // Người đang thắng = người cuối trong danh sách
        String tenNguoiDangThang = null;
        if (!danhSach.isEmpty()) {
            tenNguoiDangThang = danhSach.get(danhSach.size() - 1).layHoTen();
        }

        LichSuDatGiaResponse response = new LichSuDatGiaResponse(
                maPhien,
                phien.getGiaHienTai(),
                phien.getTrangThai().name(),
                danhSach.size(),
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
    private NguoiDung xacThucToken(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            guiPhanHoi(exchange, 401, loi("Cần đăng nhập trước (Authorization: Bearer <token>)"));
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (!token.startsWith("USER_")) {
            guiPhanHoi(exchange, 401, loi("Token không hợp lệ"));
            return null;
        }

        String phan = token.substring("USER_".length());
        int lastUnderscore = phan.lastIndexOf('_');

        if (lastUnderscore < 0) {
            guiPhanHoi(exchange, 401, loi("Token không hợp lệ"));
            return null;
        }

        String email = phan.substring(0, lastUnderscore);

        Map<String, NguoiDung> danhSach = khoNguoiDung.layTatCa();
        NguoiDung nguoiDung = danhSach.get(email);

        if (nguoiDung == null) {
            guiPhanHoi(exchange, 401, loi("Token không hợp lệ hoặc tài khoản không tồn tại"));
            return null;
        }

        return nguoiDung;
    }

    private String docBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String loi(String thongBao) {
        return gson.toJson(new ThongBaoLoi(thongBao));
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
    }

    // =========================================================
    // INNER CLASSES: DTO
    // =========================================================

    /** Request body cho POST /api/bids */
    private static class DatGiaRequest {
        String maPhien;
        double gia;
    }

    /** Response cho POST /api/bids thành công */
    private static class DatGiaResponse {
        String thongBao;
        double giaHienTai;
        String thoiGianKetThuc;

        DatGiaResponse(String thongBao, double giaHienTai, String thoiGianKetThuc) {
            this.thongBao       = thongBao;
            this.giaHienTai     = giaHienTai;
            this.thoiGianKetThuc = thoiGianKetThuc;
        }
    }

    /** Một lượt đặt giá trong lịch sử */
    private static class LuotDatGia {
        int    stt;
        String tenNguoiDat;
        String maNguoiDat;

        LuotDatGia(int stt, String tenNguoiDat, String maNguoiDat) {
            this.stt          = stt;
            this.tenNguoiDat  = tenNguoiDat;
            this.maNguoiDat   = maNguoiDat;
        }
    }

    /** Response cho GET /api/bids/{phienId} */
    private static class LichSuDatGiaResponse {
        String          maPhien;
        double          giaHienTai;
        String          trangThai;
        int             soLuotDatGia;
        String          nguoiDangThang;
        List<LuotDatGia> lichSu;

        LichSuDatGiaResponse(String maPhien, double giaHienTai, String trangThai,
                              int soLuotDatGia, String nguoiDangThang, List<LuotDatGia> lichSu) {
            this.maPhien        = maPhien;
            this.giaHienTai     = giaHienTai;
            this.trangThai      = trangThai;
            this.soLuotDatGia   = soLuotDatGia;
            this.nguoiDangThang = nguoiDangThang;
            this.lichSu         = lichSu;
        }
    }

    private static class ThongBaoLoi {
        String loi;
        ThongBaoLoi(String loi) { this.loi = loi; }
    }
}