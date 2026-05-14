package com.mycompany.server;

import com.mycompany.server.controller.AuctionController;
import com.mycompany.server.controller.BidController;
import com.mycompany.server.controller.UserController;
import com.mycompany.utils.KetNoiCSDL;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

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
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ServerApp.class);
    public static void main(String[] args) throws IOException {

        // ===== KHỞI TẠO DATABASE =====
        KetNoiCSDL.khoiTao();
        KhoLuuTruNguoiDungSQLite userStorage = new KhoLuuTruNguoiDungSQLite();
        userStorage.migratePlainTextPasswords();

        // ===== KHỞI TẠO SERVER =====
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ===== KHỞI TẠO CONTROLLER =====
        UserController userController = new UserController();
        AuctionController auctionController = new AuctionController();
        BidController     bidController     = new BidController();

        // ===== ĐĂNG KÝ ENDPOINTS: USERS =====

        /**
         * POST /api/users/login
         * Đăng nhập → trả về token nếu đúng email/mật khẩu
         * xem giai thich trong SERVER_GIAITHICH_HOANTOAN.md
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

        // ===== ĐĂNG KÝ ENDPOINTS: AUCTIONS =====

        /**
         * Context /api/auctions xử lý:
         *   GET  /api/auctions          → danh sách
         *   GET  /api/auctions/{id}     → chi tiết
         *   POST /api/auctions          → tạo mới
         *   PUT  /api/auctions/{id}/start → bắt đầu
         *
         * AuctionController.route() tự phân luồng theo method + path.
         */
        server.createContext("/api/auctions", exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { xuLyCors(exchange); return; }
            auctionController.route(exchange);
        });

        // ===== ĐĂNG KÝ ENDPOINTS: BIDS =====

        /**
         * Context /api/bids xử lý:
         *   POST /api/bids              → đặt giá
         *   GET  /api/bids/{phienId}    → lịch sử đặt giá
         *
         * BidController.java.route() tự phân luồng theo method + path.
         */
        server.createContext("/api/bids", exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { xuLyCors(exchange); return; }
            bidController.route(exchange);
        });

        // ===== CẤU HÌNH THREAD POOL =====
        server.setExecutor(Executors.newFixedThreadPool(10));

        // ===== KHỞI ĐỘNG SERVER =====
        server.start();

        // ===== KHỞI ĐỘNG WEBSOCKET SERVER WITH VERIFICATION =====
        com.mycompany.websocket.AuctionWebSocketServerStarter.startServer();

        // Wait a moment for WebSocket startup (async startup on separate thread)
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check WebSocket startup status and warn if failed
        String wsError = com.mycompany.websocket.AuctionWebSocketServerStarter.getStartupError();
        boolean wsSuccess = com.mycompany.websocket.AuctionWebSocketServerStarter.isStartupSuccessful();

        logger.info("========================================");
        logger.info("  🚨 SERVER STARTUP REPORT");
        logger.info("========================================");
        logger.info("  HTTP  http://localhost:" + PORT + " (REST API)");

        if (wsSuccess) {
            logger.info("  WS    ws://localhost:8081 (WebSocket real-time)");
            logger.info("========================================");
            logger.info("  ✅ SERVER ĐÃ KHỞI ĐỘNG THÀNH CÔNG!");
        } else {
            logger.error("  ❌ WEBSOCKET SERVER FAILED TO START!");
            if (wsError != null) {
                logger.error("  Error: " + wsError);
            }
            logger.error("========================================");
            logger.error("  ⚠️  HTTP server is running but WebSocket is NOT available");
            logger.error("  ⚠️  Real-time auction features will NOT work!");
            logger.error("========================================");
        }

        logger.info("  Địa chỉ: http://localhost:" + PORT);
        logger.info("========================================");
        logger.info("  USERS:");
        logger.info("  POST http://localhost:" + PORT + "/api/users/login");
        logger.info("  POST http://localhost:" + PORT + "/api/users/register");
        logger.info("  GET  http://localhost:" + PORT + "/api/users/{email}");
        logger.info("----------------------------------------");
        logger.info("  AUCTIONS:");
        logger.info("  GET  http://localhost:" + PORT + "/api/auctions");
        logger.info("  GET  http://localhost:" + PORT + "/api/auctions/{id}");
        logger.info("  POST http://localhost:" + PORT + "/api/auctions           [cần token]");
        logger.info("  PUT  http://localhost:" + PORT + "/api/auctions/{id}/start [cần token]");
        logger.info("----------------------------------------");
        logger.info("  BIDS:");
        logger.info("  POST http://localhost:" + PORT + "/api/bids               [cần token]");
        logger.info("  GET  http://localhost:" + PORT + "/api/bids/{phienId}");
        logger.info("========================================");
        logger.info("  Nhấn Ctrl+C để dừng server");
        logger.info("========================================");
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
        /// cho phép tất cả các nguồn truy cập vào server này
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        /// cho phép các phương thức HTTP cụ thể
        /// GET lấy dữ liệu
        /// POST gửi hoặc tạo mới dữ liệu
        /// OPTIONS phương thức ướm hỏi, Trình duyệt tự động gửi nó để hỏi xem Server có cho phép thực hiện yêu cầu hay không
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        /// cho phép các lại thông tin đi kèm
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        /// gửi mã phản hồi 200 (Thành công) nhưng không có nội dung (-1)
        exchange.sendResponseHeaders(200, -1); // -1 = không có body
        exchange.close();
    }
}