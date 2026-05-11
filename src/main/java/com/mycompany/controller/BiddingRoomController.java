package com.mycompany.controller;

import com.mycompany.server.controller.AuctionWebSocketControllerAdapter;
import com.mycompany.server.websocket.AuctionWebSocketClient;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    // --- CÁC BIẾN LOGIC ---
    private long currentPrice = 135000000; // Giá cao nhất hiện tại
    private long stepPrice = 5000000; // Bước giá (mỗi lần +/- 5 triệu)
    private DecimalFormat formatter = new DecimalFormat("#,###"); // Định dạng tiền tệ kiểu 135,000,000
    private ObservableList<String> bidHistoryList = FXCollections.observableArrayList();

    /**
     * Hàm này tự động chạy khi giao diện FXML được load
     */
    // Thêm vào BiddingRoomController.java

    private AuctionWebSocketClient wsClient;
    private String currentPhienId = "PH000001"; // nhận từ màn hình trước

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startClock();
        loadDummyBidHistory();
        setupButtonActions();
        updateBidAmountField(currentPrice + stepPrice);
        connectWebSocket(); // THÊM
    }

    private void connectWebSocket() {
        try {
            wsClient = AuctionWebSocketClient.getInstance();

            // Tạo adapter để nhận events
            AuctionWebSocketControllerAdapter adapter =
                    new AuctionWebSocketControllerAdapter(this, currentPriceLabel);
            wsClient.setListener(adapter);

            // Kết nối (chạy trên background thread để không block UI)
            new Thread(() -> {
                wsClient.connect();
                // Join phòng sau khi connected
                String email = SessionManager.getInstance()
                        .getCurrentUser().layThuDienTu();
                wsClient.sendJoin(currentPhienId, email);
            }).start();

        } catch (Exception e) {
            System.err.println("Lỗi kết nối WebSocket: " + e.getMessage());
        }
    }

    // Sửa handlePlaceBid() để gửi qua WebSocket thay vì local
    private void handlePlaceBid() {
        long myBid = parseBidAmount(bidAmountField.getText());
        if (myBid <= currentPrice) {
            showAlert("Lỗi đặt giá", "Giá phải lớn hơn giá hiện tại!");
            return;
        }

        // Gửi qua WebSocket thay vì cập nhật local
        if (wsClient != null && wsClient.isConnected()) {
            String email = SessionManager.getInstance()
                    .getCurrentUser().layThuDienTu();
            wsClient.sendBid(currentPhienId, email, myBid);
        } else {
            // Fallback: gọi REST API
            String token = SessionManager.getInstance().getCurrentToken();
            ApiClient.createBid(currentPhienId, myBid, token);
        }
    }

    // Getter để AuctionWebSocketControllerAdapter có thể access
    public Label getCurrentPriceLabel() { return currentPriceLabel; }

    private void setupButtonActions() {
        // Nút Giảm giá
        decreaseBidButton.setOnAction(event -> {
            long currentInput = parseBidAmount(bidAmountField.getText());
            if (currentInput - stepPrice > currentPrice) {
                updateBidAmountField(currentInput - stepPrice);
            } else {
                showAlert("Thông báo", "Mức giá đặt phải cao hơn giá hiện tại!");
            }
        });

        // Nút Tăng giá
        increaseBidButton.setOnAction(event -> {
            long currentInput = parseBidAmount(bidAmountField.getText());
            updateBidAmountField(currentInput + stepPrice);
        });

        // Nút Đặt giá ngay
        placeBidButton.setOnAction(event -> {
            handlePlaceBid();
        });

        // Nút Đóng cửa sổ
        closeButton.setOnAction(event -> {
            // Logic đóng cửa sổ ở đây (Ví dụ: return home)
            System.out.println("Đóng phòng đấu giá...");
        });
    }

    // --- CÁC HÀM TIỆN ÍCH CHUẨN HOÁ DỮ LIỆU ---

    /**
     * Dùng để nhận dữ liệu từ màn hình khác truyền sang
     */
    public void setAuctionData(Object modelPhiênĐấuGiá) {
        // Giả sử lấy tên từ model của bạn
        // String name = modelPhiênĐấuGiá.getName();
        // auctionNameLabel.setText("PHÒNG ĐẤU GIÁ: " + name.toUpperCase());
    }

    private void updateBidAmountField(long amount) {
        bidAmountField.setText(formatter.format(amount));
    }

    private long parseBidAmount(String text) {
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
        alert.showAndWait();
    }
}