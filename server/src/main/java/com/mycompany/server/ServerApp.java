package com.mycompany.server;

import com.mycompany.action.AuctionScheduler;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.exception.AuctionRoom.InvalidBidException;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.server.controller.AuctionController;
import com.mycompany.server.controller.BidController;
import com.mycompany.server.controller.TransactionController;
import com.mycompany.server.controller.UserController;
import com.mycompany.utils.AuctionRepositorySQLite;
import com.mycompany.utils.DatabaseConnection;
import com.mycompany.utils.UserRepositorySQLite;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * ServerApp — Điểm khởi động HTTP server của ứng dụng đấu giá.
 * <p>
 * Chạy trên cổng 8080 với các endpoint:
 * POST /api/users/login      → Đăng nhập, trả về token (với session validation)
 * POST /api/users/register   → Đăng ký tài khoản mới
 * POST /api/users/logout     → Đăng xuất, xóa session khỏi online users
 * GET  /api/users/{email}    → Lấy thông tin user theo email
 * OPTIONS (mọi đường dẫn)    → Xử lý CORS preflight request từ browser
 * <p>
 * MULTI-DEVICE SESSION VALIDATION:
 * - OnlineUsersManager tracks all logged-in users
 * - Prevents multiple active sessions for same user (1 per email)
 * - Auto-cleanup of disconnected sessions after 30 minutes
 * <p>
 * Cách chạy:
 * Chạy main() của class này → server lắng nghe tại http://localhost:8080
 * <p>
 * Test bằng curl:
 * curl -X POST http://localhost:8080/api/users/login \
 * -H "Content-Type: application/json" \
 * -d "{\"email\":\"abc@gmail.com\",\"matKhau\":\"123456\"}"
 */
public class ServerApp {
  private static final String SIGN_IN = "/api/users/login";
  private static final String SIGN_UP = "/api/users/register";
  private static final String SIGN_OUT = "/api/users/logout";

