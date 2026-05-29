package com.mycompany.controller;

import com.mycompany.local.HiddenAuctionRepository;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.ModernAlert;
import com.mycompany.utils.SessionManager;
import com.mycompany.server.dto.PhienDauGiaDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class HomeController implements Initializable {

    // ── FXML refs ──────────────────────────────────────────────────────────
    @FXML private FlowPane flowPanePhien;
    @FXML private Label    labelWelcome;

    // ── State ──────────────────────────────────────────────────────────────
    private final DecimalFormat fmt = new DecimalFormat("#,###");
    private Timeline autoRefreshTimeline;
    private Timeline countdownTimeline;
    private final HiddenAuctionRepository hiddenRepo = new HiddenAuctionRepository();
    private final Set<String> watchedSessions = new HashSet<>();

    // Danh sách (Label, endTimeStr) để cập nhật mỗi giây
    private final List<CountdownEntry> countdownEntries = new ArrayList<>();

    private static final DateTimeFormatter[] PARSE_FMT = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    };
    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Inner record ───────────────────────────────────────────────────────
    private static class CountdownEntry {
        final Label  label;
        final String endTimeStr;
        CountdownEntry(Label label, String endTimeStr) {
            this.label = label; this.endTimeStr = endTimeStr;
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (labelWelcome != null && SessionManager.getInstance().getCurrentUser() != null) {
            labelWelcome.setText("Xin chào, " +
                SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }

        startData();
        startCountdown();

        // Dừng timelines khi rời trang
        flowPanePhien.sceneProperty().addListener((obs, o, n) -> {
            if (n == null) stopAll();
        });
    }

    // ── Timelines ──────────────────────────────────────────────────────────

    private void startData() {
        doRefresh();
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> doRefresh())
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                for (CountdownEntry ce : countdownEntries) {
                    refreshCountdownLabel(ce.label, ce.endTimeStr);
                }
            })
        );
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    public void stopAll() {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        if (countdownTimeline  != null) countdownTimeline.stop();
    }
    /** Alias giữ tương thích với code cũ. */
    public void stopAutoRefresh() { stopAll(); }

    // ── Data refresh ───────────────────────────────────────────────────────

    private void doRefresh() {
        new Thread(() -> {
            List<PhienDauGiaDTO> all = ApiClient.getAuctions();
            String uid = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getUserId() : null;
            Set<String> hidden = hiddenRepo.findHiddenAuctionIds(uid);

            List<PhienDauGiaDTO> filtered = new ArrayList<>();
            for (PhienDauGiaDTO d : all) {
                if (hidden.contains(d.maPhien)) continue;
                if ("WAITING".equals(d.trangThai) || "IN_PROGRESS".equals(d.trangThai)
                    || "PAID".equals(d.trangThai)) {
                    filtered.add(d);
                }
            }
            Platform.runLater(() -> rebuildCards(filtered));
        }).start();
    }

    private void rebuildCards(List<PhienDauGiaDTO> list) {
        countdownEntries.clear();
        flowPanePhien.getChildren().clear();
        for (PhienDauGiaDTO p : list) {
            flowPanePhien.getChildren().add(createCard(p));
        }
    }

    // ── Card builder ───────────────────────────────────────────────────────

    private VBox createCard(PhienDauGiaDTO p) {
        String tt = p.trangThai != null ? p.trangThai : "";

        // ── Khung card ───────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(274);
        card.setMaxWidth(274);
        final String styleNormal =
            "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.11);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.38),15,0,0,5);" +
                "-fx-cursor: hand;";
        final String styleHover =
            "-fx-background-color: rgba(255,255,255,0.09);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(238,116,85,0.45);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.55),22,0,0,8);" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.012; -fx-scale-y: 1.012;";
        card.setStyle(styleNormal);

        // ── Vùng ảnh ─────────────────────────────────────────────────
        StackPane imgArea = new StackPane();
        imgArea.setPrefSize(274, 152);
        imgArea.setStyle(
            "-fx-background-color: " + categoryGradient(p.danhMucSanPham) + ";" +
                "-fx-background-radius: 16 16 0 0;"
        );

        // Clip bo góc trên ảnh
        Rectangle clip = new Rectangle(274, 152);
        clip.setArcWidth(32); clip.setArcHeight(32);
        imgArea.setClip(clip);

        // Load ảnh nếu có
        loadProductImage(imgArea, p.productImgPath);

        // Gradient overlay phía dưới ảnh
        Region grad = new Region();
        grad.setPrefSize(274, 68);
        grad.setStyle("-fx-background-color: linear-gradient(to bottom,transparent,rgba(15,22,60,0.82));");
        StackPane.setAlignment(grad, Pos.BOTTOM_CENTER);
        imgArea.getChildren().add(grad);

        // Badge trạng thái (top-left)
        Label badge = makeBadge(tt);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        imgArea.getChildren().add(badge);

        // Nút action nhỏ (top-right: ⭐ xem trước / 🗑 xoá)
        HBox iconBtns = new HBox(5);
        StackPane.setAlignment(iconBtns, Pos.TOP_RIGHT);
        StackPane.setMargin(iconBtns, new Insets(8, 8, 0, 0));

        if ("WAITING".equals(tt) || "IN_PROGRESS".equals(tt)) {
            boolean isWatched = watchedSessions.contains(p.maPhien);
            Button btnStar = makeIconBtn(isWatched ? "👁" : "⭐",
                isWatched ? "#e74c3c" : "#f1c40f");
            btnStar.setOnAction(e -> {
                e.consume();
                if (watchedSessions.contains(p.maPhien)) watchedSessions.remove(p.maPhien);
                else { watchedSessions.add(p.maPhien); showWatchInfo(p.tenPhien, p.thoiGianBatDau); }
                doRefresh();
            });
            iconBtns.getChildren().add(btnStar);
        }

        if ("PAID".equals(tt) || "CANCELLED".equals(tt) || "FINISHED".equals(tt)) {
            Button btnDel = makeIconBtn("🗑", "#e74c3c");
            btnDel.setOnAction(e -> {
                e.consume();
                javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận ẩn");
                confirm.setHeaderText("Ẩn phiên: " + p.tenPhien);
                confirm.setContentText("Phiên chỉ bị ẩn khỏi danh sách của bạn, dữ liệu server không đổi.");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    String uid = SessionManager.getInstance().getCurrentUser() != null
                        ? SessionManager.getInstance().getCurrentUser().getUserId() : null;
                    hiddenRepo.hideAuction(uid, p.maPhien);
                    doRefresh();
                }
            });
            iconBtns.getChildren().add(btnDel);
        }

        imgArea.getChildren().add(iconBtns);

        // ── Phần nội dung bên dưới ảnh ───────────────────────────────
        VBox content = new VBox(9);
        content.setPadding(new Insets(14, 14, 14, 14));

        // Tên sản phẩm
        Label lblSP = new Label(p.tenSanPham != null ? p.tenSanPham : "—");
        lblSP.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblSP.setStyle("-fx-text-fill: #ffffff;");
        lblSP.setWrapText(true);
        lblSP.setMaxWidth(246);
        lblSP.setMaxHeight(38);

        // Tên phiên (nhỏ hơn, mờ hơn)
        Label lblPhien = new Label(p.tenPhien != null ? p.tenPhien : "");
        lblPhien.setFont(Font.font(11));
        lblPhien.setStyle("-fx-text-fill: #6b7db3;");
        lblPhien.setMaxWidth(246);

        // Hàng giá + đếm ngược
        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        Label lblGia = new Label(fmt.format(p.giaHienTai) + " VNĐ");
        lblGia.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblGia.setStyle("-fx-text-fill: #EE7455;");
        HBox.setHgrow(lblGia, Priority.ALWAYS);

        // Đếm ngược: IN_PROGRESS → còn lại đến khi kết thúc / WAITING → đến khi bắt đầu
        String countdownTarget = "IN_PROGRESS".equals(tt) ? p.thoiGianKetThuc : p.thoiGianBatDau;
        Label lblTimer = new Label("--:--:--");
        lblTimer.setFont(Font.font(10.5));
        lblTimer.setStyle(
            "-fx-text-fill: #f9a875;" +
                "-fx-background-color: rgba(249,168,117,0.14);" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 2 7 2 7;"
        );
        refreshCountdownLabel(lblTimer, countdownTarget);
        countdownEntries.add(new CountdownEntry(lblTimer, countdownTarget));
        priceRow.getChildren().addAll(lblGia, lblTimer);

        // Nút "Đặt giá"
        boolean isOpen = "IN_PROGRESS".equals(tt);
        final String btnActive =
            "-fx-background-color: #EE7455;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-weight: bold; -fx-font-size: 12px;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(238,116,85,0.38),10,0,0,3);";
        final String btnInactive =
            "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-text-fill: #4a5478;" +
                "-fx-font-weight: bold; -fx-font-size: 12px;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: default;";
        final String btnActiveHover =
            "-fx-background-color: #d96045;" +
                "-fx-text-fill: #fff; -fx-font-weight: bold; -fx-font-size: 12px;" +
                "-fx-background-radius: 10; -fx-cursor: hand;" +
                "-fx-scale-x: 1.02; -fx-scale-y: 1.02;";

        Button btnDat = new Button("ĐẶT GIÁ");
        btnDat.setPrefWidth(246);
        btnDat.setPrefHeight(40);
        btnDat.setStyle(isOpen ? btnActive : btnInactive);

        if (isOpen) {
            btnDat.setOnMouseEntered(e -> btnDat.setStyle(btnActiveHover));
            btnDat.setOnMouseExited(e -> btnDat.setStyle(btnActive));
            btnDat.setOnAction(e -> { e.consume(); enterRoom(p, btnDat); });
        }

        content.getChildren().addAll(lblSP, lblPhien, priceRow, btnDat);
        card.getChildren().addAll(imgArea, content);

        // ── Sự kiện click toàn card ───────────────────────────────────
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e -> card.setStyle(styleNormal));
        card.setOnMouseClicked(e -> {
            switch (tt) {
                case "IN_PROGRESS" -> enterRoom(p, card);
                case "WAITING"     -> ModernAlert.show(Alert.AlertType.INFORMATION,
                    "Phiên chưa mở",
                    "Phiên đấu giá này chưa đến giờ bắt đầu.\nVui lòng quay lại sau!");
                default -> ModernAlert.show(Alert.AlertType.WARNING,
                    "Phiên đã đóng",
                    "Phiên đấu giá này đã kết thúc, không thể vào phòng.");
            }
        });

        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Tải ảnh sản phẩm từ thư mục user_data, fallback sang gradient nền. */
    private void loadProductImage(StackPane imgArea, String productImgPath) {
        if (productImgPath == null || productImgPath.isBlank()) return;
        try {
            String projectDir = System.getProperty("user.dir");
            // productImgPath = "image/filename.jpg", thực tế ở user_data/image/filename.jpg
            File imgFile = new File(projectDir + File.separator + "user_data"
                + File.separator + productImgPath);
            if (!imgFile.exists()) {
                // Thử không có prefix user_data
                imgFile = new File(projectDir + File.separator + productImgPath);
            }
            if (imgFile.exists()) {
                Image img = new Image(imgFile.toURI().toString(), 274, 152, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(274);
                iv.setFitHeight(152);
                iv.setPreserveRatio(false);
                imgArea.getChildren().add(0, iv); // add ảnh dưới cùng (dưới gradient)
            }
        } catch (Exception ignored) { /* giữ gradient nền */ }
    }

    /** Chuyển sang phòng đấu giá. */
    private void enterRoom(PhienDauGiaDTO p, javafx.scene.Node source) {
        SessionManager.getInstance().setCurrentPhienId(p.maPhien);
        try {
            StackPane root = FXMLLoader.load(getClass().getResource("/view/BiddingRoom.fxml"));
            Scene scene = new Scene(root);
            Stage window = (Stage) source.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (IOException ex) {
            ModernAlert.show(Alert.AlertType.ERROR, "Lỗi", "Không thể mở phòng đấu giá!");
        }
    }

    /** Tạo badge trạng thái. */
    private Label makeBadge(String tt) {
        String text, bg, fg;
        switch (tt) {
            case "IN_PROGRESS" -> { text = "● Đang diễn ra"; bg = "rgba(46,204,113,0.22)"; fg = "#2ecc71"; }
            case "WAITING"     -> { text = "◷ Đang chờ";     bg = "rgba(241,196,15,0.22)"; fg = "#f1c40f"; }
            case "PAID"        -> { text = "✔ Đã thanh toán";bg = "rgba(52,152,219,0.22)"; fg = "#3498db"; }
            case "CANCELLED"   -> { text = "✕ Đã hủy";       bg = "rgba(231,76,60,0.22)";  fg = "#e74c3c"; }
            case "FINISHED"    -> { text = "✕ Đã kết thúc";  bg = "rgba(231,76,60,0.22)";  fg = "#e74c3c"; }
            default            -> { text = tt;                bg = "rgba(255,255,255,0.1)"; fg = "#cccccc"; }
        }
        Label b = new Label(text);
        b.setFont(Font.font("System", FontWeight.BOLD, 10));
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: " + fg + ";" +
                "-fx-border-radius: 6;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 3 8 3 8;"
        );
        return b;
    }

    /** Tạo nút icon nhỏ overlay trên ảnh. */
    private Button makeIconBtn(String icon, String color) {
        Button btn = new Button(icon);
        btn.setStyle(
            "-fx-background-color: rgba(0,0,0,0.48);" +
                "-fx-text-fill: " + color + ";" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 13;"
        );
        return btn;
    }

    /** Gradient nền theo danh mục. */
    private String categoryGradient(String cat) {
        if (cat == null) return "linear-gradient(to bottom right,#2c3e6b,#1a254b)";
        return switch (cat) {
            case "Đá quý"     -> "linear-gradient(to bottom right,#4a1c6b,#2a1050)";
            case "Nghệ thuật" -> "linear-gradient(to bottom right,#1a4a6b,#0d2840)";
            case "Xe cộ"      -> "linear-gradient(to bottom right,#0d3b6e,#0a1f3c)";
            case "Điện tử"    -> "linear-gradient(to bottom right,#0d506e,#0a2840)";
            default            -> "linear-gradient(to bottom right,#2c3e6b,#1a254b)";
        };
    }

    /** Cập nhật nhãn đếm ngược. */
    private void refreshCountdownLabel(Label label, String endTimeStr) {
        LocalDateTime end = parseDateTime(endTimeStr);
        if (end == null) { label.setText("--:--:--"); return; }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(end)) { label.setText("00:00:00"); return; }
        long secs = ChronoUnit.SECONDS.between(now, end);
        label.setText(String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60));
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (DateTimeFormatter f : PARSE_FMT) {
            try { return LocalDateTime.parse(raw, f); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private void showWatchInfo(String tenPhien, String thoiGianBD) {
        String info;
        LocalDateTime bd = parseDateTime(thoiGianBD);
        if (bd != null) {
            long mins = ChronoUnit.MINUTES.between(LocalDateTime.now(), bd);
            long hrs  = ChronoUnit.HOURS.between(LocalDateTime.now(), bd);
            info = mins <= 0 ? "Phiên sắp bắt đầu ngay bây giờ!"
                : hrs < 1   ? "Còn khoảng " + mins + " phút nữa."
                : "Còn khoảng " + hrs + " giờ " + (mins % 60) + " phút nữa.\n"
                + "Bắt đầu: " + bd.format(DISPLAY_FMT);
        } else {
            info = "Thời gian bắt đầu: " + thoiGianBD;
        }
        ModernAlert.show(Alert.AlertType.INFORMATION,
            "⭐ Theo dõi: " + tenPhien,
            info + "\n\nBạn sẽ được nhắc khi phiên bắt đầu!");
    }
}