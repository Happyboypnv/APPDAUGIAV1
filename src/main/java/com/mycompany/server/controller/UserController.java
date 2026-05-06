package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.mycompany.models.NguoiDung;
import com.mycompany.server.dto.LoginRequest;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.server.dto.RegisterRequest;
import com.mycompany.utils.BoMaHoaMatKhau;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * UserController — Xử lý các HTTP request liên quan đến người dùng.
 *
 * Ba API endpoint:
 *  POST /api/users/login      → Đăng nhập, trả về token
 *  POST /api/users/register   → Đăng ký tài khoản mới
 *  GET  /api/users/{email}    → Lấy thông tin user theo email
 *
 * Lưu ý quan trọng:
 *  - Dùng KhoLuuTruNguoiDungSQLite (SQLite) thay vì JSON
 *  - Mật khẩu được mã hóa bằng SHA-256 + salt (BoMaHoaMatKhau)
 *  - KHÔNG lưu/trả về mật khẩu dạng plain text
 */
public class UserController {

    // ===== PHỤ THUỘC =====

    /** Gson để chuyển đổi JSON ↔ Java object */
    private final Gson gson = new Gson();

    /**
     * Kho lưu trữ người dùng dùng SQLite.
     * Dùng cùng database với phần JavaFX → dữ liệu đồng bộ hoàn toàn.
     */
    private final IKhoLuuTruNguoiDung khoNguoiDung = new KhoLuuTruNguoiDungSQLite();

    // =========================================================
    // API 1: POST /api/users/login  →  trả về token
    // =========================================================

    /**
     * Xử lý đăng nhập người dùng.
     *
     * Request:
     *   Method : POST
     *   URL    : /api/users/login
     *   Body   : { "email": "abc@gmail.com", "matKhau": "123456" }
     *
     * Quy trình:
     *  1. Đọc email + matKhau từ body
     *  2. Gọi kiemTraNguoiDung() — bên trong sẽ hash matKhau với salt rồi so sánh
     *  3. Nếu đúng → tạo token + trả 200
     *  4. Nếu sai → trả 401
     *
     * Response thành công (200):
     *   { "token": "USER_abc@gmail.com_17141234", "email": "...", "hoTen": "...", "thongBao": "Đăng nhập thành công" }
     * Response thất bại (401):
     *   { "thongBao": "Sai email hoặc mật khẩu" }
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            guiPhanHoi(exchange, 405, "{\"thongBao\":\"Chỉ chấp nhận POST\"}");
            return;
        }

        // Đọc và parse JSON body
        String body = docBody(exchange);
        LoginRequest req = gson.fromJson(body, LoginRequest.class);

        if (req == null || req.getEmail() == null || req.getMatKhau() == null) {
            guiPhanHoi(exchange, 400, gson.toJson(new LoginResponse("Thiếu email hoặc mật khẩu")));
            return;
        }

        // kiemTraNguoiDung() tự động hash matKhau với salt rồi so sánh với DB
        boolean hopLe = khoNguoiDung.kiemTraNguoiDung(req.getEmail(), req.getMatKhau());

        if (!hopLe) {
            guiPhanHoi(exchange, 401, gson.toJson(new LoginResponse("Sai email hoặc mật khẩu")));
            return;
        }

        // Đăng nhập thành công → tạo token + lấy họ tên
        String token = "USER_" + req.getEmail() + "_" + System.currentTimeMillis();
        Map<String, NguoiDung> danhSach = khoNguoiDung.layTatCa();
        NguoiDung nguoiDung = danhSach.get(req.getEmail());
        String hoTen = (nguoiDung != null) ? nguoiDung.layHoTen() : "";

        guiPhanHoi(exchange, 200,
                gson.toJson(new LoginResponse(token, req.getEmail(), hoTen, "Đăng nhập thành công")));
    }

    // =========================================================
    // API 2: POST /api/users/register  →  đăng ký tài khoản
    // =========================================================

    /**
     * Xử lý đăng ký tài khoản mới.
     *
     * Request:
     *   Method : POST
     *   URL    : /api/users/register
     *   Body   : { "hoTen": "...", "email": "...", "matKhau": "...", "ngaySinh": "2000-01-15" }
     *
     * Quy trình:
     *  1. Kiểm tra các trường bắt buộc
     *  2. Kiểm tra email chưa tồn tại
     *  3. Tạo salt ngẫu nhiên + hash mật khẩu bằng BoMaHoaMatKhau
     *  4. Tạo NguoiDung với mật khẩu đã hash + set salt
     *  5. Lưu vào SQLite qua khoNguoiDung.luu()
     *
     * Response thành công (201): { "thongBao": "Đăng ký thành công" }
     * Response thất bại (400):   { "thongBao": "Email đã tồn tại" }
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            guiPhanHoi(exchange, 405, "{\"thongBao\":\"Chỉ chấp nhận POST\"}");
            return;
        }

        String body = docBody(exchange);
        RegisterRequest req = gson.fromJson(body, RegisterRequest.class);

        // Kiểm tra các trường bắt buộc
        if (req == null || req.getHoTen() == null || req.getEmail() == null
                || req.getMatKhau() == null || req.getNgaySinh() == null) {
            guiPhanHoi(exchange, 400,
                    gson.toJson(new LoginResponse("Thiếu thông tin bắt buộc (hoTen, email, matKhau, ngaySinh)")));
            return;
        }

        // Kiểm tra email đã tồn tại chưa
        // kiemTraEmail() trả true nếu email CHƯA có → cho phép đăng ký
        if (!khoNguoiDung.kiemTraEmail(req.getEmail())) {
            guiPhanHoi(exchange, 400,
                    gson.toJson(new LoginResponse("Email đã tồn tại trong hệ thống")));
            return;
        }

        // Tạo salt ngẫu nhiên + hash mật khẩu (giống LoginAction.dangKy() trong JavaFX)
        String salt           = BoMaHoaMatKhau.taoSalt();
        String matKhauDaHash  = BoMaHoaMatKhau.maHoaMatKhau(req.getMatKhau(), salt);

        // Tạo NguoiDung với mật khẩu đã hash
        NguoiDung nguoiDungMoi = new NguoiDung(
                req.getHoTen(),
                req.getEmail(),
                matKhauDaHash,   // lưu hash, KHÔNG lưu plain text
                req.getNgaySinh()
        );
        // Set salt vào object để KhoLuuTruNguoiDungSQLite lưu vào DB
        nguoiDungMoi.setSalt(salt);

        khoNguoiDung.luu(nguoiDungMoi);

        guiPhanHoi(exchange, 201, gson.toJson(new LoginResponse("Đăng ký thành công")));
    }

    // =========================================================
    // API 3: GET /api/users/{email}  →  lấy thông tin user
    // =========================================================

    /**
     * Lấy thông tin người dùng theo email (email dùng làm ID trong URL).
     *
     * Request:
     *   Method : GET
     *   URL    : /api/users/abc@gmail.com
     *
     * Response thành công (200):
     *   { "maNguoiDung": "...", "hoTen": "...", "email": "...", "ngaySinh": "..." }
     *
     * Lưu ý: KHÔNG trả về matKhau hay salt — thông tin nhạy cảm.
     *
     * Response thất bại (404): { "thongBao": "Không tìm thấy người dùng" }
     */
    public void handleGetUser(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            guiPhanHoi(exchange, 405, "{\"thongBao\":\"Chỉ chấp nhận GET\"}");
            return;
        }

