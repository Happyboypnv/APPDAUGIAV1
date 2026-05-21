package com.mycompany.server.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.websocket.AuctionWebSocketListener;
import javafx.application.Platform;

import javafx.scene.control.Alert;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AuctionWebSocketClient - WebSocket Client cho JavaFX real-time auction
 *
 * MỤC ĐÍCH:
 * - Kết nối tới server WebSocket (port 8081)
 * - Gửi BID request lên server
 * - Nhận updates từ server và update UI real-time
 * - Đảm bảo UI operations luôn chạy trên JavaFX thread
 *
 * TÍNH NĂNG CHÍNH:
 * - Singleton pattern: chỉ 1 client connection duy nhất
 * - connectBlocking(): block cho đến khi connect thành công
 * - sendBid(): gửi giá đặt lên server (non-blocking)
 * - onMessage(): nhận kết quả từ server, update UI via Platform.runLater()
 * - Graceful disconnect và cleanup
 *
 * THREAD SAFETY:
 * - WebSocketClient callback (onMessage) chạy trên WebSocket thread
 * - Sử dụng Platform.runLater() để switch sang JavaFX thread trước update UI
 * - sendBid() có thể call từ JavaFX thread hoặc WebSocket thread → thread-safe
 * - volatile field để đảm bảo visibility across threads
 *
 * LUỒNG HOẠT ĐỘNG:
 * 1. User click "Đặt giá" → onClickedBid() gọi client.sendBid()
 * 2. sendBid() gửi JSON message qua WebSocket → not blocking
 * 3. Server xử lý, broadcast kết quả cho tất cả clients
 * 4. onMessage() nhận kết quả trên WebSocket thread
 * 5. Platform.runLater() switch sang JavaFX thread
 * 6. Update UI (change price label, color, v.v.)
 *
 * CALLBACK:
 * - Cần setListener() nếu muốn inject custom listener (ví dụ: update UI)
 * - Listener.onBidResult() được gọi từ JavaFX thread (safe to update UI)
 */
public class AuctionWebSocketClient extends WebSocketClient{

