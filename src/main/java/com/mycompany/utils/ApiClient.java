package com.mycompany.utils;

import com.google.gson.Gson;
import com.mycompany.server.controller.AuctionController;
import com.mycompany.server.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
/**
 * Hiện tại JavaFX đang gọi thẳng vào database:
 * [SignInController] → [LoginAction] → [KhoLuuTruNguoiDungSQLite] → [SQLite]
 * Sau khi có server, luồng phải đổi thành:
 * [SignInController] → [LoginAction] → [ApiClient] → [HTTP] → [Server] → [SQLite]
 */

/**
 * ApiClient — JavaFX gọi server qua HTTP thay vì trực tiếp vào DB.
 *
 * Mỗi phương thức = 1 endpoint trên server:
 *   login()    → POST /api/users/login
 *   register() → POST /api/users/register
 *   getUser()  → GET  /api/users/{email}
 */
public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    // Địa chỉ server — đổi IP này nếu server chạy trên máy khác
    private static final String BASE_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();

    // ============================================================
    // ĐĂNG XUẤT
    // ============================================================

    /**
     * Gọi POST /api/users/logout
     *
     * MULTI-DEVICx`E SESSION HANDLING:
     *   - Notifies server to remove user from online users list
     *   - Marks session as LOGGED_OUT
     *   - Broadcasts logout event (optional, via WebSocket)
     *   - Allows other devices to login to same account
     *
     * @param token JWT token của user hiện tại
     * @return LoginResponse chứa kết quả logout
     */
    public static LoginResponse logout(String token) {
        // Bước 1: Gửi POST request với token
        String responseJson = guiPost("/api/users/logout", "{}", token);

        // Bước 2: Parse response
        if (responseJson == null) {
            logger.warn("[ApiClient] ⚠️ Logout request failed (no response from server)");
            return new LoginResponse("Không kết nối được server");
        }

        try {
            LoginResponse response = gson.fromJson(responseJson, LoginResponse.class);
            logger.info("[ApiClient] ✅ Logout successful");
            return response;
        } catch (Exception e) {
            logger.error("[ApiClient] Error parsing logout response: " + e.getMessage());
            return new LoginResponse("Lỗi xử lý phản hồi từ server");
        }
    }

    // ============================================================
    // ĐĂNG NHẬP
    // ============================================================

    /**
     * Gọi POST /api/users/login
     *
     * MULTI-DEVICE SESSION HANDLING:
     *   - Server checks if user already logged in on another device
     *   - If yes: Returns response with sessionStatus="ALREADY_IN_USE"
     *   - If no: Returns response with sessionStatus="SUCCESS" + token
     *   - Client must check sessionStatus before accepting login
     *
     * @param email    email người dùng
     * @param matKhau  mật khẩu plain text
     * @return LoginResponse chứa token (SUCCESS) hoặc conflict info (ALREADY_IN_USE)
     */
    public static LoginResponse login(String email, String matKhau) {
        // Bước 1: Tạo object request rồi chuyển thành JSON
        LoginRequest req = new LoginRequest(email, matKhau);
        String jsonBody = gson.toJson(req);

        // Bước 2: Gửi HTTP POST và nhận response
        String responseJson = guiPost("/api/users/login", jsonBody, null);

        // Bước 3: Chuyển JSON response thành object Java
        if (responseJson == null) return new LoginResponse("Không kết nối được server");

        try {
            LoginResponse response = gson.fromJson(responseJson, LoginResponse.class);

            // Log session status if present
            if (response != null && response.getSessionStatus() != null) {
                logger.info("[ApiClient] Login response status: {}", response.getSessionStatus());

                if (response.getSessionStatus().equals("ALREADY_IN_USE")) {
                    logger.warn("[ApiClient] ⚠️ User already logged in on another device: {}",
                            response.getExistingDeviceId());
                }
            }

            return response;
        } catch (Exception e) {
            logger.error("[ApiClient] Error parsing login response: " + e.getMessage());
            return new LoginResponse("Lỗi xử lý phản hồi từ server");
        }
    }

    // ============================================================
    // ĐĂNG KÝ
    // ============================================================

    /**
     * Gọi POST /api/users/register
     *
     * @param hoTen     họ tên
     * @param email     email
     * @param matKhau   mật khẩu plain text
     * @param ngaySinh  ngày sinh dạng "2000-01-15"
     * @return LoginResponse chứa thongBao kết quả
     */
    public static LoginResponse register(String hoTen, String email,
                                         String matKhau, String ngaySinh) {
        RegisterRequest req = new RegisterRequest(hoTen, email, matKhau, ngaySinh);
        String jsonBody = gson.toJson(req);

        String responseJson = guiPost("/api/users/register", jsonBody, null);

        if (responseJson == null) return new LoginResponse("Không kết nối được server");
        return gson.fromJson(responseJson, LoginResponse.class);
    }
    public static DatGiaResponse createBid(String maPhien, double gia, String token) {
        try {
            // Build request body explicitly (avoid anonymous initializer issues)
            DatGiaRequest request = new DatGiaRequest(maPhien,gia);
            
            String jsonBody = gson.toJson(request);
            if (jsonBody == null || jsonBody.equals("null")) {
                logger.error("[createBid] ❌ JSON serialization failed, got null");
                return null;
            }
            
            String responseJson = guiPost("/api/bids", jsonBody, token);
            if (responseJson == null) return null;
            return gson.fromJson(responseJson, DatGiaResponse.class);
        } catch (Exception e) {
            logger.error("[createBid] ❌ Exception in createBid: " + e.getMessage(), e);
            return null;
        }
    }
    public static List<PhienDauGiaDTO> getAuctions() {
        String responseJson = guiGet("/api/auctions", null);
        if (responseJson == null) return new java.util.ArrayList<>();
        java.lang.reflect.Type listType =
                new com.google.gson.reflect.TypeToken<List<PhienDauGiaDTO>>(){}.getType();
        try {
            return gson.fromJson(responseJson, listType);
        } catch (Exception e) {
            logger.error("[ApiClient] Lỗi parse auctions: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * goi api get all auctions
     * @param token
     * @return
     */
    public static String getAuctions(String token){
        return guiGet("/api/auctions", token);
    }
    // ============================================================
    // LẤY THÔNG TIN USER
    // ============================================================
    /**
     * Gọi POST /api/auctions để tạo phiên đấu giá mới.
     * Yêu cầu token hợp lệ trong header Authorization.
     *
     * @param tenPhien      Tên phiên đấu giá
     * @param tenSanPham    Tên sản phẩm
     * @param maSanPham     Mã sản phẩm (tự sinh)
     * @param giaKhoiDiem   Giá khởi điểm (VNĐ)
     * @param thoiGianGiay  Thời gian phiên tính bằng giây
     * @param token         Token xác thực người dùng
     * @return true nếu tạo thành công (HTTP 201), false nếu thất bại
     */
    // Thêm 2 tham số moTa và danhMuc
    public static boolean createAuction(String tenPhien, String tenSanPham, String maSanPham,
                                        String danhMuc, String moTa,         // ← thêm 2 dòng này
                                        double giaKhoiDiem, int thoiGianGiay, String token) {
        try {
            // Build request body explicitly (avoid anonymous initializer issues)
            AuctionController.TaoPhienRequest request = new AuctionController.TaoPhienRequest(tenPhien,tenSanPham,maSanPham,danhMuc,moTa,giaKhoiDiem,thoiGianGiay);

            String jsonBody = gson.toJson(request);
            if (jsonBody == null || jsonBody.equals("null")) {
                logger.error("[createAuction] ❌ JSON serialization failed, got null");
                return false;
            }

            logger.info("[createAuction] JSON gửi lên: " + jsonBody);
            logger.info("[createAuction] Token: " + token);
            String responseJson = guiPost("/api/auctions", jsonBody, token);

            if (responseJson == null) return false;
            logger.info("[createAuction] Server trả về: " + responseJson);
            try {
                com.google.gson.JsonObject obj = gson.fromJson(responseJson, com.google.gson.JsonObject.class);
                if (!obj.has("maPhien")) {
                    // Log ra lỗi thật sự từ server thay vì im lặng
                    logger.error("[ApiClient] Server từ chối tạo phiên: " + responseJson);
                    return false;
                }
                return true;
            } catch (Exception e) {
                logger.error("[ApiClient] Lỗi parse createAuction response: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("[createAuction] ❌ Exception in createAuction: " + e.getMessage(), e);
            return false;
        }
    }
    public static PhienDauGiaDTO getAuctionById(String maPhien, String token) {
        String responseJson = guiGet("/api/auctions/" + maPhien, token);
        if (responseJson == null) return null;
        try {
            return gson.fromJson(responseJson, PhienDauGiaDTO.class);
        } catch (Exception e) {
            logger.error("[ApiClient] Lỗi parse auction: " + e.getMessage());
            return null;
        }
    }
    /**
     * Gọi GET /api/users/{email}
     * Cần token trong header Authorization
     *
     * @param email  email người cần lấy thông tin
     * @param token  token nhận được sau khi đăng nhập
     * @return JSON string chứa thông tin user
     */

    public static String getUser(String email, String token) {
        return guiGet("/api/users/" + email, token);
    }



    // ============================================================
    // PHƯƠNG THỨC HỖ TRỢ (private)
    // ============================================================
    /**
     * Gửi HTTP POST request
     *
     * @param path      đường dẫn endpoint, ví dụ "/api/users/login"
     * @param jsonBody  body dạng JSON string
     * @param token     token xác thực (null nếu không cần)
     * @return response body dạng String, hoặc null nếu lỗi
     */
    private static String guiPost(String path, String jsonBody, String token) {
        try {
            // 1. Mở kết nối đến server
            URL url = new URL(BASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set connection timeout to detect unreachable server sooner
            conn.setConnectTimeout(5000);  // 5 second timeout
            conn.setReadTimeout(5000);

            // 2. Cấu hình: method POST, có body, nhận JSON
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true); // cho phép gửi body

            // 3. Thêm token nếu có
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            // 4. Ghi body vào request
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            // 5. Đọc response trả về
            int statusCode = conn.getResponseCode();

            // Nếu status 4xx/5xx thì đọc error stream thay vì input stream
            InputStream is = (statusCode >= 200 && statusCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (is == null) return null;

            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();
            return response;

        } catch (java.net.ConnectException ce) {
            // Connection refused - server not responding
            String errorMsg = "[ApiClient] ❌ Cannot connect to server at " + BASE_URL +
                            " (Connection refused). Make sure the server is running on port 8080.";
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (java.net.SocketTimeoutException ste) {
            // Server not responding in time
            String errorMsg = "[ApiClient] ❌ Server timeout at " + BASE_URL +
                            " (no response). Server might be overloaded or unresponsive.";
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (java.net.UnknownHostException uhe) {
            // DNS resolution failed
            String errorMsg = "[ApiClient] ❌ Cannot resolve hostname: " + BASE_URL;
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (Exception e) {
            String errorMsg = "[ApiClient] ❌ Error POST " + path + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        }
    }

    /**
     * Gửi HTTP GET request
     *
     * @param path   đường dẫn endpoint
     * @param token  token xác thực (null nếu không cần)
     * @return response body dạng String, hoặc null nếu lỗi
     */
    private static String guiGet(String path, String token) {
        try {
            URL url = new URL(BASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set connection timeout to detect unreachable server sooner
            conn.setConnectTimeout(5000);  // 5 second timeout
            conn.setReadTimeout(5000);

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            int statusCode = conn.getResponseCode();
            InputStream is = (statusCode >= 200 && statusCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (is == null) return null;

            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();
            return response;

        } catch (java.net.ConnectException ce) {
            // Connection refused - server not responding
            String errorMsg = "[ApiClient] ❌ Cannot connect to server at " + BASE_URL +
                            " (Connection refused). Make sure the server is running on port 8080.";
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (java.net.SocketTimeoutException ste) {
            // Server not responding in time
            String errorMsg = "[ApiClient] ❌ Server timeout at " + BASE_URL +
                            " (no response). Server might be overloaded or unresponsive.";
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (java.net.UnknownHostException uhe) {
            // DNS resolution failed
            String errorMsg = "[ApiClient] ❌ Cannot resolve hostname: " + BASE_URL;
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        } catch (Exception e) {
            String errorMsg = "[ApiClient] ❌ Error GET " + path + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error(errorMsg);
            System.err.println(errorMsg);
            return null;
        }
    }
}
