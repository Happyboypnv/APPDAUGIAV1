package com.mycompany.utils;

import com.google.gson.Gson;
import com.mycompany.server.dto.LoginRequest;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.server.dto.RegisterRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    // Địa chỉ server — đổi IP này nếu server chạy trên máy khác
    private static final String BASE_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();

    // ============================================================
    // ĐĂNG NHẬP
    // ============================================================

    /**
     * Gọi POST /api/users/login
     *
     * @param email    email người dùng
     * @param matKhau  mật khẩu plain text
     * @return LoginResponse chứa token nếu thành công, thongBao nếu thất bại
     */
    public static LoginResponse login(String email, String matKhau) {
        // Bước 1: Tạo object request rồi chuyển thành JSON
        LoginRequest req = new LoginRequest(email, matKhau);
        String jsonBody = gson.toJson(req);

        // Bước 2: Gửi HTTP POST và nhận response
        String responseJson = guiPost("/api/users/login", jsonBody, null);

        // Bước 3: Chuyển JSON response thành object Java
        if (responseJson == null) return new LoginResponse("Không kết nối được server");
        return gson.fromJson(responseJson, LoginResponse.class);
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

    // ============================================================
    // LẤY THÔNG TIN USER
    // ============================================================

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

        } catch (Exception e) {
            System.err.println("[ApiClient] Lỗi POST " + path + ": " + e.getMessage());
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

        } catch (Exception e) {
            System.err.println("[ApiClient] Lỗi GET " + path + ": " + e.getMessage());
            return null;
        }
    }
}
