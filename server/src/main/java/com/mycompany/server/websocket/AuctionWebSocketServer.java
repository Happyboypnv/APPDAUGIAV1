package com.mycompany.server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.utils.UserRepositorySQLite;
import com.mycompany.utils.AuctionRepositorySQLite;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuctionWebSocketServer - WebSocket Server cho Real-Time Auction Bidding
 *
 * MỤC ĐÍCH:
 * - Quản lý kết nối real-time của các client đang tham gia đấu giá
 * - Xử lý các sự kiện: JOIN, BID, DISCONNECT
 * - Broadcast kết quả đấu giá cho tất cả client
 * - Đảm bảo thread-safety trong môi trường đa luồng
 *
 * TÍNH NĂNG CHÍNH:
 * - Quản lý phòng đấu giá (mỗi phiên = 1 phòng)
 * - Nhận BID từ client → gọi service → broadcast kết quả
 * - Client tracking: ConcurrentHashMap đảm bảo thread-safe
 * - Graceful disconnect handling
 *
 * PORT: 8081 (tránh conflict với HTTP server port 8080)
 *
 * THREAD SAFETY:
 * - Sử dụng ConcurrentHashMap thay vì HashMap
 * - Sử dụng synchronized block khi cần multiple operations
 * - Mỗi broadcast operation là atomic
 * - PhienDauGiaService.setPrice() phải thread-safe
 */
public class AuctionWebSocketServer extends WebSocketServer {
    private final UserRepositorySQLite userRepositorySQLite = new UserRepositorySQLite();
    private final AuctionRepositorySQLite auctionRepositorySQLite = new AuctionRepositorySQLite();
    private final static Logger logger = LoggerFactory.getLogger(AuctionWebSocketServer.class);
    // ===== THREAD-SAFE DATA STRUCTURES =====

    // rooms: Map<phienId, Set<WebSocket clients>>
    // ConcurrentHashMap: multiple threads có thể access cùng lúc an toàn
    private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();

    private final Map<String, WebSocket> emailToConn = new ConcurrentHashMap<>();

    // clientRooms: Map<WebSocket client, phienId>
    // Để track client đang ở phòng nào (cần khi disconnect)
    private final Map<WebSocket, String> clientRooms = new ConcurrentHashMap<>();

    // Gson: Cho serialization/deserialization JSON
    // Gson là thread-safe (trong version >= 2.8.9)
    private final Gson gson = new Gson();

    // PhienDauGiaService instance (giả sử là singleton)
    private final AuctionSessionService auctionSessionService = AuctionSessionService.getInstance();

