package com.mycompany.server;

import com.mycompany.server.controller.UserController;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * ServerApp — Điểm khởi động HTTP server của ứng dụng đấu giá.
 *
 * Chạy trên cổng 8080 với các endpoint:
 *  POST /api/users/login      → Đăng nhập, trả về token
 *  POST /api/users/register   → Đăng ký tài khoản mới
 *  GET  /api/users/{email}    → Lấy thông tin user theo email
 *  OPTIONS (mọi đường dẫn)    → Xử lý CORS preflight request từ browser
 *
 * Cách chạy:
 *   Chạy main() của class này → server lắng nghe tại http://localhost:8080
 *
 * Test bằng curl:
 *   curl -X POST http://localhost:8080/api/users/login \
 *        -H "Content-Type: application/json" \
 *        -d "{\"email\":\"abc@gmail.com\",\"matKhau\":\"123456\"}"
 */
public class ServerApp {

    /** Cổng server lắng nghe */
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {

        // ===== KHỞI TẠO SERVER =====

        // Tạo HttpServer lắng nghe tại 0.0.0.0:8080
        // InetSocketAddress(PORT) = lắng nghe trên tất cả network interface
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ===== KHỞI TẠO CONTROLLER =====
        UserController userController = new UserController();

        // ===== ĐĂNG KÝ CÁC ENDPOINT =====

        /**
         * POST /api/users/login
         * Đăng nhập → trả về token nếu đúng email/mật khẩu
         */
        server.createContext("/api/users/login", exchange -> {
            // Xử lý CORS preflight (browser gửi OPTIONS trước khi POST)
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                xuLyCors(exchange);
                return;
            }
            userController.handleLogin(exchange);
        });

        /**
         * POST /api/users/register
         * Đăng ký tài khoản mới → lưu vào dulieunguoidung.json
         */
        server.createContext("/api/users/register", exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                xuLyCors(exchange);
                return;
            }
            userController.handleRegister(exchange);
        });

        /**
         * GET /api/users/{email}
         * Lấy thông tin người dùng theo email
         * Ví dụ: GET /api/users/abc@gmail.com
         */
        server.createContext("/api/users/", exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                xuLyCors(exchange);
                return;
            }
            // Bỏ qua nếu là /api/users/login hoặc /api/users/register
            // (đã được xử lý bởi context riêng ở trên)
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/users/login") || path.equals("/api/users/register")) {
                // Java HttpServer ưu tiên context cụ thể hơn, nhưng thêm check để an toàn
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            userController.handleGetUser(exchange);
        });

        // ===== CẤU HÌNH THREAD POOL =====
        // Dùng thread pool cố định 10 luồng để xử lý nhiều request đồng thời
        server.setExecutor(Executors.newFixedThreadPool(10));

        // ===== KHỞI ĐỘNG SERVER =====
        server.start();

        System.out.println("========================================");
        System.out.println("  SERVER ĐÃ KHỞI ĐỘNG THÀNH CÔNG!");
        System.out.println("  Địa chỉ: http://localhost:" + PORT);
        System.out.println("========================================");
        System.out.println("  Các endpoint sẵn sàng:");
        System.out.println("  POST http://localhost:" + PORT + "/api/users/login");
        System.out.println("  POST http://localhost:" + PORT + "/api/users/register");
        System.out.println("  GET  http://localhost:" + PORT + "/api/users/{email}");
        System.out.println("========================================");
        System.out.println("  Nhấn Ctrl+C để dừng server");
    }

    /**
     * Xử lý CORS preflight request (OPTIONS).
     *
     * Browser tự động gửi OPTIONS trước khi gửi POST/GET thật sự
     * khi domain của client khác domain của server (cross-origin).
     * Server phải trả 200 + CORS headers thì browser mới tiếp tục gửi request thật.
     *
     * @param exchange đối tượng HttpExchange của OPTIONS request
     */
    private static void xuLyCors(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(200, -1); // -1 = không có body
        exchange.close();
    }
}