package com.mycompany.server;

import com.google.gson.Gson;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.utils.KetNoiCSDL;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Integration cho UserController của Người 1.
 *
 * Cách chạy trong IntelliJ:
 *   Chuột phải vào file → Run 'UserControllerTest'
 *
 * Lưu ý: File này tự khởi động server trên port 8081 (tránh xung đột với port 8080 thật).
 * Không cần chạy ServerApp trước.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {

    // Dùng port 8081 để tránh xung đột với server thật đang chạy ở 8080
    private static final int TEST_PORT = 8081;
    private static final String BASE = "http://localhost:" + TEST_PORT;
    private static final Gson gson = new Gson();

    // Email ngẫu nhiên mỗi lần chạy test để tránh trùng database
    private static final String TEST_EMAIL = "test_" + System.currentTimeMillis() + "@gmail.com";
    private static final String TEST_PASSWORD = "Test@1234";
    private static String savedToken;

    // ============================================================
    // SETUP: Khởi động server test trước khi chạy tất cả test
    // ============================================================

    @BeforeAll
    static void khoiDongServer() throws Exception {
        // Bước 1: Khởi tạo database
        KetNoiCSDL.khoiTao();
        new KhoLuuTruNguoiDungSQLite().migratePlainTextPasswords();

        // Bước 2: Chạy server trên port 8081 trong background thread
        // setDaemon(true) = tự tắt khi test xong, không cần dừng thủ công
        Thread serverThread = new Thread(() -> {
            try {
                // Tạo server tạm trên port 8081 thay vì 8080
                com.sun.net.httpserver.HttpServer server =
                        com.sun.net.httpserver.HttpServer.create(
                                new java.net.InetSocketAddress(TEST_PORT), 0
                        );

                com.mycompany.server.controller.UserController userController =
                        new com.mycompany.server.controller.UserController();

                server.createContext("/api/users/login", exchange -> {
                    if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                        xuLyCors(exchange); return;
                    }
                    userController.handleLogin(exchange);
                });

                server.createContext("/api/users/register", exchange -> {
                    if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                        xuLyCors(exchange); return;
                    }
                    userController.handleRegister(exchange);
                });

                server.createContext("/api/users/", exchange -> {
                    if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                        xuLyCors(exchange); return;
                    }
                    String path = exchange.getRequestURI().getPath();
                    if (path.equals("/api/users/login") || path.equals("/api/users/register")) {
                        exchange.sendResponseHeaders(404, -1); return;
                    }
                    userController.handleGetUser(exchange);
                });

                server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(5));
                server.start();
                System.out.println("✅ Test server khởi động tại port " + TEST_PORT);

            } catch (Exception e) {
                System.err.println("❌ Lỗi khởi động test server: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Chờ 1 giây để server khởi động xong
        Thread.sleep(1000);
    }

    private static void xuLyCors(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    // ============================================================
    // TEST ĐĂNG KÝ
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("✅ Đăng ký thành công với thông tin hợp lệ")
    void test_DangKy_ThanhCong() throws Exception {
        String body = String.format(
                "{\"hoTen\":\"Nguyen Van Test\",\"email\":\"%s\",\"matKhau\":\"%s\",\"ngaySinh\":\"2000-01-01\"}",
                TEST_EMAIL, TEST_PASSWORD
        );

        KetQuaHttp ket = guiPost("/api/users/register", body, null);

        // Phải trả 201 Created
        assertEquals(201, ket.statusCode,
                "Đăng ký thành công phải trả về 201. Response: " + ket.body);

        LoginResponse res = gson.fromJson(ket.body, LoginResponse.class);
        assertEquals("Đăng ký thành công", res.getThongBao());

        System.out.println("✅ PASS: Đăng ký thành công → " + ket.body);
    }

    @Test
    @Order(2)
    @DisplayName("✅ Đăng ký thất bại khi email đã tồn tại")
    void test_DangKy_EmailTrung() throws Exception {
        // Dùng lại email đã đăng ký ở test 1
        String body = String.format(
                "{\"hoTen\":\"Ten Khac\",\"email\":\"%s\",\"matKhau\":\"%s\",\"ngaySinh\":\"2000-01-01\"}",
                TEST_EMAIL, TEST_PASSWORD
        );

        KetQuaHttp ket = guiPost("/api/users/register", body, null);

        assertEquals(400, ket.statusCode,
                "Email trùng phải trả về 400. Response: " + ket.body);

        LoginResponse res = gson.fromJson(ket.body, LoginResponse.class);
        assertTrue(res.getThongBao().contains("tồn tại"),
                "Thông báo phải nhắc email đã tồn tại");

        System.out.println("✅ PASS: Email trùng → " + ket.body);
    }

    @Test
    @Order(3)
    @DisplayName("✅ Đăng ký thất bại khi thiếu trường ngaySinh")
    void test_DangKy_ThieuNgaySinh() throws Exception {
        String body = String.format(
                "{\"hoTen\":\"Ai Do\",\"email\":\"aido_%d@gmail.com\",\"matKhau\":\"%s\"}",
                System.currentTimeMillis(), TEST_PASSWORD
        );

        KetQuaHttp ket = guiPost("/api/users/register", body, null);

        assertEquals(400, ket.statusCode,
                "Thiếu ngaySinh phải trả về 400. Response: " + ket.body);

        System.out.println("✅ PASS: Thiếu ngaySinh → " + ket.body);
    }

    @Test
    @Order(4)
    @DisplayName("✅ Đăng ký thất bại khi body rỗng {}")
    void test_DangKy_BodyRong() throws Exception {
        KetQuaHttp ket = guiPost("/api/users/register", "{}", null);

        assertEquals(400, ket.statusCode,
                "Body rỗng phải trả về 400. Response: " + ket.body);

        System.out.println("✅ PASS: Body rỗng → " + ket.body);
    }

    @Test
    @Order(5)
    @DisplayName("✅ Đăng ký từ chối method GET")
    void test_DangKy_SaiMethod() throws Exception {
        KetQuaHttp ket = guiGet("/api/users/register", null);

        // GET thay vì POST phải bị từ chối
        // Server trả 404 vì /api/users/register được catch bởi /api/users/ context
        // hoặc 405 nếu vào được handleRegister
        assertTrue(ket.statusCode == 404 || ket.statusCode == 405,
                "Sai method phải trả về 404 hoặc 405. Response: " + ket.body);

        System.out.println("✅ PASS: Sai method đăng ký → HTTP " + ket.statusCode);
    }

    // ============================================================
    // TEST ĐĂNG NHẬP
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("✅ Đăng nhập thành công với đúng email và mật khẩu")
    void test_DangNhap_ThanhCong() throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"matKhau\":\"%s\"}",
                TEST_EMAIL, TEST_PASSWORD
        );

        KetQuaHttp ket = guiPost("/api/users/login", body, null);

        assertEquals(200, ket.statusCode,
                "Đăng nhập thành công phải trả về 200. Response: " + ket.body);

        LoginResponse res = gson.fromJson(ket.body, LoginResponse.class);

        // Token phải tồn tại và đúng định dạng
        assertNotNull(res.getToken(),
                "Phải có token trong response");
        assertTrue(res.getToken().startsWith("USER_"),
                "Token phải bắt đầu bằng USER_");

        // Email trong response phải khớp
        assertEquals(TEST_EMAIL, res.getEmail(),
                "Email trong response phải khớp");

        // Lưu token lại cho test GET
        savedToken = res.getToken();

        System.out.println("✅ PASS: Đăng nhập thành công, token: " + savedToken);
    }

    @Test
    @Order(7)
    @DisplayName("✅ Đăng nhập thất bại khi sai mật khẩu")
    void test_DangNhap_SaiMatKhau() throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"matKhau\":\"SaiMatKhau@999\"}",
                TEST_EMAIL
        );

        KetQuaHttp ket = guiPost("/api/users/login", body, null);

        assertEquals(401, ket.statusCode,
                "Sai mật khẩu phải trả về 401. Response: " + ket.body);

        LoginResponse res = gson.fromJson(ket.body, LoginResponse.class);
        assertNull(res.getToken(),
                "Đăng nhập thất bại KHÔNG được trả token");

        System.out.println("✅ PASS: Sai mật khẩu → " + ket.body);
    }

    @Test
    @Order(8)
    @DisplayName("✅ Đăng nhập thất bại khi email không tồn tại")
    void test_DangNhap_EmailKhongTonTai() throws Exception {
        String body = "{\"email\":\"khongtontai_xyz@gmail.com\",\"matKhau\":\"Test@1234\"}";

        KetQuaHttp ket = guiPost("/api/users/login", body, null);

        assertEquals(401, ket.statusCode,
                "Email không tồn tại phải trả về 401. Response: " + ket.body);

        System.out.println("✅ PASS: Email không tồn tại → " + ket.body);
    }

    @Test
    @Order(9)
    @DisplayName("✅ Đăng nhập thất bại khi thiếu email")
    void test_DangNhap_ThieuEmail() throws Exception {
        String body = "{\"matKhau\":\"Test@1234\"}";

        KetQuaHttp ket = guiPost("/api/users/login", body, null);

        assertEquals(400, ket.statusCode,
                "Thiếu email phải trả về 400. Response: " + ket.body);

        System.out.println("✅ PASS: Thiếu email → " + ket.body);
    }

    @Test
    @Order(10)
    @DisplayName("✅ Đăng nhập thất bại khi thiếu mật khẩu")
    void test_DangNhap_ThieuMatKhau() throws Exception {
        String body = String.format("{\"email\":\"%s\"}", TEST_EMAIL);

        KetQuaHttp ket = guiPost("/api/users/login", body, null);

        assertEquals(400, ket.statusCode,
                "Thiếu matKhau phải trả về 400. Response: " + ket.body);

        System.out.println("✅ PASS: Thiếu mật khẩu → " + ket.body);
    }

    // ============================================================
    // TEST LẤY THÔNG TIN USER
    // ============================================================

    @Test
    @Order(11)
    @DisplayName("✅ Lấy thông tin user thành công khi có token")
    void test_GetUser_ThanhCong() throws Exception {
        // Đảm bảo có token (đăng nhập lại nếu chưa có)
        if (savedToken == null) {
            String loginBody = String.format(
                    "{\"email\":\"%s\",\"matKhau\":\"%s\"}", TEST_EMAIL, TEST_PASSWORD
            );
            KetQuaHttp lr = guiPost("/api/users/login", loginBody, null);
            savedToken = gson.fromJson(lr.body, LoginResponse.class).getToken();
        }

        KetQuaHttp ket = guiGet("/api/users/" + TEST_EMAIL, savedToken);

        assertEquals(200, ket.statusCode,
                "Lấy user hợp lệ phải trả về 200. Response: " + ket.body);

        // Response phải chứa email
        assertTrue(ket.body.contains(TEST_EMAIL),
                "Response phải chứa email của user");

        // QUAN TRỌNG: Response KHÔNG được chứa matKhau hay salt
        assertFalse(ket.body.contains("matKhau"),
                "Response KHÔNG được lộ matKhau — bảo mật bị vi phạm!");
        assertFalse(ket.body.contains("\"salt\""),
                "Response KHÔNG được lộ salt — bảo mật bị vi phạm!");

        System.out.println("✅ PASS: Lấy user thành công, không lộ password");
    }

    @Test
    @Order(12)
    @DisplayName("✅ Lấy thông tin user thất bại khi không có token")
    void test_GetUser_KhongCoToken() throws Exception {
        KetQuaHttp ket = guiGet("/api/users/" + TEST_EMAIL, null);

        assertEquals(401, ket.statusCode,
                "Không có token phải trả về 401. Response: " + ket.body);

        System.out.println("✅ PASS: Không có token → " + ket.body);
    }

    @Test
    @Order(13)
    @DisplayName("✅ Lấy thông tin user thất bại khi token sai định dạng")
    void test_GetUser_TokenSaiDinhDang() throws Exception {
        // Token không có "Bearer " prefix
        KetQuaHttp ket = guiGet("/api/users/" + TEST_EMAIL, "INVALID_TOKEN_FORMAT");

        // Gọi guiGet với token thô — không có "Bearer " prefix
        // Cần kiểm tra thủ công vì helper tự thêm Bearer
        URL url = new URL(BASE + "/api/users/" + TEST_EMAIL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "INVALID_FORMAT"); // Không có "Bearer "

        int status = conn.getResponseCode();
        assertEquals(401, status,
                "Token sai định dạng phải trả về 401");

        System.out.println("✅ PASS: Token sai định dạng → HTTP 401");
    }

    @Test
    @Order(14)
    @DisplayName("✅ Lấy thông tin user không tồn tại trả về 404")
    void test_GetUser_KhongTonTai() throws Exception {
        if (savedToken == null) savedToken = "USER_fake_000";

        KetQuaHttp ket = guiGet("/api/users/khongtontai_xyz@gmail.com", savedToken);

        assertEquals(404, ket.statusCode,
                "User không tồn tại phải trả về 404. Response: " + ket.body);

        System.out.println("✅ PASS: User không tồn tại → " + ket.body);
    }

    @Test
    @Order(15)
    @DisplayName("✅ Gọi sai method POST cho endpoint GET user")
    void test_GetUser_SaiMethod() throws Exception {
        if (savedToken == null) savedToken = "USER_fake_000";

        // Gửi POST đến endpoint chỉ nhận GET
        KetQuaHttp ket = guiPost("/api/users/" + TEST_EMAIL,
                "{}", savedToken);

        assertEquals(405, ket.statusCode,
                "Sai method phải trả về 405. Response: " + ket.body);

        System.out.println("✅ PASS: Sai method GET user → " + ket.body);
    }

    // ============================================================
    // PHƯƠNG THỨC HỖ TRỢ
    // ============================================================

    static class KetQuaHttp {
        int statusCode;
        String body;
        KetQuaHttp(int s, String b) { statusCode = s; body = b; }
    }

    private KetQuaHttp guiPost(String path, String jsonBody, String token) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream() : conn.getErrorStream();

        String body = is != null
                ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";

        conn.disconnect();
        return new KetQuaHttp(status, body);
    }

    private KetQuaHttp guiGet(String path, String token) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream() : conn.getErrorStream();

        String body = is != null
                ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";

        conn.disconnect();
        return new KetQuaHttp(status, body);
    }
}