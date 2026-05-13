package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import com.mycompany.server.dto.PhienDauGiaDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private ListView<String> listViewPhien;
    @FXML private Label labelWelcome;

    private List<PhienDauGiaDTO> danhSachPhien;
    private final DecimalFormat fmt = new DecimalFormat("#,###");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Hiển thị chào mừng
        if (labelWelcome != null && SessionManager.getInstance().getCurrentUser() != null) {
            labelWelcome.setText("Xin chào, " +
                    SessionManager.getInstance().getCurrentUser().layHoTen() + "!");
        }

        // Load danh sách phiên từ server (chạy trên thread riêng, không block UI)
        taiDanhSachPhien();

        // Bắt sự kiện double-click vào phiên
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
                ObservableList<String> items = FXCollections.observableArrayList();
                for (PhienDauGiaDTO p : list) {
                    String trangThai = p.trangThai != null ? p.trangThai : "N/A";
                    String tenSanPham = p.tenSanPham != null ? p.tenSanPham : "?";
                    items.add(String.format("[%s] %s — %s VNĐ — %s",
                            trangThai, p.tenPhien,
                            fmt.format(p.giaHienTai), tenSanPham));
                }
                if (listViewPhien != null) {
                    listViewPhien.setItems(items);
                }
            });
        }).start();
    }

    private void vaoPhongDauGia(MouseEvent event) {
        int idx = listViewPhien.getSelectionModel().getSelectedIndex();
        if (idx < 0 || danhSachPhien == null || idx >= danhSachPhien.size()) return;

        PhienDauGiaDTO phien = danhSachPhien.get(idx);

        // Kiểm tra phiên đang diễn ra
        if (!"DANG_DIEN_RA".equals(phien.trangThai)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.WARNING, "Thông báo",
                    "Phiên này chưa bắt đầu hoặc đã kết thúc (trạng thái: " + phien.trangThai + ")");
            return;
        }

        // Lưu mã phiên vào session để BiddingRoomController lấy ra
        SessionManager.getInstance().setCurrentPhienId(phien.maPhien);
        try {
            HandleNavigationAndAlert.getInstance().goToBiddingRoom(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi", "Không thể mở phòng đấu giá!");
        }
    }
}