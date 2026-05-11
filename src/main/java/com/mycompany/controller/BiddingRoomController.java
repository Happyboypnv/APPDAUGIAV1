package com.mycompany.controller;

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
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Khởi tạo đồng hồ thời gian thực
        startClock();

        // 2. Load dữ liệu giả lập cho Lịch sử đấu giá
        loadDummyBidHistory();

        // 3. Gán sự kiện cho các nút bấm
        setupButtonActions();

        // 4. Định dạng ô nhập giá ban đầu (Giá hiện tại + 1 bước giá)
        updateBidAmountField(currentPrice + stepPrice);
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---

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

    private void handlePlaceBid() {
        long myBid = parseBidAmount(bidAmountField.getText());

        if (myBid <= currentPrice) {
            showAlert("Lỗi đặt giá", "Giá bạn đặt (" + formatter.format(myBid) + ") phải lớn hơn giá hiện tại!");
            return;
        }

        // Cập nhật giá hiện tại mới
        currentPrice = myBid;
        currentPriceLabel.setText(formatter.format(currentPrice) + " VND");
        topBidderLabel.setText("Người giữ giá cao nhất: Bạn (Tôi)");

        // Cập nhật vào danh sách lịch sử
        String newHistoryRecord = "Bạn : " + formatter.format(myBid) + " VND (Vừa xong)";
        bidHistoryList.add(0, newHistoryRecord); // Thêm lên đầu danh sách

        // Chuẩn bị ô nhập giá cho lượt tiếp theo
        updateBidAmountField(currentPrice + stepPrice);

        showAlert("Thành công", "Bạn đã đặt giá thành công: " + formatter.format(myBid) + " VND");
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