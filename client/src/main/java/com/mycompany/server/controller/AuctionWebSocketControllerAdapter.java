package com.mycompany.server.controller;

import com.google.gson.JsonObject;
import com.mycompany.controller.BiddingRoomController;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.SessionManager;
import com.mycompany.server.websocket.AuctionWebSocketClient;
import com.mycompany.websocket.AuctionWebSocketListener;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.slf4j.Logger;

/**
 * AuctionWebSocketControllerAdapter - Adapter để integrate WebSocket vào BiddingRoomController
 *
 * MỤC ĐÍCH:
 * - Implement AuctionWebSocketListener
 * - Handle WebSocket events và update HomeController UI
 * - Manage connection lifecycle
 * - Provide methods để send bids
 * - Giống action của các controller đã có
 *
 * DESIGN PATTERN:
 * - Adapter pattern: chuyển WebSocket events thành Controller actions
 * - Observer pattern: Controller observe WebSocket events
 *
 * USAGE IN BiddingRoomController:
 *
 * public class BiddingRoomController implements Initializable {
 * @FXML private Label priceLabel;
 *
 * private AuctionWebSocketClient wsClient;
 * private AuctionWebSocketControllerAdapter adapter;
 *
 * @Override
 * public void initialize(URL url, ResourceBundle resources) {
 * try {
 * // Initialize WebSocket
 * wsClient = AuctionWebSocketClient.getInstance();
 * adapter = new AuctionWebSocketControllerAdapter(this, priceLabel);
 * wsClient.setListener(adapter);
 *
 * // Connect to server (block until connected)
 * wsClient.connect();
 * logger.info("✅ Connected to WebSocket server");
 *
 * // Join auction room
 * String phienId = "PHIEN001";  // Get from current auction session
 * String userId = SessionManager.getInstance().getUserId();
 * wsClient.sendJoin(phienId, userId);
 *
 * } catch (Exception e) {
 * HandleNavigationAndAlert.getInstance().showAlert(
 * Alert.AlertType.ERROR,
 * "Connection Error",
 * "Failed to connect to auction server: " + e.getMessage()
 * );
 * }
 * }
 *
 * @FXML
 * public void onClickedBid(ActionEvent event) {
 * try {
 * String phienId = "PHIEN001";
 * String userId = SessionManager.getInstance().getUserId();
 * double giaRa = Double.parseDouble(bidAmountField.getText());
 *
 * // Send bid (non-blocking)
 * wsClient.sendBid(phienId, userId, giaRa);
 *
 * } catch (NumberFormatException e) {
 * HandleNavigationAndAlert.getInstance().showAlert(
 * Alert.AlertType.ERROR,
 * "Invalid Input",
 * "Please enter a valid amount"
 * );
 * }
 * }
 *
 * @Override
 * public void cleanup() {
 * try {
 * wsClient.disconnect();
 * } catch (InterruptedException e) {
 * logger.error("Error disconnecting: " + e.getMessage());
 * }
 * }
 * }
 */
public class AuctionWebSocketControllerAdapter implements AuctionWebSocketListener {

