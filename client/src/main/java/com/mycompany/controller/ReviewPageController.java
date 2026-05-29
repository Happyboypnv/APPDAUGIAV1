package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.User;
import com.mycompany.server.dto.PhienDauGiaDTO;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ReviewPageController implements Initializable {

    @FXML
    private TextField tenPhienField;
    @FXML
    private TextField tenSanPhamField;
    @FXML
    private TextField danhMucSanPham;
    @FXML
    private TextArea moTaField;
    @FXML
    private TextField giaKhoiDiemField;

    @FXML
    private TextField ngayBatDau;

    @FXML
    private TextField ngayKetThuc;

    @FXML
    private StackPane dropArea;
    @FXML
    private ImageView previewImage;
    @FXML
    private VBox instructionText;

    @FXML
    private Button approveBtn;

    @FXML
    private Button denyBtn;

    private File selectedImageFile = null;

    private PhienDauGiaDTO currentAuctionSession;

    @FXML
    private VBox adminNavBar;
    @FXML
    private AdminNavBarController adminNavBarController;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        adminNavBarController.setActive("review");

        tenPhienField.setEditable(false);

        tenSanPhamField.setEditable(false);

        moTaField.setEditable(false);

        giaKhoiDiemField.setEditable(false);

        ngayBatDau.setEditable(false);

        ngayKetThuc.setEditable(false);

        danhMucSanPham.setEditable(false);

        instructionText.setVisible(false);


    }
    @FXML
    public void accept(ActionEvent event) {
        String token = SessionManager.getInstance().getServerToken();
        boolean success = ApiClient.approveAuction(currentAuctionSession.maPhien, token);
        if (!success) {
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR, "Lỗi", "Không thể duyệt phiên đấu giá!");
            return;
        }
        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.INFORMATION, "Thành công", "Đã duyệt phiên đấu giá!");
        try {
            HandleNavigationAndAlert.getInstance().handleGoToReviewAuction(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR, "Lỗi hiển thị", "Không load được trang danh sách chờ!");
            e.printStackTrace();
        }
    }

    @FXML
    public void deny(ActionEvent event) {
        String token = SessionManager.getInstance().getServerToken();
        boolean success = ApiClient.rejectAuction(currentAuctionSession.maPhien, token);
        if (!success) {
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR, "Lỗi", "Không thể từ chối phiên đấu giá!");
            return;
        }
        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối phiên đấu giá!");
        try {
            HandleNavigationAndAlert.getInstance().handleGoToReviewAuction(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR, "Lỗi hiển thị", "Không load được trang danh sách chờ!");
            e.printStackTrace();
        }
    }

    private void loadProductImage(PhienDauGiaDTO session) {
        try {
            String productPath;
            if (session != null && session.productImgPath != null) {
                System.out.println(session.maPhien);
                System.out.println(session.productImgPath);
                productPath = session.productImgPath;
            } else {
                productPath = "image/default_avatar.jpg";
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING,"Ko tìm thấy đường dẫn", "Không lấy được đường dẫn product từ phiên!");
            }

            // Trường hợp 1: Nếu là ảnh mặc định ban đầu -> Đọc từ resource tĩnh của bạn
            if (productPath.equals("image/default_avatar.jpg")) {
                URL resourceUrl = getClass().getResource("/" + productPath);
                if (resourceUrl != null) {
                    previewImage.setImage(new Image(resourceUrl.toExternalForm()));
                }
            }
            // Trường hợp 2: Nếu là ảnh do user thay đổi -> Đọc từ thư mục lưu trữ vĩnh viễn trong dự án
            else {
                String projectDir = System.getProperty("user.dir");
                File externalFile = new File(projectDir + File.separator + "user_data" + File.separator + productPath);

                if (externalFile.exists()) {
                    previewImage.setImage(new Image(externalFile.toURI().toString()));
                } else {
                    // Nếu không tìm thấy file, quay về ảnh mặc định trong resource
                    previewImage.setImage(new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm()));
                }
            }
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING, "Lỗi ko xác định", "Lỗi ko xác định");
            try {
                Image avt = new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm());
                previewImage.setImage(avt);
            } catch (Exception ignored) {}
        }
    }

    public void setCurrentAuctionSession (PhienDauGiaDTO phien) {
        this.currentAuctionSession = phien;

        if (currentAuctionSession != null) {
            tenPhienField.setText(currentAuctionSession.tenPhien);
            tenSanPhamField.setText(currentAuctionSession.tenSanPham);
            moTaField.setText(currentAuctionSession.moTa);
            DecimalFormat df = new DecimalFormat("#,###");
            giaKhoiDiemField.setText(df.format(phien.giaHienTai) + " VNĐ");

            DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try {
                if (phien.thoiGianBatDau != null) {
                    LocalDateTime start = LocalDateTime.parse(phien.thoiGianBatDau, inputFormatter);
                    ngayBatDau.setText(start.format(outputFormatter));
                }
                if (phien.thoiGianKetThuc != null) {
                    LocalDateTime end = LocalDateTime.parse(phien.thoiGianKetThuc, inputFormatter);
                    ngayKetThuc.setText(end.format(outputFormatter));
                }
            } catch (Exception e) {
                // Phòng hờ nếu chuỗi ngày tháng truyền từ server không đúng chuẩn ISO
                ngayBatDau.setText(phien.thoiGianBatDau);
                ngayKetThuc.setText(phien.thoiGianKetThuc);
            }

            danhMucSanPham.setText(currentAuctionSession.danhMucSanPham);
            loadProductImage(currentAuctionSession);
        }
    }
}