  /**
   * Cổng server lắng nghe
   */
  private static final int PORT = 8080;
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ServerApp.class);

  public static void main(String[] args) throws IOException {

    // ===== KHỞI TẠO DATABASE =====
    DatabaseConnection.initialize();
    AuctionRepositorySQLite auctionRepo = new AuctionRepositorySQLite();

    try {
      Map<String, AuctionSession> allSessions = auctionRepo.findAll();
      AuctionScheduler scheduler = AuctionScheduler.getInstance();
      AuctionSessionService sessionService = AuctionSessionService.getInstance();
      AuctionSessionRegistry registry = AuctionSessionRegistry.getInstance();
      LocalDateTime now = LocalDateTime.now();

      int countOpened = 0;    // WAITING da den gio -> mo ngay
      int countScheduled = 0; // WAITING chua den gio -> len lich
      int countClosed = 0;    // IN_PROGRESS qua gio dong -> dong ngay
      int countResumed = 0;   // IN_PROGRESS con hieu luc -> khoi phuc lich dong

      for (AuctionSession session : allSessions.values()) {
        SessionStatus status = session.getStatus();

        if (status == SessionStatus.PENDING) {
          registry.add(session);
          scheduler.setASAuction(session);
          logger.info("Khoi phuc phien {} dang cho admin duyet", session.getSessionId());

        } else if (status == SessionStatus.WAITING) {
          if (session.getStartTime() == null) continue;

          if (session.isAccepted() == 0) {
            scheduler.cancelAS(session);
            session.setStatus(SessionStatus.CANCELLED);
            auctionRepo.update(session);
            logger.info("Phien {} da bi admin tu choi truoc do", session.getSessionId());
            continue;
          }

          if (!session.getStartTime().isAfter(now)) {
            // Da den hoac qua gio mo -> mo ngay (delay=0)
            registry.add(session);
              scheduler.setASAuction(session);
            countOpened++;
            logger.info("Mo ngay phien {} (startTime={} da qua)", session.getSessionId(), session.getStartTime());
          } else {
            // Chua den gio mo -> len lich binh thuong
            AuctionSessionRegistry.getInstance().add(session);
            scheduler.setASAuction(session);
            countScheduled++;
            logger.info("Len lich mo phien {} luc {}", session.getSessionId(), session.getStartTime());
          }

        } else if (status == SessionStatus.IN_PROGRESS) {
          if (session.getEndTime() != null && !session.getEndTime().isAfter(now)) {
            // Da qua gio dong -> dong ngay (delay=0)
            AuctionSessionRegistry.getInstance().add(session);
            scheduler.setACAuction(session);
            countClosed++;
            logger.info("Dong ngay phien {} (endTime={} da qua)", session.getSessionId(), session.getEndTime());
          } else {
            // Con dang dien ra -> nap lai va len lich dong dung gio
            AuctionSessionRegistry.getInstance().add(session);
            scheduler.setACAuction(session);
            countResumed++;
            logger.info("Khoi phuc phien {} (dong luc {})", session.getSessionId(), session.getEndTime());
          }
        }
        // PAID / CANCELLED -> bo qua
      }

      logger.info("========================================");
      logger.info("  KHOI PHUC PHIEN DAU GIA:");
      logger.info("  Mo ngay (da den gio):       {}", countOpened);
      logger.info("  Len lich mo (chua den gio): {}", countScheduled);
      logger.info("  Dong ngay (qua gio dong):   {}", countClosed);
      logger.info("  Khoi phuc dang chay:        {}", countResumed);
      logger.info("========================================");

    } catch (Exception e) {
      logger.error("Failed to preload sessions: " + e.getMessage());
    }
    UserRepositorySQLite userStorage = new UserRepositorySQLite();
    userStorage.migrateLegacyPasswords();

    // ===== KHỞI TẠO SERVER =====
    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

    // ===== KHỞI TẠO CONTROLLER =====
    UserController userController = new UserController();
    AuctionController auctionController = new AuctionController();
    BidController bidController = new BidController();
    TransactionController transactionController = new TransactionController();

    // ===== ĐĂNG KÝ ENDPOINTS: USERS =====

    /**
     * POST /api/users/login
     * Đăng nhập → trả về token nếu đúng email/mật khẩu
     * xem giai thich trong SERVER_GIAITHICH_HOANTOAN.md
     */
    server.createContext(SIGN_IN, exchange -> {
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
    server.createContext(SIGN_UP, exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleRegister(exchange);
    });

    /**
     * POST /api/users/logout
     * Đăng xuất → xóa session khỏi online users list
     * NEW: Multi-device session handling
     */
    server.createContext(SIGN_OUT, exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleLogout(exchange);
    });

    /**
     * PUT /api/users/profile  →  cập nhật thông tin cá nhân (name, phone, address, ...)
     */
    server.createContext("/api/users/profile", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleUpdateProfile(exchange);
    });

    /**
     * PUT /api/users/balance  →  cập nhật số dư (nạp / rút tiền)
     */
    server.createContext("/api/users/balance", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleUpdateBalance(exchange);
    });

    /**
     * POST /api/users/deposit  →  nạp tiền (delta-based, thread-safe)
     */
    server.createContext("/api/users/deposit", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleDeposit(exchange);
    });

    /**
     * POST /api/users/withdraw  →  rút tiền (delta-based, thread-safe)
     */
    server.createContext("/api/users/withdraw", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleWithdraw(exchange);
    });

    /**
     * POST /api/users/change-password  →  đổi mật khẩu
     */
    server.createContext("/api/users/change-password", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleChangePassword(exchange);
    });

    server.createContext("/api/users/ban", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleBanUser(exchange);
    });

    server.createContext("/api/users/unban", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      userController.handleUnbanUser(exchange);
    });

    /**
     * GET /api/users/{email}
     * Lấy thông tin người dùng theo email
     * Ví dụ: GET /api/users/abc@gmail.com
     */
    server.createContext("/api/users", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      // Bỏ qua nếu là /api/users/login hoặc /api/users/register
      // (đã được xử lý bởi context riêng ở trên)
      String path = exchange.getRequestURI().getPath();
      if (path.equals(SIGN_IN) || path.equals(SIGN_UP)) {
        // Java HttpServer ưu tiên context cụ thể hơn, nhưng thêm check để an toàn
        exchange.sendResponseHeaders(404, -1);
        return;
      }
      userController.handleGetUser(exchange);
    });

    server.createContext("/api/users/all-users", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }

      userController.handleFindAllUsers(exchange);
    });

    /**
     * GET /api/users/bankacc/{so tai khoan}
     * Trả về true nếu chưa có liên kết, false nếu có rồi
     */
    server.createContext("/api/users/bankacc", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }

      userController.handleCheckBankAcc(exchange);
    });

    server.createContext("/api/users/number", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }

      userController.handleGetUserCount(exchange);
    });

    server.createContext("/api/users/online-number", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }

      userController.handleGetOnlineUserCount(exchange);
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
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
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
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      try {
        bidController.route(exchange);
      } catch (InvalidBidException e) {
        throw new RuntimeException(e);
      }
    });

    server.createContext("/api/transactions", exchange -> {
      if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
      }
      transactionController.route(exchange);
    });

      server.createContext("/api/transactions/number", exchange -> {
          if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
              xuLyCors(exchange);
              return;
          }
          transactionController.handleCountTransactions(exchange);
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

    com.mycompany.server.websocket.AuctionWebSocketServer wsServerInstance =
        com.mycompany.websocket.AuctionWebSocketServerStarter.getServer();
    if (wsServerInstance != null) {
      AuctionSessionService.getInstance().setWebSocketServer(wsServerInstance);
      logger.info("✅ WebSocket server đã được inject vào AuctionSessionService");
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
    logger.info("  POST http://localhost:" + PORT + "/api/users/logout");
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
   * <p>
   * Browser tự động gửi OPTIONS trước khi gửi POST/GET thật sự
   * khi domain của client khác domain của server (cross-origin).
   * Server phải trả 200 + CORS headers thì browser mới tiếp tục gửi request thật.
   *
   * @param exchange đối tượng HttpExchange của OPTIONS request
   */
  private static void xuLyCors(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
    /// cho phép tất cả các nguồn truy cập vào server này
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    /// cho phép các phương thức HTTP cụ thể
    /// GET lấy dữ liệu
    /// POST gửi hoặc tạo mới dữ liệu
    /// OPTIONS phương thức ướm hỏi, Trình duyệt tự động gửi nó để hỏi xem Server có cho phép thực hiện yêu cầu hay không
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    /// cho phép các lại thông tin đi kèm
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    /// gửi mã phản hồi 200 (Thành công) nhưng không có nội dung (-1)
    exchange.sendResponseHeaders(200, -1); // -1 = không có body
    exchange.close();
  }
}