    // ===== THREAD-SAFE FIELDS =====

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleNavigationAndAlert.class);
    // Singleton instance
    private static volatile AuctionWebSocketClient instance;

    private final static Logger logger = LoggerFactory.getLogger(AuctionWebSocketClient.class);

    // Lock/Mutex để ensure một lần chỉ 1 instance được tạo (thread-safe singleton)
    private static final Object INSTANCE_LOCK = new Object();

    // Listener callback (custom behavior khi nhận message từ server)
    // volatile: đảm bảo changes từ một thread visible lập tức cho thread khác
    private volatile AuctionWebSocketListener listener;

    // Gson: JSON serialization (thread-safe >= 2.8.9)
    private final Gson gson = new Gson();

    // Latch để block cho đến khi connected successfully
    // CountDownLatch: thread-safe counter, khi count = 0 → all waiting threads wake up
    private  CountDownLatch connectionLatch = new CountDownLatch(1);

    // Connection status
    // volatile: ensure visibility across threads
    private volatile boolean isConnected = false;

    /**
     * Constructor: Khởi tạo WebSocketClient với server URI
     *
     * @param uri WebSocket server address (ví dụ: ws://localhost:8081)
     * @throws URISyntaxException nếu URI format sai
     */
    public AuctionWebSocketClient(URI uri) throws URISyntaxException {
        super(uri);
        logger.info("🎯 AuctionWebSocketClient initialized for: " + uri);
    }

    /**
     * PHƯƠNG THỨC (STATIC): getInstance()
     * Lấy Singleton instance của WebSocket client
     *
     * THREAD-SAFETY:
     * - Sử dụng double-checked locking pattern
     * - Synchronized block chỉ execute nếu instance null (avoid lock overhead)
     * - volatile field: ensure all threads thấy updates immediately
     *
     * @return Singleton AuctionWebSocketClient instance
     */
    public static AuctionWebSocketClient getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null || instance.isClosed()) {
                try {
                    instance = new AuctionWebSocketClient(
                            new URI("ws://26.71.32.210:8081"));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return instance;
        }
    }
    public static void resetInstance() {
        synchronized (INSTANCE_LOCK) { instance = null; }
    }
    /**
     * PHƯƠNG THỨC: connectToServer()
     * (Đã đổi tên từ connect() để tránh đụng độ với hàm connect() của class cha)
     * Kết nối tới server và block cho đến khi successfully connected
     *
     * THREAD-SAFETY & BEHAVIOR:
     * - WebSocketClient.connectBlocking(): blocks current thread
     * - Chỉ return khi onOpen() được call (connection established)
     * - Timeout 5s: nếu không connect được → throw InterruptedException
     * - connectionLatch countdown → onOpen() gọi latch.countDown()
     * - Sau khi return, isConnected = true
     *
     * @throws InterruptedException nếu timeout hoặc thread interrupt
     */
    public void connectToServer() {
        connectionLatch = new CountDownLatch(1); // reset
        try {
            boolean connected = this.connectBlocking(5, TimeUnit.SECONDS);
            if (connected) {
                isConnected = true;
                connectionLatch.await(5, TimeUnit.SECONDS);
            } else {
                isConnected = false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * PHƯƠNG THỨC: sendBid()
     * Gửi bid message lên server
     *
     * THREAD-SAFETY:
     * - Có thể call từ JavaFX thread hoặc bất kỳ thread nào
     * - WebSocketClient.send() là synchronized internally (thread-safe)
     * - isConnected volatile: check before sending
     * - Exception handled gracefully
     *
     * MESSAGE FORMAT (JSON):
     * {
     * "action": "BID",
     * "phienId": "auction session ID",
     * "userId": "current user ID",
     * "giaRa": 100000.0
     * }
     *
     * @param phienId  Auction session ID (ví dụ: "PHIEN001")
     * @param email   Current user email (ví dụ: "phong@gmail.com")
     * @param giaRa    Bid amount (ví dụ: 100000.0)
     */
    public void sendBid(String phienId, String email, double giaRa) {
        // 🔹 STEP 1: Validate connection
        if (!isConnected) {
            logger.error("❌ WebSocket not connected");
            return;
        }

        try {
            // 🔹 STEP 2: Create BID message
            JsonObject bidMessage = new JsonObject();
            bidMessage.addProperty("action", "BID");
            bidMessage.addProperty("phienId", phienId);
            bidMessage.addProperty("email", email);
            bidMessage.addProperty("giaRa", giaRa);

            // 🔹 STEP 3: Send to server (non-blocking)
            // send() là non-blocking: return immediately, message queued for sending
            String jsonString = gson.toJson(bidMessage);
            this.send(jsonString);

            logger.info("💬 Sent BID: " + jsonString);

        } catch (Exception e) {
            logger.error("❌ Error sending bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC: sendJoin()
     * Gửi JOIN message để vào phòng auction
     *
     * THREAD-SAFETY:
     * - Same as sendBid()
     * - Non-blocking send
     * - Thread-safe WebSocket library
     *
     * @param phienId Auction session ID
     * @param email  Current user ID
     */
    public void sendJoin(String phienId, String email) {
        if (!isConnected) {
            logger.error("❌ WebSocket not connected");
            return;
        }

        try {
            JsonObject joinMessage = new JsonObject();
            joinMessage.addProperty("action", "JOIN");
            joinMessage.addProperty("phienId", phienId);
            joinMessage.addProperty("email", email);

            String jsonString = gson.toJson(joinMessage);
            this.send(jsonString);

            logger.info("💬 Sent JOIN: " + jsonString);

        } catch (Exception e) {
            logger.error("❌ Error sending join: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC: setListener()
     * Inject custom listener callback cho message events
     *
     * THREAD-SAFETY:
     * - volatile field assignment is atomic
     * - Listener callback sẽ be called từ Platform.runLater()
     * - Guaranteed to run on JavaFX thread
     *
     * @param listener AuctionWebSocketListener implementation
     */
    public void setListener(AuctionWebSocketListener listener) {
        this.listener = listener;
    }

    /**
     * CALLBACK: onOpen()
     * Gọi khi WebSocket connection established
     *
     * Thread context: WebSocket internal thread
     * Actions:
     * - Set isConnected = true
     * - Countdown latch to unblock connect() method
     * - Notify listener (optional)
     *
     * @param handshake Server handshake
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("✅ WebSocket connection opened");
        isConnected = true;

        // Countdown latch → unblock connectToServer() method
        connectionLatch.countDown();

        // Notify listener (switch to JavaFX thread)
        if (listener != null) {
            Platform.runLater(() -> listener.onConnected());
        }
    }

    /**
     * CALLBACK: onMessage()
     * Gọi khi receive message từ server
     *
     * ⚠️ THREAD-SAFETY CRITICAL POINT ⚠️
     * - Callback này chạy trên WebSocket thread (KHÔNG phải JavaFX thread)
     * - KHÔNG ĐƯỢC phép update UI trực tiếp từ đây
     * - PHẢI dùng Platform.runLater() để switch sang JavaFX thread
     *
     * ĐÚNG:
     * ✅ Platform.runLater(() -> label.setText("Price: 100"));
     *
     * SAI:
     * ❌ label.setText("Price: 100");  // Will throw exception
     *
     * Thread context: WebSocket thread (NOT JavaFX thread)
     *
     * MESSAGE HANDLING:
     * 1. Parse JSON message từ server
     * 2. Get event type (BID_RESULT, USER_JOINED, USER_LEFT)
     * 3. Switch to JavaFX thread via Platform.runLater()
     * 4. Call listener callback (safe to update UI)
     *
     * @param message JSON message từ server
     */
    @Override
    public void onMessage(String message) {
        try {
            // 🔹 STEP 1: Parse JSON (safe, no UI update yet)
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String event = json.get("event").getAsString();

            logger.info("📩 Received message: " + event);

            // 🔹 STEP 2: Switch to JavaFX thread before UI update
            // Platform.runLater(): schedule code to run on JavaFX thread
            // Guaranteed to be called on JavaFX thread → safe to update UI
            Platform.runLater(() -> {
                try {
                    // 🔹 STEP 3: Handle different event types
                    switch (event) {
                        case "BID_RESULT":
                            if (listener != null) {
                                listener.onBidResult(json);
                            }
                            break;

                        case "USER_JOINED":
                            if (listener != null) {
                                listener.onUserJoined(json);
                            }
                            break;

                        case "USER_LEFT":
                            if (listener != null) {
                                listener.onUserLeft(json);
                            }
                            break;

                        case "SESSION_ENDED":
                            if (listener != null) listener.onSessionEnded(json);
                            break;

                        default:
                            logger.info("⚠️ Unknown event: " + event);
                    }
                } catch (Exception e) {
                    logger.error("❌ Error in UI callback: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            logger.error("❌ Error parsing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * CALLBACK: onClose()
     * Gọi khi WebSocket connection closed
     *
     * Thread context: WebSocket thread
     *
     * @param code    Close code
     * @param reason  Close reason
     * @param remote  true if closed by server, false if closed by client
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("🚪 WebSocket closed: " + reason);
        isConnected = false;

        if (listener != null) {
            Platform.runLater(listener::onDisconnected);
        }
    }

    /**
     * CALLBACK: onError()
     * Gọi khi có lỗi WebSocket
     *
     * Thread context: WebSocket thread
     *
     * @param ex Exception
     */
    @Override
    public void onError(Exception ex) {
        logger.error("❌ WebSocket error: " + ex.getMessage());
        ex.printStackTrace();

        if (listener != null) {
            Platform.runLater(() -> listener.onError(ex.getMessage()));
        }
    }

    /**
     * PHƯƠNG THỨC: disconnect()
     * Graceful disconnect từ server
     *
     * THREAD-SAFETY:
     * - closeBlocking() là synchronized internally
     * - Safe to call from any thread
     * - onClose() sẽ be called after disconnection
     *
     * @throws InterruptedException nếu thread interrupt
     */
    public void disconnect() throws InterruptedException {
        logger.info("🔌 Disconnecting from WebSocket server...");
        this.closeBlocking();
        isConnected = false;
    }

    /**
     * PHƯƠNG THỨC: isConnected()
     * Check connection status (volatile field)
     *
     * @return true nếu connected, false nếu không
     */
    public boolean isConnected() {
        return isConnected;
    }
}