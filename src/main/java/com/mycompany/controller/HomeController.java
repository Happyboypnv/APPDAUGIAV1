package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.SessionStatus;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import com.mycompany.server.dto.PhienDauGiaDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private ListView<PhienDauGiaDTO> listViewPhien;
    @FXML private Label labelWelcome;

    private List<PhienDauGiaDTO> danhSachPhien;
    private final DecimalFormat fmt = new DecimalFormat("#,###");
    private Timeline autoRefreshTimeline;
    // Set lưu mã phiên user đang theo dõi
    private final Set<String> watchedSessions = new HashSet<>();

    // Formatter để parse chuỗi từ server (ISO hoặc "yyyy-MM-dd HH:mm:ss")
    private static final DateTimeFormatter[] PARSE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    };
    // Formatter để hiển thị ra UI
    private static final DateTimeFormatter DISPLAY_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (labelWelcome != null && SessionManager.getInstance().getCurrentUser() != null) {
            labelWelcome.setText("Xin chào, " +
                    SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }

        // Gán custom cell factory trước khi load dữ liệu
        listViewPhien.setCellFactory(lv -> new PhienDauGiaCell());

        taiDanhSachPhien();

        listViewPhien.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                vaoPhongDauGia(event);
            }
        });

        // Tự động dừng Timeline khi scene này bị thay thế (rời Home)
        listViewPhien.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }
        });
    }

    private void taiDanhSachPhien() {
        // Load lần đầu ngay lập tức
        doRefresh();

        // Auto-refresh mỗi 5 giây
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> doRefresh())
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void doRefresh() {
        new Thread(() -> {
            List<PhienDauGiaDTO> list = ApiClient.getAuctions();
            Platform.runLater(() -> {
                danhSachPhien = list;
                // Giữ vị trí scroll hiện tại
                int selectedIndex = listViewPhien.getSelectionModel().getSelectedIndex();
                listViewPhien.setItems(FXCollections.observableArrayList(list));
                if (selectedIndex >= 0 && selectedIndex < list.size()) {
                    listViewPhien.getSelectionModel().select(selectedIndex);
                }
            });
        }).start();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private void vaoPhongDauGia(MouseEvent event) {
        PhienDauGiaDTO phien = listViewPhien.getSelectionModel().getSelectedItem();
        if (phien == null) return;

        String tt = phien.trangThai != null ? phien.trangThai : "";
        switch (tt) {
            case "IN_PROGRESS":
                break; // cho vào bình thường
            case "WAITING":
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.INFORMATION, "Phiên chưa mở",
                    "Phiên đấu giá này chưa đến giờ bắt đầu. Vui lòng quay lại sau!");
                return;
            case "PAID":
            case "CANCELLED":
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.WARNING, "Phiên đã đóng",
                    "Phiên đấu giá này đã kết thúc, không thể vào phòng.");
                return;
            default:
                HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.WARNING, "Không thể vào phòng",
                    "Trạng thái phiên không hợp lệ: " + tt);
                return;
        }

        SessionManager.getInstance().setCurrentPhienId(phien.maPhien);
        try {
            HandleNavigationAndAlert.getInstance().goToBiddingRoom(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi", "Không thể mở phòng đấu giá!");
        }
    }

    /** Format chuỗi thời gian từ server thành dạng dd/MM/yyyy HH:mm */
    private String formatThoiGian(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        for (DateTimeFormatter f : PARSE_FORMATTERS) {
            try {
                return LocalDateTime.parse(raw, f).format(DISPLAY_FORMATTER);
            } catch (DateTimeParseException ignored) {}
        }
        // Nếu không parse được, trả về chuỗi gốc (cắt bớt nếu quá dài)
        return raw.length() > 16 ? raw.substring(0, 16) : raw;
    }

    // ─── Custom Cell ────────────────────────────────────────────────────────────

    private class PhienDauGiaCell extends ListCell<PhienDauGiaDTO> {

        private final HBox root;
        private final Label lblTrangThai;
        private final Label lblTenPhien;
        private final Label lblSanPham;
        private final Label lblGia;
        private final Label lblBatDau;
        private final Label lblKetThuc;
        private final Button btnWatch;
        private final Button btnXoa;

        PhienDauGiaCell() {
            // Badge trạng thái (bên trái)
            lblTrangThai = new Label();
            lblTrangThai.setPrefWidth(110);
            lblTrangThai.setAlignment(Pos.CENTER);
            lblTrangThai.setPadding(new Insets(4, 8, 4, 8));
            lblTrangThai.setFont(Font.font("System Bold", 11));
            lblTrangThai.setWrapText(false);

            // Cột giữa: tên phiên + sản phẩm
            lblTenPhien = new Label();
            lblTenPhien.setFont(Font.font("System Bold", 14));
            lblTenPhien.setTextFill(javafx.scene.paint.Color.WHITE);

            lblSanPham = new Label();
            lblSanPham.setFont(Font.font(12));
            lblSanPham.setTextFill(javafx.scene.paint.Color.web("#aab4d4"));

            VBox colInfo = new VBox(3, lblTenPhien, lblSanPham);
            colInfo.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(colInfo, Priority.ALWAYS);

            // Cột thời gian
            Label lblBatDauHeader = makeHeader("🕐 Bắt đầu");
            lblBatDau = new Label();
            lblBatDau.setFont(Font.font(12));
            lblBatDau.setTextFill(javafx.scene.paint.Color.web("#7ec8e3"));

            Label lblKetThucHeader = makeHeader("🏁 Kết thúc");
            lblKetThuc = new Label();
            lblKetThuc.setFont(Font.font(12));
            lblKetThuc.setTextFill(javafx.scene.paint.Color.web("#f9a875"));

            VBox colTime = new VBox(3,
                lblBatDauHeader, lblBatDau,
                lblKetThucHeader, lblKetThuc);
            colTime.setAlignment(Pos.CENTER_LEFT);
            colTime.setPrefWidth(160);

            // Cột giá (bên phải)
            Label lblGiaHeader = makeHeader("Giá hiện tại");
            lblGia = new Label();
            lblGia.setFont(Font.font("System Bold", 13));
            lblGia.setTextFill(javafx.scene.paint.Color.web("#2ecc71"));

            VBox colGia = new VBox(3, lblGiaHeader, lblGia);
            colGia.setAlignment(Pos.CENTER_RIGHT);
            colGia.setPrefWidth(140);

            root = new HBox(14, lblTrangThai, colInfo, colTime, colGia);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 14));
            root.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.1);" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
            );

            btnWatch = new Button("⭐ Chú ý");
            btnWatch.setPrefWidth(100);
            btnWatch.setStyle(
                "-fx-background-color: rgba(241,196,15,0.2);" +
                    "-fx-text-fill: #f1c40f;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: #f1c40f;" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-cursor: hand;"
            );

            btnXoa = new Button("🗑 Xóa");
            btnXoa.setPrefWidth(85);
            btnXoa.setStyle(
                "-fx-background-color: rgba(231,76,60,0.2);" +
                    "-fx-text-fill: #e74c3c;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: #e74c3c;" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-cursor: hand;"
            );
        }

        private Label makeHeader(String text) {
            Label l = new Label(text);
            l.setFont(Font.font(10));
            l.setTextFill(javafx.scene.paint.Color.web("#6b7db3"));
            return l;
        }

        @Override
        protected void updateItem(PhienDauGiaDTO p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            // Tên phiên & sản phẩm
            lblTenPhien.setText(p.tenPhien != null ? p.tenPhien : "—");
            lblSanPham.setText("Sản phẩm: " + (p.tenSanPham != null ? p.tenSanPham : "?"));

            // Thời gian
            lblBatDau.setText(formatThoiGian(p.thoiGianBatDau));
            lblKetThuc.setText(formatThoiGian(p.thoiGianKetThuc));

            // Giá
            lblGia.setText(fmt.format(p.giaHienTai) + " VNĐ");

            // Badge trạng thái với màu sắc tương ứng
            String tt = p.trangThai != null ? p.trangThai : "N/A";
            String mauNen, mauChu, labelText;
            switch (tt) {
                case "IN_PROGRESS":
                    mauNen = "rgba(46,204,113,0.25)";
                    mauChu = "#2ecc71";
                    labelText = "● Đang diễn ra";
                    break;
                case "WAITING":
                    mauNen = "rgba(241,196,15,0.25)";   // vàng
                    mauChu = "#f1c40f";
                    labelText = "◷ Đang chờ";
                    break;
                case "PAID":
                    mauNen = "rgba(231,76,60,0.25)";    // đỏ
                    mauChu = "#e74c3c";
                    labelText = "✕ Đã đóng";
                    break;
                case "CANCELLED":
                    mauNen = "rgba(231,76,60,0.25)";    // đỏ (cùng màu PAID)
                    mauChu = "#e74c3c";
                    labelText = "✕ Đã hủy";
                    break;
                default:
                    mauNen = "rgba(255,255,255,0.1)";
                    mauChu = "#ccc";
                    labelText = tt;
            }
            lblTrangThai.setText(labelText);
            lblTrangThai.setStyle(
                "-fx-background-color: " + mauNen + ";" +
                "-fx-text-fill: " + mauChu + ";" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: " + mauChu + ";" +
                "-fx-border-radius: 6;" +
                "-fx-border-width: 1;"
            );

            // Chỉ hiện nút Chú ý/Bỏ qua cho phiên WAITING hoặc IN_PROGRESS
            if ("WAITING".equals(tt) || "IN_PROGRESS".equals(tt)) {
                boolean isWatched = watchedSessions.contains(p.maPhien);
                btnWatch.setText(isWatched ? "👁 Bỏ qua" : "⭐ Chú ý");
                btnWatch.setStyle(
                    isWatched
                        ? "-fx-background-color: rgba(231,76,60,0.2);-fx-text-fill:#e74c3c;" +
                        "-fx-background-radius:6;-fx-border-color:#e74c3c;-fx-border-radius:6;" +
                        "-fx-border-width:1;-fx-cursor:hand;"
                        : "-fx-background-color: rgba(241,196,15,0.2);-fx-text-fill:#f1c40f;" +
                        "-fx-background-radius:6;-fx-border-color:#f1c40f;-fx-border-radius:6;" +
                        "-fx-border-width:1;-fx-cursor:hand;"
                );

                final String maPhien = p.maPhien;
                final String thoiGianBD = p.thoiGianBatDau;
                final String tenPhien = p.tenPhien;
                btnWatch.setOnAction(e -> {
                    e.consume(); // ngăn sự kiện nổi bọt lên ListView
                    if (watchedSessions.contains(maPhien)) {
                        watchedSessions.remove(maPhien);
                    } else {
                        watchedSessions.add(maPhien);
                        showCountdownInfo(tenPhien, thoiGianBD);
                    }
                    // Refresh cell
                    listViewPhien.refresh();
                });

                // Thêm nút vào root nếu chưa có
                if (!root.getChildren().contains(btnWatch)) {
                    root.getChildren().add(btnWatch);
                }
            } else {
                root.getChildren().remove(btnWatch);
            }

            // Hiện nút Xóa chỉ cho PAID và CANCELLED
            if ("PAID".equals(tt) || "CANCELLED".equals(tt)) {
                final String maPhienToDelete = p.maPhien;
                final String tenPhienToDelete = p.tenPhien;
                btnXoa.setOnAction(e -> {
                    e.consume();
                    javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Xác nhận xóa");
                    confirm.setHeaderText("Xóa phiên: " + tenPhienToDelete);
                    confirm.setContentText("Bạn có chắc muốn xóa phiên này không? Hành động không thể hoàn tác.");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String token = SessionManager.getInstance().getServerToken();
                        new Thread(() -> {
                            boolean ok = ApiClient.deleteAuction(maPhienToDelete, token);
                            Platform.runLater(() -> {
                                if (ok) {
                                    HandleNavigationAndAlert.getInstance().showAlert(
                                        Alert.AlertType.INFORMATION, "Thành công", "Đã xóa phiên!");
                                    doRefresh(); // Refresh danh sách
                                } else {
                                    HandleNavigationAndAlert.getInstance().showAlert(
                                        Alert.AlertType.ERROR, "Lỗi", "Không thể xóa phiên. Bạn có phải người tạo phiên không?");
                                }
                            });
                        }).start();
                    }
                });
                if (!root.getChildren().contains(btnXoa)) {
                    root.getChildren().add(btnXoa);
                }
            } else {
                root.getChildren().remove(btnXoa);
            }

            setGraphic(root);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
        }
    }

    private void showCountdownInfo(String tenPhien, String thoiGianBatDau) {
        String timeInfo;
        try {
            LocalDateTime batDau = null;
            for (DateTimeFormatter f : PARSE_FORMATTERS) {
                try { batDau = LocalDateTime.parse(thoiGianBatDau, f); break; }
                catch (DateTimeParseException ignored) {}
            }
            if (batDau != null) {
                LocalDateTime now = LocalDateTime.now();
                long minutes = ChronoUnit.MINUTES.between(now, batDau);
                long hours   = ChronoUnit.HOURS.between(now, batDau);
                if (minutes <= 0) {
                    timeInfo = "Phiên sắp bắt đầu ngay bây giờ!";
                } else if (hours < 1) {
                    timeInfo = "Còn khoảng " + minutes + " phút nữa bắt đầu.";
                } else {
                    timeInfo = "Còn khoảng " + hours + " giờ " + (minutes % 60) + " phút nữa bắt đầu.\n"
                        + "Thời gian bắt đầu: " + batDau.format(DISPLAY_FORMATTER);
                }
            } else {
                timeInfo = "Thời gian bắt đầu: " + thoiGianBatDau;
            }
        } catch (Exception e) {
            timeInfo = "Thời gian bắt đầu: " + thoiGianBatDau;
        }

        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.INFORMATION,
            "⭐ Đang theo dõi: " + tenPhien,
            timeInfo + "\n\nBạn đang theo dõi phiên này. Quay lại trước giờ bắt đầu để tham gia!"
        );
    }
}
