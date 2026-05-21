package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.server.controller.AuctionWebSocketControllerAdapter;
import com.mycompany.server.dto.PhienDauGiaDTO;
import com.mycompany.server.websocket.AuctionWebSocketClient;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class BiddingRoomController implements Initializable {

    // --- KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN (fx:id) ---
    @FXML
    private Label auctionNameLabel; // Tên phiên đấu giá
    @FXML
    private Label currentTimeLabel; // Đồng hồ góc phải
    @FXML
    private Button closeButton; // Nút X đóng cửa sổ

    @FXML
    private ImageView mainWatchImage; // Ảnh sản phẩm chính

    @FXML
    private ListView<String> bidHistoryListView; // Danh sách lịch sử đấu giá

    @FXML
    private Label currentPriceLabel; // Giá hiện tại (Ví dụ: 135,000,000 VND)
    @FXML
    private Label topBidderLabel; // Tên người giữ giá cao nhất

    @FXML
    private Button decreaseBidButton; // Nút trừ (-)
    @FXML
    private TextField bidAmountField; // Ô nhập giá đặt
    @FXML
    private Button increaseBidButton; // Nút cộng (+)
    @FXML
    private Button placeBidButton; // Nút ĐẶT GIÁ NGAY

    // --- WEBSOCKET FIELDS ---
    private AuctionWebSocketClient wsClient;
    private AuctionWebSocketControllerAdapter adapter;
    private String currentPhienId;
    private Thread wsThread;
    private volatile boolean isDestroyed = false; // Biến trạng thái kết nối WebSocket
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BiddingRoomController.class);
    // --- CÁC BIẾN LOGIC ---
    private double currentPrice = 135000000; // Giá cao nhất hiện tại
    private double suggestedPrice = 0;
    private double stepPrice = 5000000; // Bước giá (mỗi lần +/- 5 triệu)
    private DecimalFormat formatter = new DecimalFormat("#,###"); // Định dạng tiền tệ kiểu 135,000,000
    private ObservableList<String> bidHistoryList = FXCollections.observableArrayList();

    /**
     * Hàm này tự động chạy khi giao diện FXML được load
     */
    @Override
    // ✅ SAU - gán currentPhienId TRƯỚC KHI tạo bất kỳ thread nào để đảm bảo tất cả các thread đều có giá trị currentPhienId hợp lệ, tránh lỗi null pointer khi truy cập currentPhienId trong các thread đó.
    public void initialize(URL location, ResourceBundle resources) {
        // ⭐ BƯỚC 1: Gán currentPhienId NGAY ĐẦU TIÊN
        currentPhienId = SessionManager.getInstance().getCurrentPhienId();

        startClock();
        setupButtonActions();
        bidHistoryListView.setItems(bidHistoryList); // fix Bug 10 luôn
        Platform.runLater(() -> {
            if (placeBidButton != null && placeBidButton.getScene() != null) {
                placeBidButton.getScene().getRoot().setUserData(this);
            }
        });
        // BƯỚC 2: Load thông tin phiên thực tế (currentPhienId đã có giá trị)
        new Thread(() -> {
            PhienDauGiaDTO phien = ApiClient.getAuctionById(
                    currentPhienId,  // ✅ không còn null
                    SessionManager.getInstance().getServerToken()
            );
            if (phien != null) {
                Platform.runLater(() -> {
                    currentPrice = phien.giaHienTai;
                    stepPrice = currentPrice * 0.06;
                    suggestedPrice = currentPrice + stepPrice;  // ← lưu lại giá đề xuất chính xác
                    updateBidAmountField(suggestedPrice);
                    if (auctionNameLabel != null) {
                        auctionNameLabel.setText("PHÒNG ĐẤU GIÁ: " + phien.tenPhien);
                    }
                    if (currentPriceLabel != null) {
                        currentPriceLabel.setText(formatter.format(currentPrice) + " VNĐ");
                    }
                });
            }
        }).start();

        // BƯỚC 3: Kết nối WebSocket (currentPhienId đã có giá trị)
        // ✅ SAU
        wsThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (isDestroyed) return;

                wsClient = AuctionWebSocketClient.getInstance();
                adapter = new AuctionWebSocketControllerAdapter(this, currentPriceLabel);
                wsClient.setListener(adapter);

                // ⭐ Chỉ connect nếu chưa connected
                if (!wsClient.isConnected()) {
                    wsClient.connectToServer();
                }

                // ⭐ Chỉ join nếu connect thành công
                if (wsClient.isConnected()) {
                    String email = SessionManager.getInstance().getCurrentUser().getEmail();
                    wsClient.sendJoin(currentPhienId, email);
                    logger.info("✅ Đã kết nối và JOIN phòng: " + currentPhienId);
                } else {
                    logger.error("❌ Không thể kết nối WebSocket sau timeout");
                    Platform.runLater(() ->
                            HandleNavigationAndAlert.getInstance().showAlert(
                                    Alert.AlertType.WARNING,
                                    "Cảnh báo",
                                    "Không thể kết nối real-time. Tính năng đặt giá có thể bị chậm."
                            )
                    );
                }
            } catch (Exception e) {
                logger.error("❌ Lỗi kết nối WebSocket: " + e.getMessage());
            }
        });
        wsThread.setDaemon(true);
        wsThread.setName("BiddingRoom-WebSocket-Thread");
        wsThread.start();
    }

    // --- XỬ LÝ SỰ KIỆN NÚT BẤM ---
    private void setupButtonActions() {
        // Nút Giảm giá
        decreaseBidButton.setOnAction(event -> {
            double currentInput = parseBidAmount(bidAmountField.getText());
            double minRequired = currentPrice + stepPrice;
            if (currentInput - stepPrice >= minRequired) {
                updateBidAmountField(currentInput - stepPrice);
            } else {
                updateBidAmountField(minRequired); // reset về mức tối thiểu
            }
        });

        // Nút Tăng giá
        increaseBidButton.setOnAction(event -> {
            double currentInput = parseBidAmount(bidAmountField.getText());
            updateBidAmountField(currentInput + stepPrice);
        });

        // Nút Đặt giá ngay
        placeBidButton.setOnAction(event -> {
            handlePlaceBid();
        });

        // Nút Đóng cửa sổ
        closeButton.setOnAction(event -> {
            onClose(); // Đóng kết nối WebSocket trước khi thoát
            System.out.println("Đóng phòng đấu giá...");
            try {
                HandleNavigationAndAlert.getInstance().handleGoToHome(event);
            } catch (IOException e) {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể tải giao diên");
            }
        });
    }

    // --- LOGIC ĐẶT GIÁ ---
    private void handlePlaceBid() {
        double myBid = parseBidAmount(bidAmountField.getText());

        // Validate: giá phải >= suggestedPrice (tức currentPrice + stepPrice)
        double minRequired = currentPrice + stepPrice;
        if (myBid < minRequired) {
            showAlert("Lỗi đặt giá",
                "Giá tối thiểu phải là: " + formatter.format(minRequired) + " VNĐ");
            updateBidAmountField(suggestedPrice);
            return;
        }

        // Gửi qua WebSocket thay vì cập nhật local
        if (wsClient != null && wsClient.isConnected() && currentPhienId != null) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            wsClient.sendBid(currentPhienId, email, myBid);
        } else {
            // Fallback: gọi REST API nếu mất kết nối
            System.out.println("WebSocket chưa kết nối, sử dụng API Fallback...");
            String token = SessionManager.getInstance().getCurrentToken();
            ApiClient.createBid(currentPhienId, myBid, token);
        }
    }

    // --- HÀM ĐÓNG KẾT NỐI WEBSOCKET KHI THOÁT PHÒNG ---
    public void onClose() {
        isDestroyed = true;
        if (wsThread != null) wsThread.interrupt();
        if (wsClient != null && wsClient.isConnected()) {
            try { wsClient.disconnect(); } catch (Exception ignored) {}
        }
    }

    // Getter để AuctionWebSocketControllerAdapter có thể access
    public Label getCurrentPriceLabel() {
        return currentPriceLabel;
    }

    // --- CÁC HÀM TIỆN ÍCH CHUẨN HOÁ DỮ LIỆU ---
    private void updateBidAmountField(double amount) {
        bidAmountField.setText(formatter.format(amount));
    }

    private double parseBidAmount(String text) {
        try {
            // Xóa dấu phẩy trước khi parse số (Ví dụ: "140,000,000" -> "140000000")
            return Long.parseLong(text.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return currentPrice + stepPrice; // Mặc định nếu lỗi nhập liệu
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalTime currentTime = LocalTime.now();
            currentTimeLabel.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
    public void syncNewPrice(double newPrice, String bidderFullName) {
        this.currentPrice = newPrice;
        this.stepPrice = newPrice * 0.06;
        this.suggestedPrice = newPrice + stepPrice;
        currentPriceLabel.setText(formatter.format(newPrice) + " VNĐ");

        String displayName = getDisplayName(bidderFullName, participantNames);
        topBidderLabel.setText("Người dẫn đầu: " + displayName);
        updateBidAmountField(suggestedPrice);
    }

    public void addBidHistory(String bidderFullName, double price) {
        if (!participantNames.contains(bidderFullName)) {
            participantNames.add(bidderFullName);
        }
        String displayName = getDisplayName(bidderFullName, participantNames);
        String entry = displayName + ": " + formatter.format(price) + " VNĐ (vừa xong)";
        bidHistoryList.add(0, entry);
    }

    private void loadDummyBidHistory() {
        bidHistoryList.addAll(
                "Nguyễn Văn T. : 135,000,000 VND (1 phút trước)",
                "Trần Thị L. : 130,000,000 VND (2 phút trước)",
                "Lê Văn B. : 125,000,000 VND (5 phút trước)"
        );
        bidHistoryListView.setItems(bidHistoryList);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait(); // Có ở HandleNavigationAndAlert rồi
    }

    /**
     * Rút gọn tên hiển thị: dùng chữ cái đầu họ + tên cuối.
     * Nếu trùng với người khác trong danh sách, thêm tên đệm.
     * VD: "Hồ Văn Trung" → "H Trung", nếu trùng → "H Văn Trung"
     */
    private String getDisplayName(String fullName, List<String> otherNames) {
        if (fullName == null || fullName.isBlank()) return fullName;
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];

        String lastName = parts[0];
        String firstName = parts[parts.length - 1];
        String shortName = lastName.substring(0, 1) + " " + firstName;

        // Kiểm tra có trùng với ai trong danh sách không
        boolean hasDuplicate = otherNames.stream()
            .anyMatch(other -> {
                String[] otherParts = other.trim().split("\\s+");
                if (otherParts.length < 2) return false;
                String otherFirst = otherParts[otherParts.length - 1];
                String otherLastInitial = otherParts[0].substring(0, 1);
                return otherFirst.equals(firstName) && otherLastInitial.equals(lastName.substring(0, 1));
            });

        if (hasDuplicate && parts.length >= 3) {
            // Thêm tên đệm: "H Văn Trung"
            String middleName = parts[parts.length - 2];
            return lastName.substring(0, 1) + " " + middleName + " " + firstName;
        }
        return shortName;
    }

    private final List<String> participantNames = new java.util.ArrayList<>();

    public void disableBidding() {
        if (placeBidButton != null) placeBidButton.setDisable(true);
        if (increaseBidButton != null) increaseBidButton.setDisable(true);
        if (decreaseBidButton != null) decreaseBidButton.setDisable(true);
        if (bidAmountField != null) bidAmountField.setEditable(false);
    }

    public void navigateToHome() {
        onClose();
        // Cần một Event giả hoặc dùng Stage trực tiếp
        javafx.stage.Stage stage = (javafx.stage.Stage) placeBidButton.getScene().getWindow();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/Home.fxml"));
            javafx.scene.layout.StackPane root = loader.load();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}