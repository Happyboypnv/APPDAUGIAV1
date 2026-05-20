package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.mycompany.models.User;
import com.mycompany.server.dto.LoginRequest;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.server.dto.RegisterRequest;
import com.mycompany.server.sessionmanager.OnlineUsersManager;
import com.mycompany.server.sessionmanager.OnlineUserSession;
import com.mycompany.utils.PasswordEncoder;
import com.mycompany.utils.IUserRepository;
import com.mycompany.utils.UserRepositorySQLite;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * UserController — Xử lý các HTTP request liên quan đến người dùng.
 *
 * Ba API endpoint:
 *  POST /api/users/login      → Đăng nhập, trả về token (với session validation)
 *  POST /api/users/register   → Đăng ký tài khoản mới
 *  GET  /api/users/{email}    → Lấy thông tin user theo email
 *  POST /api/users/logout     → Đăng xuất (xóa session)
 *
 * MULTI-DEVICE SESSION VALIDATION:
 *  - Maintain OnlineUsersManager để track logged-in users
 *  - Khi login: Check if user already online trên device khác
 *  - Nếu yes: Return ALREADY_IN_USE error
 *  - Nếu no: Create session + add to online users list
 *  - When logout/disconnect: Remove from online users list
 *
 * Lưu ý quan trọng:
 *  - Dùng KhoLuuTruNguoiDungSQLite (SQLite) thay vì JSON
 *  - Mật khẩu được mã hóa bằng SHA-256 + salt (BoMaHoaMatKhau)
 *  - KHÔNG lưu/trả về mật khẩu dạng plain text
 */
public class UserController {

    // ===== PHỤ THUỘC =====

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /** Gson để chuyển đổi JSON ↔ Java object */
    private final Gson gson = new Gson();

    /**
     * Kho lưu trữ người dùng dùng SQLite.
     * Dùng cùng database với phần JavaFX → dữ liệu đồng bộ hoàn toàn.
     */
    private final IUserRepository khoNguoiDung = new UserRepositorySQLite();

    /**
     * Online users manager — Track logged-in users across all devices
     */
    private final OnlineUsersManager onlineUsersManager = OnlineUsersManager.getInstance();

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
     * MULTI-DEVICE SESSION VALIDATION:
     *   1. Check if user already logged in on another device
     *   2. If yes (ALREADY_IN_USE): Return error with existing device info
     *   3. If no: Create new session and add to online users list
     *   4. Return token + session status
     *
     * Quy trình:
     *  1. Đọc email + matKhau từ body
     *  2. Gọi kiemTraNguoiDung() — bên trong sẽ hash matKhau với salt rồi so sánh
     *  3. Check if user already online (multi-device validation)
     *  4. Nếu online: return ALREADY_IN_USE error
     *  5. Nếu offline: tạo token + add to online users + trả 200
     *
     * Response thành công (200):
     *   {
     *     "token": "USER_abc@gmail.com_17141234",
     *     "email": "abc@gmail.com",
     *     "hoTen": "...",
     *     "thongBao": "Đăng nhập thành công",
     *     "sessionStatus": "SUCCESS"
     *   }
     *
     * Response - User already logged in (409 CONFLICT):
     *   {
     *     "sessionStatus": "ALREADY_IN_USE",
     *     "thongBao": "Tài khoản của bạn đang đăng nhập trên thiết bị khác",
     *     "existingDeviceId": "192.168.1.100:xxxxx",
     *     "existingIpAddress": "192.168.1.100"
     *   }
     *
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
        boolean hopLe = khoNguoiDung.verifyCredentials(req.getEmail(), req.getMatKhau());

        if (!hopLe) {
            guiPhanHoi(exchange, 401, gson.toJson(new LoginResponse("Sai email hoặc mật khẩu")));
            return;
        }

        // 🔹 NEW: Multi-Device Session Validation
        // Get client's device info
        String clientIp = getClientIpAddress(exchange);
        String deviceId = generateDeviceId(clientIp);
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");

        // Check if user already logged in on another device
        if (onlineUsersManager.isAlreadyInUse(req.getEmail())) {
            // User already online → Return conflict error
            OnlineUserSession existingSession = onlineUsersManager.getSession(req.getEmail());

            LoginResponse response = new LoginResponse();
            response.setSessionStatus("ALREADY_IN_USE");
            response.setThongBao("⚠️ Tài khoản của bạn đang đăng nhập trên thiết bị khác. Vui lòng đăng xuất thiết bị kia trước hoặc chờ kết nối mất (30 phút).");

            if (existingSession != null) {
                response.setExistingDeviceId(existingSession.getDeviceId());
                response.setExistingIpAddress(existingSession.getIpAddress());
            }

            logger.warn("[LoginController] ⚠️ User {} already logged in on device {}. New login blocked from device {}",
                    req.getEmail(),
                    existingSession != null ? existingSession.getDeviceId() : "unknown",
                    deviceId);

            // Return 409 Conflict
            guiPhanHoi(exchange, 409, gson.toJson(response));
            return;
        }