    // Reference to UI components
    private final BiddingRoomController controller;
    private final Label priceLabel;
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionWebSocketControllerAdapter.class);
    /**
     * Constructor
     *
     * @param controller HomeController instance
     * @param priceLabel Label to display current price
     */
    public AuctionWebSocketControllerAdapter(BiddingRoomController controller, Label priceLabel) {
        this.controller = controller;
        this.priceLabel = priceLabel;
    }

    /**
     * CALLBACK: onBidResult()
     * Called when server broadcasts BID_RESULT event
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread (safe to update UI)
     * Platform.runLater() already handled by client
     *
     * MESSAGE FORMAT:
     * {
     * "event": "BID_RESULT",
     * "userId": "USER001",
     * "giaRa": 150000.0,
     * "status": "SUCCESS"|"FAILED",
     * "currentPrice": 150000.0,
     * "message": "error message if failed",
     * "timestamp": 1710000000000
     * }
     *
     * @param message JSON message from server
     */

    @Override
    public void onBidResult(JsonObject message) {
        if (priceLabel == null) {
            logger.warn("⚠️ onBidResult: priceLabel is null, bỏ qua cập nhật UI");
            return;
        }
        try {
            String status = message.get("status").getAsString();
            logger.info("📊 onBidResult status={}, message={}", status, message);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                double newPrice = message.get("currentPrice").getAsDouble();
                String displayId = message.has("fullName") && !message.get("fullName").isJsonNull()
                    ? message.get("fullName").getAsString()
                    : message.get("email").getAsString();

                // syncNewPrice và addBidHistory phải chạy trên JavaFX thread.
                // onBidResult đã được gọi từ Platform.runLater() trong AuctionWebSocketClient,
                // nên đây an toàn để update UI trực tiếp.
                controller.syncNewPrice(newPrice, displayId);
                controller.addBidHistory(displayId, newPrice);
            } else {
                String msg = message.has("message") ? message.get("message").getAsString() : "Không rõ lý do";
                logger.warn("❌ BID_RESULT FAILED: {}", msg);
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Đặt giá thất bại");
                alert.setHeaderText(null);
                alert.setContentText(msg);
                alert.show();
            }
        } catch (Exception e) {
            logger.error("❌ Error processing bid result: {}", e.getMessage(), e);
        }
    }

    /**
     * CALLBACK: onUserJoined()
     * Called when another user joins the auction room
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread
     *
     * @param message JSON message from server
     */
    @Override
    public void onUserJoined(JsonObject message) {
        try {
            String email = message.get("email").getAsString();

            logger.info("👤 User joined: " + email);

            // Update UI: increment online user count if you have such display
            // Example: onlineCountLabel.setText("Online: 5");

            // Optional: Show notification
            // HandleNavigationAndAlert.getInstance().showAlert(
            //     Alert.AlertType.INFORMATION,
            //     "Người dùng mới",
            //     email + " đã vào phòng"
            // );

        } catch (Exception e) {
            logger.error("❌ Error processing user joined: " + e.getMessage());
        }
    }

    /**
     * CALLBACK: onUserLeft()
     * Called when a user leaves the auction room
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread
     *
     * @param message JSON message from server
     */
    @Override
    public void onUserLeft(JsonObject message) {
        try {
            logger.info("👤 User left room");

            // Update UI: decrement online user count
            // Example: onlineCountLabel.setText("Online: 4");

        } catch (Exception e) {
            logger.error("❌ Error processing user left: " + e.getMessage());
        }
    }

    /**
     * CALLBACK: onConnected()
     * Called when WebSocket connection established
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread
     *
     * USAGE:
     * - Enable bid button
     * - Show "Connected" status
     * - Update UI to indicate ready state
     */
    @Override
    public void onConnected() {
        // KHÔNG ghi đè label giá ở đây — giá đã được load từ API trước đó.
        // Chỉ log để biết kết nối thành công; UI sẽ tự cập nhật khi nhận BID_RESULT.
        logger.info("✅ WebSocket connected — ready to receive real-time updates");
    }

    /**
     * CALLBACK: onDisconnected()
     * Called when WebSocket connection closed
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread
     *
     * USAGE:
     * - Disable bid button
     * - Show "Disconnected" status
     * - Prevent further bid attempts
     */

    @Override
    public void onDisconnected() {
        if (priceLabel == null) return;
        priceLabel.setStyle("-fx-text-fill: #757575;");
        priceLabel.setText("Mất kết nối đến server...");
    }

    /**
     * CALLBACK: onError()
     * Called when WebSocket error occurred
     *
     * ⚠️ THREAD CONTEXT: JavaFX thread
     *
     * @param errorMessage Error description
     *
     * USAGE:
     * - Show error dialog
     * - Log error
     * - Optionally retry or auto-reconnect
     */
    @Override
    public void onError(String errorMessage) {
        if (priceLabel == null) return;
        priceLabel.setStyle("-fx-text-fill: #F44336;");
        priceLabel.setText("Lỗi kết nối: " + errorMessage);
    }

    /**
     * UTILITY METHOD: sendBid()
     * Helper method để gửi bid (có thể call từ HomeController)
     *
     * THREAD-SAFE: Can call from JavaFX thread or any thread
     *
     * @param phienId Auction session ID
     * @param giaRa Bid amount
     */
    public void sendBid(String phienId, double giaRa) {
        try {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            AuctionWebSocketClient client = AuctionWebSocketClient.getInstance();

            if (!client.isConnected()) {
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Mất kết nối",
                    "Bạn chưa kết nối đến server. Vui lòng tải lại trang."
                );
                return;
            }

            // Send bid (non-blocking)
            client.sendBid(phienId, email, giaRa);

            logger.info("💰 Sent bid: " + giaRa);

        } catch (Exception e) {
            logger.error("❌ Error sending bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSessionEnded(JsonObject message) {
        javafx.application.Platform.runLater(() -> {
            controller.disableBidding();

            String winner = message.has("winner") && !message.get("winner").isJsonNull()
                ? message.get("winner").getAsString() : null;
            String winnerName = message.has("winnerName") && !message.get("winnerName").isJsonNull()
                ? message.get("winnerName").getAsString() : null;
            String seller = message.has("seller") && !message.get("seller").isJsonNull()
                ? message.get("seller").getAsString() : null;
            double finalPrice = message.has("finalPrice") && !message.get("finalPrice").isJsonNull()
                ? message.get("finalPrice").getAsDouble() : 0;

            String currentUserEmail = SessionManager.getInstance().getCurrentUser().getEmail();
            boolean isWinner = winner != null && winner.equals(currentUserEmail);
            boolean isSeller = seller != null && seller.equals(currentUserEmail);
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
            String priceText = fmt.format(finalPrice) + " VNĐ";

            if (isWinner) {
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.INFORMATION,
                    "Phiên đấu giá kết thúc",
                    "Bạn đã thắng phiên. Hệ thống đã thanh toán " + priceText + " từ số dư đóng băng."
                );
            } else if (isSeller) {
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.INFORMATION,
                    "Phiên đấu giá kết thúc",
                    "Phiên của bạn đã kết thúc. Người mua: " +
                        (winnerName != null ? winnerName : winner) + ". Số tiền: " + priceText + "."
                );
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.INFORMATION,
                    "Phiên đấu giá kết thúc",
                    winner != null
                        ? "Phiên đã kết thúc. Người thắng: " + (winnerName != null ? winnerName : winner)
                        : "Phiên đấu giá đã kết thúc (không có người đặt giá)."
                );
            }
            controller.navigateToHome();
        });
    }

    @Override
    public void onBalanceUpdate(JsonObject message) {
        com.mycompany.models.User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        if (message.has("actualBalance") && !message.get("actualBalance").isJsonNull()) {
            user.setActualBalance(message.get("actualBalance").getAsDouble());
        }
        if (message.has("frozenBalance") && !message.get("frozenBalance").isJsonNull()) {
            user.setFrozenBalance(message.get("frozenBalance").getAsDouble());
        }
    }
    @Override
    public void onPaymentResult(JsonObject message) {
        javafx.application.Platform.runLater(() -> {
            String status = message.has("status") ? message.get("status").getAsString() : "";
            String msg = message.has("message") ? message.get("message").getAsString() : "Đã xử lý thanh toán";
            Alert.AlertType type = "PAID".equals(status) ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
            HandleNavigationAndAlert.getInstance().showAlert(type, "Kết quả thanh toán", msg);
            controller.navigateToHome();
        });
    }
}