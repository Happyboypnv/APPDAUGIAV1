package com.mycompany.controller;

import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.AuctionSession;
import com.mycompany.server.dto.PhienDauGiaDTO;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.AuctionRepositorySQLite;
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

    private final AuctionRepositorySQLite auctionRepositorySQLite = new AuctionRepositorySQLite();



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tenPhienField.setEditable(false);

        tenSanPhamField.setEditable(false);

        moTaField.setEditable(false);

        giaKhoiDiemField.setEditable(false);

        ngayBatDau.setEditable(false);

        ngayKetThuc.setEditable(false);

        danhMucSanPham.setEditable(false);

    }
    @FXML
    public void accept(ActionEvent event) {
        AuctionSession auctionSession = auctionRepositorySQLite.findById(currentAuctionSession.maPhien);
        AuctionSessionService.getInstance().acceptAuctionRequest(auctionSession);
        try {
            HandleNavigationAndAlert.getInstance().handleGoToReviewAuction(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hiển thị", "Không load được trang danh sách chờ!");
            e.printStackTrace();
        }
    }

    @FXML
    public void deny(ActionEvent event) {
        AuctionSession auctionSession = auctionRepositorySQLite.findById(currentAuctionSession.maPhien);
        AuctionSessionService.getInstance().denyAuctionRequest(auctionSession);
        try {
            HandleNavigationAndAlert.getInstance().handleGoToReviewAuction(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hiển thị", "Không load được trang danh sách chờ!");
            e.printStackTrace();
        }
    }

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
        }
    }
}