        // Đăng nhập thành công → tạo token + lưu session
        String token = "USER_" + req.getEmail() + "_" + System.currentTimeMillis();
        User nguoiDung = khoNguoiDung.findByEmail(req.getEmail());
        String hoTen = (nguoiDung != null) ? nguoiDung.getFullName() : "";

        // 🔹 NEW: Add user to online users list
        OnlineUserSession session = onlineUsersManager.addOrReplaceSession(
                req.getEmail(),
                token,
                deviceId,
                clientIp,
                userAgent
        );

        // 🔹 NEW: Broadcast login event via WebSocket (will be implemented later)
        broadcastLoginEvent(req.getEmail(), deviceId, clientIp);

        // Build successful response
        LoginResponse response = new LoginResponse(token, req.getEmail(), hoTen, "Đăng nhập thành công");
        response.setSessionStatus("SUCCESS");

        logger.info("[LoginController] ✅ User {} logged in successfully. Device: {}",
                req.getEmail(), deviceId);

        guiPhanHoi(exchange, 200, gson.toJson(response));
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
        if (!khoNguoiDung.isEmailAvailable(req.getEmail())) {
            guiPhanHoi(exchange, 400,
                    gson.toJson(new LoginResponse("Email đã tồn tại trong hệ thống")));
            return;
        }

        // Tạo salt ngẫu nhiên + hash mật khẩu (giống LoginAction.dangKy() trong JavaFX)
        String salt           = PasswordEncoder.createSalt();
        String matKhauDaHash  = PasswordEncoder.passwordEncoder(req.getMatKhau(), salt);

        // Tạo NguoiDung với mật khẩu đã hash
        User nguoiDungMoi = new User(
                req.getHoTen(),
                req.getEmail(),
                matKhauDaHash,   // lưu hash, KHÔNG lưu plain text
                req.getNgaySinh()
        );
        // Set salt vào object để KhoLuuTruNguoiDungSQLite lưu vào DB
        nguoiDungMoi.setSalt(salt);

