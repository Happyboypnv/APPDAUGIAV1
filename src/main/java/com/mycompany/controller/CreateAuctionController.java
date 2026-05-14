package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

/**
 * CreateAuctionController - Quản lý form tạo phiên đấu giá với xử lý Multi-threading
 */
public class CreateAuctionController implements Initializable {

    // ===== @FXML FIELDS =====
    @FXML private TextField tenPhienField;
    @FXML private TextField tenSanPhamField;
    @FXML private ComboBox<String> danhMucSanPham;
    @FXML private TextArea moTaField;
    @FXML private TextField giaKhoiDiemField;

    @FXML private DatePicker ngayBatDauPicker;
    @FXML private Spinner<Integer> gioBatDauSpinner;
    @FXML private DatePicker ngayKetThucPicker;
    @FXML private Spinner<Integer> gioKetThucSpinner;

    @FXML private StackPane dropArea;
    @FXML private ImageView previewImage;
    @FXML private VBox instructionText;

    private File selectedImageFile = null;

    // ===== INITIALIZE =====
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDanhMuc();
        setupSpinners();
        setupDatePickers();
        setupDragAndDrop();
    }

    private void setupDanhMuc() {
        danhMucSanPham.getItems().addAll(
                "Đồng hồ", "Trang sức", "Nghệ thuật", "Bất động sản",
                "Xe cộ", "Điện tử", "Thời trang", "Khác"
        );
    }

    private void setupSpinners() {
        gioBatDauSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        gioKetThucSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
        gioBatDauSpinner.setEditable(true);
        gioKetThucSpinner.setEditable(true);
    }

    private void setupDatePickers() {
        ngayBatDauPicker.setValue(LocalDate.now());
        ngayKetThucPicker.setValue(LocalDate.now().plusDays(1));
        previewImage.setVisible(false);
    }

    private void setupDragAndDrop() {
        dropArea.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                loadImage(db.getFiles().get(0));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // ===== IMAGE HANDLING =====
    @FXML
    public void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) dropArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadImage(file);
        }
    }

    private void loadImage(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            previewImage.setImage(image);
            previewImage.setVisible(true);
            instructionText.setVisible(false);
            selectedImageFile = file;
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đọc ảnh!");
        }
    }

    // ===== MAIN HANDLER: TẠO PHIÊN (FIXED WITH TASK) =====
    @FXML
    public void handleTaoPhien(ActionEvent event) {
        // 1. Thu thập và chuẩn bị dữ liệu
        String tenPhien = tenPhienField.getText().trim();
        String tenSanPham = tenSanPhamField.getText().trim();
        String danhMuc = danhMucSanPham.getValue();
        String moTa = moTaField.getText().trim();
        String giaStr = giaKhoiDiemField.getText().trim().replace(",", "").replace(".", "");

        LocalDate ngayBD = ngayBatDauPicker.getValue();
        LocalDate ngayKT = ngayKetThucPicker.getValue();
        int gioBD = gioBatDauSpinner.getValue();
        int gioKT = gioKetThucSpinner.getValue();

        // 2. Validate
        String error = validateForm(tenPhien, tenSanPham, danhMuc, giaStr, ngayBD, ngayKT, gioBD, gioKT);
        if (error != null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING, "Thông báo", error);
            return;
        }

        // 3. Chuẩn bị tham số gọi API
        double giaKhoiDiem = Double.parseDouble(giaStr);
        LocalDateTime thoiGianBD = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, 0));
        LocalDateTime thoiGianKT = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, 0));
        long thoiGianGiay = Duration.between(thoiGianBD, thoiGianKT).getSeconds();
        String token = SessionManager.getInstance().getCurrentToken();
        String maSanPham = "SP_" + System.currentTimeMillis();

        if (token == null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
            return;
        }

        // 4. Thực thi Task đa luồng
        Button btn = (Button) event.getSource();
        btn.setDisable(true);
        btn.setText("Đang tạo...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                // Gọi API ở luồng nền
                return ApiClient.createAuction(
                        tenPhien, tenSanPham, maSanPham,
                        danhMuc, moTa,
                        giaKhoiDiem, (int) thoiGianGiay, token
                );
            }
        };

        task.setOnSucceeded(e -> {
            btn.setDisable(false);
            btn.setText("Tạo phiên");
            if (task.getValue()) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.INFORMATION, "Thành công", "Đã tạo phiên đấu giá thành công!");
                try {
                    HandleNavigationAndAlert.getInstance().handleGoToHome(event);
                } catch (IOException ex) { ex.printStackTrace(); }
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi", "Server từ chối yêu cầu!");
            }
        });

        task.setOnFailed(e -> {
            btn.setDisable(false);
            btn.setText("Tạo phiên");
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới Server!");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleHuy(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== VALIDATION LOGIC =====
    private String validateForm(String tenPhien, String tenSanPham, String danhMuc,
                                String giaStr, LocalDate ngayBD, LocalDate ngayKT,
                                int gioBD, int gioKT) {
        if (tenPhien.isEmpty() || tenSanPham.isEmpty()) return "Tên phiên và tên sản phẩm không được để trống!";
        if (danhMuc == null) return "Vui lòng chọn danh mục!";

        try {
            double gia = Double.parseDouble(giaStr);
            if (gia < 1000) return "Giá khởi điểm phải từ 1.000 VNĐ!";
        } catch (NumberFormatException e) {
            return "Giá khởi điểm phải là số!";
        }

        if (ngayBD == null || ngayKT == null) return "Vui lòng chọn đầy đủ ngày bắt đầu/kết thúc!";

        LocalDateTime start = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, 0));
        LocalDateTime end = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, 0));

        if (!end.isAfter(start)) return "Thời gian kết thúc phải sau thời gian bắt đầu!";

        long giay = Duration.between(start, end).getSeconds();
        if (giay < 60) return "Thời gian đấu giá tối thiểu là 1 phút!";

        return null;
    }
}