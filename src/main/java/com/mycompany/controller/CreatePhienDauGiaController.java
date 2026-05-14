package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.SanPham;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * CreatePhienDauGiaController - Controller quản lý tạo phiên đấu giá
 *
 * MỤC ĐÍCH:
 * - Xử lý biểu mẫu tạo phiên đấu giá mới
 * - Thu thập thông tin về sản phẩm và phiên đấu giá
 * - Xác thực dữ liệu đầu vào
 * - Gửi dữ liệu tới máy chủ
 * - Điều hướng người dùng sau khi tạo thành công
 *
 * CHỨC NĂNG:
 * - Chọn loại sản phẩm từ dropdown
 * - Nhập thông tin phiên (tên, thời gian)
 * - Nhập thông tin sản phẩm (tên, mô tả, hình ảnh)
 * - Nhập thông tin giá (giá khởi điểm, bước giá)
 * - Upload hình ảnh sản phẩm
 * - Xác thực và gửi dữ liệu lên server
 */
public class CreatePhienDauGiaController implements Initializable {

    // FXML Fields - Auction Session Info
    @FXML
    private TextField tenPhienField;

    @FXML
    private Spinner<Integer> thoiGianSpinner;

    // FXML Fields - Product Info
    @FXML
    private ComboBox<String> loaiSanPhamCombo;

    @FXML
    private TextField tenSanPhamField;

    @FXML
    private TextArea moTaField;

    @FXML
    private Label tenAenhLabel;

    @FXML
    private Button chonAenhButton;

    // FXML Fields - Pricing Info
    @FXML
    private TextField giaKhoiDiemField;

    @FXML
    private TextField buocGiaField;

    // FXML Fields - Buttons
    @FXML
    private Button taoPhienButton;

    @FXML
    private Button huyCancelButton;

    // Fields to store selected image
    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize Spinner for time duration (minutes)
        spinnerInitialize();