        // Lấy email từ URL: /api/users/abc@gmail.com → "abc@gmail.com"
        String path   = exchange.getRequestURI().getPath();
        String prefix = "/api/users/";

        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            guiPhanHoi(exchange, 400, "{\"thongBao\":\"Thiếu email trong URL\"}");
            return;
        }

        String email = path.substring(prefix.length());

        // Tìm người dùng trong SQLite
        Map<String, NguoiDung> danhSach = khoNguoiDung.layTatCa();
        NguoiDung nguoiDung = danhSach.get(email);

        if (nguoiDung == null) {
            guiPhanHoi(exchange, 404,
                    gson.toJson(new LoginResponse("Không tìm thấy người dùng: " + email)));
            return;
        }

        // Trả về thông tin public — KHÔNG bao gồm matKhau và salt
        guiPhanHoi(exchange, 200, gson.toJson(new ThongTinNguoiDung(nguoiDung)));
    }

    // =========================================================
    // INNER CLASS: Thông tin trả về cho GET /api/users/{email}
    // =========================================================

    /**
     * Chỉ chứa thông tin public của NguoiDung.
     * KHÔNG có matKhau, salt — tránh lộ thông tin nhạy cảm.
     */
    private static class ThongTinNguoiDung {
        String maNguoiDung;
        String hoTen;
        String email;
        String ngaySinh;
        String diaChi;
        String soDienThoai;
        double soDuKhaDung;

        ThongTinNguoiDung(NguoiDung nd) {
            this.maNguoiDung  = nd.layMaNguoiDung();
            this.hoTen        = nd.layHoTen();
            this.email        = nd.layThuDienTu();
            this.ngaySinh     = nd.layNgaySinh();
            this.diaChi       = nd.getDiaChi();
            this.soDienThoai  = nd.getSoDienThoai();
            this.soDuKhaDung  = nd.getSoDuKhaDung();
        }
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // =========================================================

    /** Đọc toàn bộ body từ HTTP request */
    private String docBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Ghi HTTP response ra cho client.
     * Tự động thêm Content-Type: application/json và CORS headers.
     */
    private void guiPhanHoi(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type",                 "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}