    /**
     * Constructor: Khởi tạo WebSocket Server ở port 8081
     *
     * @param port Port để lắng nghe (8081)
     */
    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        logger.info("🎯 AuctionWebSocketServer initialized on port " + port);
    }

    /**
     * PHƯƠNG THỨC: onOpen()
     * Gọi khi client connect thành công
     * <p>
     * Mục đích:
     * - Nhận biết kết nối mới, ghi log, và thực hiện các thao tác khởi tạo nhẹ (ví dụ: set timeout)
     * //     * - KHÔNG thêm client vào phòng ở giai đoạn này (chờ message JOIN từ client)
     * - Có thể gửi một message chào mừng / ACK để client biết kết nối đã thành công
     * <p>
     * Lưu ý thread-safety:
     * - Không thực hiện các thao tác nặng hoặc block trong onOpen()
     * - Việc cập nhật `clientRooms` chỉ nên thực hiện khi nhận được JOIN
     *
     * @param conn      WebSocket connection object (connection vừa mở)
     * @param handshake Client handshake request
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String addr = "unknown";
        try {
            if (conn != null && conn.getRemoteSocketAddress() != null) {
                addr = conn.getRemoteSocketAddress().toString();
            }
        } catch (Exception e) {
            // ignore - best-effort logging
        }

        logger.info("✅ Client connected: " + addr);

        // Set a reasonable connection lost timeout so dead connections are cleaned up
        try {
            setConnectionLostTimeout(60); // seconds
        } catch (Throwable t) {
            // oldest versions or implementations may not allow this; ignore safely
        }

        // Optional: send a lightweight welcome/ack message to the client
        try {
            JsonObject welcome = new JsonObject();
            welcome.addProperty("event", "CONNECTED");
            welcome.addProperty("message", "Welcome to Auction WebSocket Server");
            welcome.addProperty("timestamp", System.currentTimeMillis());
            conn.send(gson.toJson(welcome));
        } catch (Exception e) {
            // If sending fails, log and continue; do not close the connection here
            logger.error("⚠️ Failed to send welcome message to " + addr + ": " + e.getMessage());
        }
    }

    /**
     * PHƯƠNG THỨC: onMessage()
     * Gọi khi receive message từ client
     * <p>
     * MESSAGE FORMAT (JSON):
     * {
     * "action": "JOIN"|"BID",
     * "phienId": "string",
     * "userId": "string",
     * "giaRa": number  // chỉ dùng cho BID
     * }
     * <p>
     * LUỒNG XỬ LÝ:
     * 1. Parse JSON
     * 2. Check action type
     * 3. JOIN: thêm client vào phòng
     * 4. BID: gọi service, broadcast kết quả
     * <p>
     * Thread-safety:
     * - Parse message từ từng client (mỗi client có thread riêng)
     * - Synchronized when updating shared rooms map
     * - All broadcasts are atomic operations
     *
     * @param conn    WebSocket connection
     * @param message Message từ client (dạng JSON string)
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            // 🔹 BƯỚC 1: Parse JSON từ client
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.get("action").getAsString();

            // 🔹 BƯỚC 2: Xử lý theo action type
            switch (action.toUpperCase()) {
                case "JOIN":
                    handleJoin(conn, json);
                    break;
                case "BID":
                    handleBid(conn, json);
                    break;
                case "PAYMENT_DECISION":
                    handlePaymentDecision(conn, json);
                    break;
                default:
                    logger.error("❌ Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("❌ Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC (PRIVATE): handleJoin()
     * Xử lý client JOIN vào phòng đấu giá
     * <p>
     * LOGIC:
     * 1. Lấy phienId từ message
     * 2. Thêm client vào rooms.get(phienId)
     * 3. Nếu phòng chưa tồn tại → tạo mới
     * 4. Ghi nhớ client đang ở phòng nào (clientRooms)
     * 5. Broadcast "USER_JOINED" cho tất cả client trong phòng
     * <p>
     * Thread-safety:
     * - rooms.computeIfAbsent(): atomic operation, tự động tạo phòng nếu chưa có
     * - Set.add() trong synchronized block để tránh race condition
     * - clientRooms.put() là thread-safe
     *
     * @param conn WebSocket connection
     * @param json Message JSON
     */
    private void handleJoin(WebSocket conn, JsonObject json) {
        String phienId = json.get("phienId").getAsString();
        String email = json.get("email").getAsString();

        logger.info("🚪 Email " + email + " joining room: " + phienId);

        // 🔹 STEP 1: Ensure room exists (thread-safe)
        // computeIfAbsent: nếu phòng chưa tồn tại → tạo mới với ConcurrentHashMap.newSetFromMap()
        // Nếu phòng đã tồn tại → không làm gì, trả về Set hiện có
        Set<WebSocket> room = rooms.computeIfAbsent(phienId, k ->
            Collections.newSetFromMap(new ConcurrentHashMap<>())
        );

        // 🔹 STEP 2: Add client vào phòng
        // room.add() là thread-safe vì Set được tạo từ ConcurrentHashMap
        synchronized (room) {  // Extra protection khi modifying set
            room.add(conn);
        }
        clientRooms.put(conn, phienId);

        // 🔹 STEP 3: Broadcast "USER_JOINED" to all clients in room
        JsonObject response = new JsonObject();
        response.addProperty("event", "USER_JOINED");
        response.addProperty("email", email);
        response.addProperty("timestamp", System.currentTimeMillis());
        broadcastToRoom(phienId, gson.toJson(response));
        emailToConn.put(email, conn);

    }

    /**
     * PHƯƠNG THỨC (PRIVATE): handleBid()
     * Xử lý client đặt giá vào đấu giá
     * <p>
     * LOGIC:
     * 1. Lấy phienId, userId, giaRa từ message
     * 2. Gọi PhienDauGiaService.setPrice(phienId, userId, giaRa)
     * - Service sẽ validate và save vào DB
     * - Trả về object kết quả (thành công/lỗi)
     * 3. Broadcast kết quả cho toàn phòng
     * <p>
     * Thread-safety:
     * - PhienDauGiaService.setPrice() phải thread-safe (sử dụng transaction)
     * - Broadcast là atomic operation
     * - Multiple clients có thể bid cùng lúc → service phải handle
     *
     * @param conn WebSocket connection
     * @param json Message JSON
     */
     private void handleBid(WebSocket conn, JsonObject json) {
         try {
             String phienId = json.get("phienId").getAsString();
             String email = json.get("email").getAsString();
             double giaRa = json.get("giaRa").getAsDouble();

             AuctionSession phienHienTai = AuctionSessionRegistry.getInstance().find(phienId);

             // FIX: Nếu không tìm thấy trong Registry (in-memory), thử tìm trong database
             // và load vào Registry để các bid tiếp theo có thể tìm thấy
             if (phienHienTai == null) {
                 AuctionSession phienFromDB = auctionRepositorySQLite.findById(phienId);
                 if (phienFromDB == null) {
                     sendError(conn, "Phiên đấu giá không tồn tại");
                     return;
                 }
                 // Load từ DB thành công → đưa vào Registry để các bid tiếp theo tìm thấy
                 phienHienTai = phienFromDB;
                 AuctionSessionRegistry.getInstance().add(phienHienTai);
                 logger.info("✅ Loaded auction từ DB vào Registry: " + phienId);
             }

            // FIX: Lấy giá SAU khi setPrice thành công, không phải trước
            User bidder = userRepositorySQLite.findByEmail(email);
            if (bidder == null) {
                sendError(conn, "Người dùng không tồn tại");
                return;
            }
            if (phienHienTai.getSeller() != null
                && bidder.getUserId().equals(phienHienTai.getSeller().getUserId())) {
                sendError(conn, "Người tạo phiên ko thể đặt giá");
                return;
            }
            List<User> bidders = phienHienTai.getBidderList();
            if (!bidders.isEmpty() && bidders.get(bidders.size() - 1).getUserId().equals(bidder.getUserId())) {
                sendError(conn, "Bạn không được đặt giá 2 lần liên tiếp. Hãy chờ người khác đặt giá cao hơn.");
                return;
            }
             boolean bidIsCompleted = auctionSessionService.setPrice(phienHienTai, bidder, giaRa);

             JsonObject response = new JsonObject();
             response.addProperty("event", "BID_RESULT");
             response.addProperty("email", email);
             response.addProperty("giaRa", giaRa);
             response.addProperty("timestamp", System.currentTimeMillis());

             if (bidIsCompleted) {
                 auctionRepositorySQLite.update(phienHienTai);
                 auctionRepositorySQLite.saveBidRecord(phienHienTai.getSessionId(), bidder.getUserId(), giaRa);
                 response.addProperty("status", "SUCCESS");
                 // FIX: Gửi giá MỚI (sau khi đặt), không phải giá cũ
                 response.addProperty("currentPrice", phienHienTai.getCurrentPrice());
                 response.addProperty("fullName", bidder.getFullName()); //Hiển thị full name của người chơi
             } else {
                 response.addProperty("status", "FAILED");
                 // FIX: Cung cấp thông báo lỗi chi tiết hơn dựa vào trạng thái phiên
                 String errorMessage = "Giá không hợp lệ hoặc phiên đã kết thúc";
                 if (phienHienTai.isClosed()) {
                     errorMessage = "Phiên đấu giá đã đóng";
                 } else if (phienHienTai.getStatus() != SessionStatus.IN_PROGRESS) {
                     errorMessage = "Phiên không ở trạng thái IN_PROGRESS (trạng thái hiện tại: " + phienHienTai.getStatus().name() + ")";
                 } else {
                     // Kiểm tra thêm các điều kiện khác
                     double minPrice = phienHienTai.isHasBid()
                         ? phienHienTai.getCurrentPrice() + phienHienTai.getPriceStep()
                         : phienHienTai.getCurrentPrice();
                     if (giaRa < minPrice) {
                         errorMessage = String.format("Giá đặt phải ≥ %.0f (giaHienTai=%.0f + buocGia=%.0f)",
                             minPrice, phienHienTai.getCurrentPrice(), phienHienTai.getPriceStep());
                     } else {
                         errorMessage = "Số dư không đủ hoặc bạn vừa đặt giá, hãy chờ người khác đặt cao hơn";
                     }
                 }
                 response.addProperty("message", errorMessage);
                 logger.warn("❌ BID REJECTED - phien={}, user={}, gia={}, reason={}",
                     phienId, email, giaRa, errorMessage);
             }

            broadcastToRoom(phienId, gson.toJson(response));

        } catch (Exception e) {
            logger.error("Lỗi xử lý bid: " + e.getMessage());
        }
    }

    private void handlePaymentDecision(WebSocket conn, JsonObject json) {
        try {
            String phienId = json.get("phienId").getAsString();
            String email = json.get("email").getAsString();
            boolean accepted = json.get("accepted").getAsBoolean();

            AuctionSession phien = AuctionSessionRegistry.getInstance().find(phienId);
            if (phien == null) {
                phien = auctionRepositorySQLite.findById(phienId);
            }
            if (phien == null) {
                sendError(conn, "Phiên đấu giá không tồn tại");
                return;
            }

            phien.setWinner();
            User winner = phien.getWinner();
            if (winner == null || !email.equals(winner.getEmail())) {
                sendError(conn, "Chỉ người thắng phiên mới được xác nhận thanh toán");
                return;
            }

            boolean ok = auctionSessionService.finalizePayment(phien, accepted);
            if (!ok) {
                sendError(conn, "Không thể xử lý thanh toán cho phiên này");
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý xác nhận thanh toán: " + e.getMessage());
        }
    }

    /**
     * PHƯƠNG THỨC (PRIVATE): broadcastToRoom()
     * Gửi message cho tất cả client trong một phòng
     * <p>
     * Thread-safety:
     * - Lấy room reference (ConcurrentHashMap thread-safe)
     * - Lặp qua tất cả client trong Set
     * - Send là non-blocking (WebSocket library xử lý)
     * - Nếu send fail → log error, tiếp tục send cho client khác
     *
     * @param phienId Room ID
     * @param message Message (dạng JSON string)
     */
    private void broadcastToRoom(String phienId, String message) {
        // 🔹 STEP 1: Lấy room
        Set<WebSocket> room = rooms.get(phienId);

        // 🔹 STEP 2: Check room tồn tại
        if (room == null || room.isEmpty()) {
            logger.info("⚠️ No clients in room: " + phienId);
            return;
        }

        // 🔹 STEP 3: Send message cho tất cả client (thread-safe iteration)
        synchronized (room) {  // Bảo vệ khi iterate qua Set đang bị modify
            for (WebSocket client : room) {
                try {
                    if (client != null && client.isOpen()) {
                        client.send(message);
                    }
                } catch (Exception e) {
                    logger.error("❌ Error sending to client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * PHƯƠNG THỨC: onClose()
     * Gọi khi client disconnect
     * <p>
     * LOGIC:
     * 1. Lấy phienId từ clientRooms
     * 2. Remove client khỏi phòng
     * 3. Nếu phòng trống → xóa phòng
     * 4. Remove từ clientRooms
     * 5. Broadcast "USER_LEFT" cho phòng
     * <p>
     * Thread-safety:
     * - ConcurrentHashMap.remove() là thread-safe
     * - Set.remove() + synchronized để tránh race condition
     *
     * @param code   Close code
     * @param reason Close reason
     * @param remote True nếu client disconnect, False nếu server close
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // 🔹 STEP 1: Lấy phionId mà client đó đang ở
        String phienId = clientRooms.remove(conn);

        if (phienId != null) {
            // 🔹 STEP 2: Remove client khỏi phòng
            Set<WebSocket> room = rooms.get(phienId);
            if (room != null) {
                synchronized (room) {
                    room.remove(conn);

                    // 🔹 STEP 3: Nếu phòng trống → xóa phòng
                    if (room.isEmpty()) {
                        rooms.remove(phienId);
                        logger.info("🗑️ Room cleaned up: " + phienId);
                    }
                }
            }

            // 🔹 STEP 4: Broadcast "USER_LEFT"
            JsonObject response = new JsonObject();
            response.addProperty("event", "USER_LEFT");
            response.addProperty("timestamp", System.currentTimeMillis());
            broadcastToRoom(phienId, gson.toJson(response));
            emailToConn.values().remove(conn);
        }

        logger.info("🚪 Client disconnected: " + conn.getRemoteSocketAddress());
    }

    /**
     * PHƯƠNG THỨC: onError()
     * Gọi khi có lỗi trên WebSocket
     *
     * @param conn WebSocket connection
     * @param ex   Exception
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("❌ WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    /**
     * PHƯƠNG THỨC: onStart()
     * Gọi khi server start thành công
     */
    @Override
    public void onStart() {
        logger.info("🚀 AuctionWebSocketServer started on port " + this.getPort());
    }

    private void sendError(WebSocket conn, String message) {
        JsonObject err = new JsonObject();
        err.addProperty("event", "BID_RESULT");
        err.addProperty("status", "FAILED");
        err.addProperty("message", message);
        conn.send(gson.toJson(err));


    }

    public void broadcastSessionEnded(String phienId) {
        AuctionSession phien = AuctionSessionRegistry.getInstance().find(phienId);

        JsonObject msg = new JsonObject();
        msg.addProperty("event", "SESSION_ENDED");
        msg.addProperty("phienId", phienId);
        msg.addProperty("message", "Phiên đấu giá đã kết thúc");
        msg.addProperty("timestamp", System.currentTimeMillis());

        if (phien != null) {
            msg.addProperty("finalPrice", phien.getCurrentPrice());
            msg.addProperty("status", phien.getStatus().name());
            if (phien.getSeller() != null) {
                msg.addProperty("seller", phien.getSeller().getEmail());
                msg.addProperty("sellerName", phien.getSeller().getFullName());
            }
            if (phien.getWinner() != null) {
                msg.addProperty("winner", phien.getWinner().getEmail());
                msg.addProperty("winnerName", phien.getWinner().getFullName());
            }
        }

        broadcastToRoom(phienId, gson.toJson(msg));   // ← dùng broadcastToRoom, không phải broadcastToSession
        logger.info("📢 Broadcast SESSION_ENDED cho phòng: " + phienId);
    }

    /**
     * Gửi số dư available mới cho một user cụ thể (sau Outbid).
     */
    public void sendBalanceUpdate(String email, double availableBalance) {
        WebSocket conn = emailToConn.get(email);
        if (conn != null && conn.isOpen()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("event", "BALANCE_UPDATE");
            msg.addProperty("availableBalance", availableBalance);
            User user = userRepositorySQLite.findByEmail(email);
            if (user != null) {
                msg.addProperty("actualBalance", user.getActualBalance());
                msg.addProperty("frozenBalance", user.getFrozenBalance());
            }
            msg.addProperty("timestamp", System.currentTimeMillis());
            conn.send(gson.toJson(msg));
            logger.info("📨 Gửi BALANCE_UPDATE tới {}: {}", email, availableBalance);
        }
    }

    /**
     * Gửi yêu cầu thanh toán (chỉ gửi trực tiếp cho người thắng nếu họ đang kết nối,
     * đồng thời broadcast thông tin PAYMENT_REQUEST cho cả phòng).
     *
     * @param phienId      mã phiên
     * @param winnerUserId mã người thắng (ma_nguoi_dung)
     * @param amount       số tiền cần thanh toán
     */
    public void broadcastPaymentRequest(String phienId, String winnerUserId, double amount) {
        JsonObject msg = new JsonObject();
        msg.addProperty("event", "PAYMENT_REQUEST");
        msg.addProperty("phienId", phienId);
        msg.addProperty("amount", amount);
        msg.addProperty("status", "PENDING");
        msg.addProperty("timestamp", System.currentTimeMillis());

        // Cố gắng lấy email người thắng từ DB (thông qua auction repo)
        String winnerEmail = null;
        try {
            AuctionSession phien = auctionRepositorySQLite.findById(phienId);
            if (phien != null && phien.getWinner() != null && winnerUserId != null) {
                // ensure userId khớp
                if (winnerUserId.equals(phien.getWinner().getUserId())) {
                    winnerEmail = phien.getWinner().getEmail();
                    msg.addProperty("winner", winnerEmail);
                    msg.addProperty("winnerName", phien.getWinner().getFullName());
                }
            }
        } catch (Exception ex) {
            logger.warn("Không lấy được thông tin phiên khi broadcastPaymentRequest: " + ex.getMessage());
        }

        // 1) Nếu biết email và client đang kết nối → gửi trực tiếp
        if (winnerEmail != null) {
            WebSocket ws = emailToConn.get(winnerEmail);
            if (ws != null && ws.isOpen()) {
                try {
                    ws.send(gson.toJson(msg));
                    logger.info("📨 Gửi PAYMENT_REQUEST trực tiếp tới {} cho phiên {}", winnerEmail, phienId);
                } catch (Exception e) {
                    logger.error("Lỗi gửi PAYMENT_REQUEST trực tiếp: " + e.getMessage());
                }
            }
        }

        // 2) Luôn broadcast lên phòng để UI/quan sát viên biết trạng thái chờ thanh toán
        try {
            broadcastToRoom(phienId, gson.toJson(msg));
            logger.info("📢 Broadcast PAYMENT_REQUEST cho phòng: " + phienId);
        } catch (Exception e) {
            logger.error("Lỗi broadcast PAYMENT_REQUEST cho phòng {}: {}", phienId, e.getMessage());
        }
    }

    public void broadcastPaymentResult(String phienId, boolean paid, String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("event", "PAYMENT_RESULT");
        msg.addProperty("phienId", phienId);
        msg.addProperty("status", paid ? "PAID" : "CANCELLED");
        msg.addProperty("message", message);
        msg.addProperty("timestamp", System.currentTimeMillis());
        broadcastToRoom(phienId, gson.toJson(msg));
        logger.info("📢 Broadcast PAYMENT_RESULT cho phòng {}: {}", phienId, msg);
    }
}