        // Set default image label
        tenAenhLabel.setText("Chưa chọn ảnh");
        selectedImageFile = null;
    }

    /**
     * Initialize Spinner control with default values
     */
    private void spinnerInitialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                5,      // minimum value (5 minutes)
                1440,   // maximum value (24 hours)
                60,     // initial value (60 minutes)
                5       // step increment (5 minutes)
        );
        thoiGianSpinner.setValueFactory(valueFactory);
    }

    /**
     * Handle image selection via FileChooser
     * @param event ActionEvent from button click
     */
    @FXML
    private void chonAnh(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");

        // Set file extension filters
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"
        );
        fileChooser.getExtensionFilters().addAll(imageFilter);

        // Show file chooser dialog
        File file = fileChooser.showOpenDialog(chonAenhButton.getScene().getWindow());

        if (file != null) {
            selectedImageFile = file;
            tenAenhLabel.setText("✓ " + file.getName());
        }
    }

    /**
     * Handle auction creation
     * @param event ActionEvent from create button click
     */
    @FXML
    private void taoPhienDauGia(ActionEvent event) {
        // 1. Validate all input fields
        if (!validateInput()) {
            return;
        }

        try {
            // Get current user
            NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để tạo phiên đấu giá!");
                return;
            }

            // 2. Extract data from form fields
            String tenPhien = tenPhienField.getText().trim();
            String loaiSanPham = loaiSanPhamCombo.getValue();
            String tenSanPham = tenSanPhamField.getText().trim();
            String moTa = moTaField.getText().trim();
            int thoiGian = thoiGianSpinner.getValue();

            // Parse prices
            double giaKhoiDiem = Double.parseDouble(giaKhoiDiemField.getText().trim());
            double buocGia = Double.parseDouble(buocGiaField.getText().trim());

            // 3. Create SanPham object
            String maSanPham = generateProductId(loaiSanPham); // Generate unique product ID
            SanPham sanPham = new SanPham(tenSanPham, maSanPham, giaKhoiDiem);

            // 4. Create PhienDauGia object
            String maPhien = generateAuctionId(); // Generate unique auction ID
            PhienDauGia phien = new PhienDauGia(
                    maPhien, tenPhien, sanPham, giaKhoiDiem, currentUser, thoiGian
            );
            phien.setBuocGia(buocGia);
            phien.setThoiGianBatDau(LocalDateTime.now());

            // 5. Send data to server
            boolean success = sendAuctionToServer(phien, selectedImageFile, moTa, loaiSanPham);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Phiên đấu giá '" + tenPhien + "' đã được tạo thành công!");

                // Navigate back to home
                try {
                    HandleNavigationAndAlert.getInstance().handleGoToHome(event);
                } catch (Exception e) {
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.ERROR, "Lỗi", "Không thể quay về trang chủ!");
                }
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu",
                    "Giá phải là số. Vui lòng kiểm tra lại!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Lỗi khi tạo phiên: " + e.getMessage());
        }
    }

    /**
     * Validate all input fields before submission
     * @return true if all fields are valid, false otherwise
     */
    private boolean validateInput() {
        // Check auction session name
        if (tenPhienField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Vui lòng nhập tên phiên đấu giá!");
            return false;
        }

        // Check product type
        if (loaiSanPhamCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Vui lòng chọn loại sản phẩm!");
            return false;
        }

        // Check product name
        if (tenSanPhamField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Vui lòng nhập tên sản phẩm!");
            return false;
        }

        // Check initial price
        try {
            double gia = Double.parseDouble(giaKhoiDiemField.getText().trim());
            if (gia <= 0) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                        "Giá khởi điểm phải lớn hơn 0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Giá khởi điểm phải là số!");
            return false;
        }

        // Check price step
        try {
            double buoc = Double.parseDouble(buocGiaField.getText().trim());
            if (buoc <= 0) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                        "Bước giá phải lớn hơn 0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Bước giá phải là số!");
            return false;
        }

        // Check duration
        if (thoiGianSpinner.getValue() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Thời gian diễn ra phải lớn hơn 0!");
            return false;
        }

        // Check image selection (optional but recommended)
        if (selectedImageFile == null) {
            // Warning but allow continuation
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận");
            alert.setHeaderText(null);
            alert.setContentText("Bạn chưa chọn ảnh. Tiếp tục tạo phiên?");
            return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        }

        return true;
    }

    /**
     * Send auction data to server
     * @param phien PhienDauGia object to create
     * @param imageFile Image file (can be null)
     * @param moTa Product description
     * @param loaiSanPham Product type
     * @return true if successful, false otherwise
     */
    private boolean sendAuctionToServer(PhienDauGia phien, File imageFile,
                                       String moTa, String loaiSanPham) {
        try {
            // In a real application, this would use ApiClient to send data to server
            // For now, we'll use a placeholder method
            // ApiClient.createAuction(phien, imageFile, moTa);

            // Simulating successful creation
            System.out.println("Phiên đấu giá: " + phien.getMaPhien());
            System.out.println("Sản phẩm: " + phien.getSanPham().layTenSanPham());
            System.out.println("Loại: " + loaiSanPham);
            if (imageFile != null) {
                System.out.println("Ảnh: " + imageFile.getAbsolutePath());
            }
            System.out.println("Mô tả: " + moTa);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate unique auction ID (maPhien)
     * Format: PHIEN_YYYYMMDDHHMMSS_RANDOM
     * @return Generated auction ID
     */
    private String generateAuctionId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return "PH_" + timestamp + "_" + random;
    }

    /**
     * Generate unique product ID based on product type
     * @param loaiSanPham Product type
     * @return Generated product ID
     */
    private String generateProductId(String loaiSanPham) {
        String prefix;
        switch (loaiSanPham) {
            case "Điện Thoại": prefix = "DT"; break;
            case "Máy Tính": prefix = "MT"; break;
            case "Xe Máy": prefix = "XM"; break;
            case "Ô Tô": prefix = "OT"; break;
            case "Đồ Nội Thất": prefix = "NT"; break;
            case "Quần Áo": prefix = "QA"; break;
            case "Trang Sức": prefix = "TS"; break;
            default: prefix = "SP";
        }
        long timestamp = System.currentTimeMillis();
        return prefix + "_" + timestamp;
    }

    /**
     * Handle cancel button click
     * @param event ActionEvent from cancel button
     */
    @FXML
    private void huyCancelButton(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi", "Không thể quay về trang chủ!");
        }
    }

    /**
     * Show alert dialog
     * @param alertType Type of alert
     * @param title Alert title
     * @param message Alert message
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

