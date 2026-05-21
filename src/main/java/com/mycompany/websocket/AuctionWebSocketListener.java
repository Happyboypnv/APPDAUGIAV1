package com.mycompany.websocket;

import com.google.gson.JsonObject;

/**
 * dinh nghia cac su kien co the xu ly tuong ung voi WebSocket client
 * AuctionWebSocketListener - Interface for WebSocket event callbacks
 *
 * MỤC ĐÍCH:
 * - Định nghĩa các callback events cho WebSocket client
 * - Cho phép Controller/UI react to server events
 * - Pattern: Observer/Listener pattern
 *
 * TÍNH NĂNG:
 * - onBidResult(): Server gửi kết quả bid (successful/failed)
 * - onUserJoined(): Người dùng mới vào phòng
 * - onUserLeft(): Người dùng rời khỏi phòng
 * - onConnected(): Connection established
 * - onDisconnected(): Connection closed
 * - onError(): Connection error occurred
 *
 * THREAD-SAFETY:
 * - Tất cả callbacks được call từ JavaFX thread (via Platform.runLater())
 * - Safe to update UI components directly
 * - Không cần synchronization hoặc locks
 *
 * USAGE:
 * 1. Implements AuctionWebSocketListener
 * 2. Implement callbacks để update UI
 * 3. Call client.setListener(this) để register
 * 4. Callbacks tự động được call khi events happen
 */
public interface AuctionWebSocketListener {

    /**
     * Gọi khi server broadcast BID RESULT
     *
     * Message JSON:
     * {
     *   "event": "BID_RESULT",
     *   "userId": "USER001",
     *   "giaRa": 100000.0,
     *   "status": "SUCCESS" or "FAILED",
     *   "currentPrice": 100000.0,
     *   "message": "error message if failed",
     *   "timestamp": 1234567890
     * }
     *
     * CONTEXT: JavaFX thread (safe to update UI)
     *
     * @param message JSON object from server
     */
    void onBidResult(JsonObject message);

    /**
     * Gọi khi người dùng mới vào phòng auction
     *
     * Message JSON:
     * {
     *   "event": "USER_JOINED",
     *   "userId": "USER002",
     *   "timestamp": 1234567890
     * }
     *
     * USAGE: Update online user count, show notification
     *
     * @param message JSON object from server
     */
    void onUserJoined(JsonObject message);

    /**
     * Gọi khi người dùng rời khỏi phòng auction
     *
     * Message JSON:
     * {
     *   "event": "USER_LEFT",
     *   "timestamp": 1234567890
     * }
     *
     * USAGE: Update online user count
     *
     * @param message JSON object from server
     */
    void onUserLeft(JsonObject message);

    /**
     * Gọi khi WebSocket connection established
     *
     * USAGE: Enable bid buttons, show "Ready" status
     */
    void onConnected();

    /**
     * Gọi khi WebSocket connection closed
     *
     * USAGE: Disable bid buttons, show "Disconnected" status
     */
    void onDisconnected();

    /**
     * Gọi khi có lỗi WebSocket
     *
     * @param errorMessage Error description
     *
     * USAGE: Show error dialog
     */
    void onError(String errorMessage);

    void onSessionEnded(JsonObject message);
}