        khoNguoiDung.save(nguoiDungMoi);

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
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        /// khi gui request no gui kem mot header co dang Authorization: Bearer USER_abc@gmail.com_1714123456
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            guiPhanHoi(exchange, 401, "{\"thongBao\":\"Cần đăng nhập trước\"}");
            return;
        }
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

        User nguoiDung = khoNguoiDung.findByEmail(email);
        if (nguoiDung == null) {
            guiPhanHoi(exchange, 404,
                    gson.toJson(new LoginResponse("Không tìm thấy người dùng: " + email)));
            return;
        }

        // Trả về thông tin public — KHÔNG bao gồm matKhau và salt
        guiPhanHoi(exchange, 200, gson.toJson(new ThongTinNguoiDung(nguoiDung)));
    }

    // =========================================================
    // API 4: POST /api/users/logout  →  đăng xuất
    // =========================================================

    /**
     * Xử lý đăng xuất người dùng.
     *
     * Request:
     *   Method : POST
     *   URL    : /api/users/logout
     *   Headers: Authorization: Bearer USER_abc@gmail.com_1714123456
     *
     * MULTI-DEVICE SESSION HANDLING:
     *   1. Get email from token
     *   2. Remove user from online users list
     *   3. Mark session as LOGGED_OUT
     *   4. Broadcast logout event via WebSocket
     *   5. Return success response
     *
     * Response thành công (200):
     *   { "thongBao": "Đăng xuất thành công" }
     *
     * Response thất bại (401):
     *   { "thongBao": "Cần đăng nhập trước" }
     */
    public void handleLogout(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            guiPhanHoi(exchange, 405, "{\"thongBao\":\"Chỉ chấp nhận POST\"}");
            return;
        }

        // Get token from Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            guiPhanHoi(exchange, 401, "{\"thongBao\":\"Cần đăng nhập trước\"}");
            return;
        }

        // Extract token
        String token = authHeader.substring("Bearer ".length());

        // Parse email from token (format: USER_email_timestamp)
        // Simple parsing: format is "USER_<email>_<timestamp>"
        try {
            String[] parts = token.split("_");
            if (parts.length < 2) {
                guiPhanHoi(exchange, 401, "{\"thongBao\":\"Token không hợp lệ\"}");
                return;
            }

            // Reconstruct email (may contain underscores)
            // Token format: USER_<email>_<timestamp>
            int emailEnd = token.lastIndexOf("_");
            if (emailEnd <= 5) {  // "USER_".length() = 5
                guiPhanHoi(exchange, 401, "{\"thongBao\":\"Token không hợp lệ\"}");
                return;
            }

            String email = token.substring(5, emailEnd);  // Skip "USER_" and timestamp

            // 🔹 NEW: Remove user from online users list
            logger.info("[LoginController] 🚪 User {} logging out...", email);
            onlineUsersManager.removeSession(email);

            // 🔹 NEW: Broadcast logout event via WebSocket (will be implemented later)
            broadcastLogoutEvent(email);

            logger.info("[LoginController] ✅ User {} successfully logged out", email);

            guiPhanHoi(exchange, 200, "{\"thongBao\":\"Đăng xuất thành công\"}");

        } catch (Exception e) {
            logger.error("[LoginController] Error parsing logout token: " + e.getMessage());
            guiPhanHoi(exchange, 400, "{\"thongBao\":\"Lỗi xử lý đăng xuất\"}");
        }
    }

    /**
     * Broadcast logout event via WebSocket (optional)
     *
     * @param email User's email
     */
    private void broadcastLogoutEvent(String email) {
        try {
            // TODO: Broadcast logout event via WebSocket server
            // Example:
            // AuctionWebSocketServer server = AuctionWebSocketServerStarter.getServer();
            // if (server != null) {
            //     JsonObject event = new JsonObject();
            //     event.addProperty("event", "USER_LOGOUT");
            //     event.addProperty("email", email);
            //     event.addProperty("timestamp", System.currentTimeMillis());
            //     server.broadcast(gson.toJson(event));
            // }
        } catch (Exception e) {
            logger.warn("[LoginController] Failed to broadcast logout event: " + e.getMessage());
        }
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

        ThongTinNguoiDung(User nd) {
            this.maNguoiDung  = nd.getUserId();
            this.hoTen        = nd.getFullName();
            this.email        = nd.getEmail();
            this.ngaySinh     = nd.getDateOfBirth();
            this.diaChi       = nd.getAddress();
            this.soDienThoai  = nd.getPhoneNumber();
            this.soDuKhaDung  = nd.getAvailableBalance();
        }
    }

    // =========================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // =========================================================

    /**
     * Lấy IP address của client từ HTTP request headers
     *
     * @param exchange HttpExchange object
     * @return Client IP address (hoặc "unknown" nếu không lấy được)
     */
    private String getClientIpAddress(HttpExchange exchange) {
        try {
            // 🔹 BƯỚC 1: Check X-Forwarded-For (proxy header)
            String xForwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            // 🔹 BƯỚC 2: Check X-Real-IP (nginx reverse proxy)
            String xRealIp = exchange.getRequestHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp.trim();
            }

            // 🔹 BƯỚC 3: Get from remote socket address
            if (exchange.getRemoteAddress() != null) {
                String address = exchange.getRemoteAddress().getAddress().getHostAddress();
                if (address != null && !address.isEmpty()) {
                    return address;
                }
            }

            return "unknown";
        } catch (Exception e) {
            logger.error("[UserController] Error getting client IP: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Generate unique device ID from client IP + port + timestamp
     *
     * @param ipAddress Client IP address
     * @return Device ID string
     */
    private String generateDeviceId(String ipAddress) {
        long timestamp = System.currentTimeMillis();
        return ipAddress + "_" + (timestamp % 100000);  // Last 5 digits of timestamp
    }

    /**
     * Broadcast login event via WebSocket (optional)
     * Notifies other connected clients about new login
     *
     * @param email User's email
     * @param deviceId Device ID
     * @param ipAddress Client IP
     */
    private void broadcastLoginEvent(String email, String deviceId, String ipAddress) {
        try {
            // TODO: Broadcast via WebSocket server
            // Example:
//             AuctionWebSocketServer server = AuctionWebSocketServerStarter.getServer();
//             if (server != null) {
//                 JsonObject event = new JsonObject();
//                 event.addProperty("event", "USER_LOGIN");
//                 event.addProperty("email", email);
//                 event.addProperty("deviceId", deviceId);
//                 event.addProperty("timestamp", System.currentTimeMillis());
//                 server.broadcast(gson.toJson(event));
            // }
        } catch (Exception e) {
            logger.warn("[UserController] Failed to broadcast login event: " + e.getMessage());
        }
    }

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
        exchange.getResponseHeaders().add("Content-Type","application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        // FIX: Gọi exchange.close() để tránh EOF phía client
        exchange.close();
    }
}