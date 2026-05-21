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

        if (listViewPhien != null) {
            listViewPhien.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    vaoPhongDauGia(event);
                }
            });
        }
    }

    private void taiDanhSachPhien() {
        new Thread(() -> {
            List<PhienDauGiaDTO> list = ApiClient.getAuctions();
            Platform.runLater(() -> {
                danhSachPhien = list;
                ObservableList<PhienDauGiaDTO> items = FXCollections.observableArrayList(list);
                if (listViewPhien != null) {
                    listViewPhien.setItems(items);
                }
            });
        }).start();
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

            setGraphic(root);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
        }
    }
}
