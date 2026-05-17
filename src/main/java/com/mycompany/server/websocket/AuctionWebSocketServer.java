package com.mycompany.server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.models.AuctionSession;
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

            // FIX: Null check thay vì NPE
            if (phienHienTai == null) {
                sendError(conn, "Phiên đấu giá không tồn tại");
                return;
            }

            // FIX: Lấy giá SAU khi setPrice thành công, không phải trước
            User bidder = userRepositorySQLite.findByEmail(email);
            if (bidder == null) {
                sendError(conn, "Người dùng không tồn tại");
                return;
            }
            boolean bidIsCompleted = auctionSessionService.setPrice(
                    phienHienTai,
                    userRepositorySQLite.findByEmail(email),
                    giaRa
            );

            JsonObject response = new JsonObject();
            response.addProperty("event", "BID_RESULT");
            response.addProperty("email", email);
            response.addProperty("giaRa", giaRa);
            response.addProperty("timestamp", System.currentTimeMillis());

            if (bidIsCompleted) {
                auctionRepositorySQLite.update(phienHienTai);
                response.addProperty("status", "SUCCESS");
                // FIX: Gửi giá MỚI (sau khi đặt), không phải giá cũ
                response.addProperty("currentPrice", phienHienTai.getCurrentPrice());
            } else {
                response.addProperty("status", "FAILED");
                response.addProperty("message", "Giá không hợp lệ hoặc phiên đã kết thúc");
            }

            broadcastToRoom(phienId, gson.toJson(response));

        } catch (Exception e) {
            logger.error("Lỗi xử lý bid: " + e.getMessage());
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
}

