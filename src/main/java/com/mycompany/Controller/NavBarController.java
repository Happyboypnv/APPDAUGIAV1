package com.mycompany.Controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.HomeAction;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.Alert;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class NavBarController implements Initializable { // Controller chung cho các trang có thanh điều hướng, để tránh trùng code, giúp dễ bảo trì hơn

    // Initializable cho phép chúng ta thực hiện các thao tác khởi tạo sau khi tất cả các @FXML đã được liên kết với controller, đảm bảo rằng avatarImage đã sẵn sàng để sử dụng khi chúng ta thiết lập hình ảnh và clip.
    // Đảm bảo tất cả UI (ảnh, video...) được load trước khi thao tác tiếp, tránh null pointer
    // Ở đây còn có tác dụng khởi tạo menu item không qua scene buider, vì nếu tạo trong scene buider sẽ bị vướng dấu mũi tên thừa khi click vào ảnh, còn tạo trong controller thì sẽ không bị
    @FXML
    private ImageView avatarImage;

    @FXML
    private ImageView homeIcon;

    @FXML
    private VBox navBar; // Nếu muốn thêm nút mới vào thanh điều hướng, chỉ cần thêm vào đây là được, ko cần chỉnh sửa từng trang

    @FXML
    private Label createAuction;

    @FXML
    private Label transactionHistory;

    @FXML
    private Label financeManagement;

    private ContextMenu contextMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Image avt = new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm());
        avatarImage.setImage(avt);

        Circle clip = new Circle(20, 20, 20);
        avatarImage.setClip(clip); // tạo Avatar hình tròn, cho vào ImageView

        MenuItem profileItem = new MenuItem("Profile");
        MenuItem logoutItem = new MenuItem("Đăng xuất");

        profileItem.setOnAction(this::navigateToProfile);
        logoutItem.setOnAction(this::logOut); // Tạo menu item ở controller và gắn hành động cho nó

        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(profileItem, logoutItem); // cho vào danh sách drop down
        // ko tạo trong scene buider luôn để không bị vướng dấu mũi tên thừa

        Image home = new Image(getClass().getResource("/image/square.png").toExternalForm());
        homeIcon.setImage(home);
    }

    @FXML
    public void returnToHome(MouseEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện trang chủ không thành công!");
        }
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        // Hiển thị menu ngay bên dưới ImageView khi click
        // Side.BOTTOM giúp menu hiện lên trên, sát mép phải của ảnh
        contextMenu.show(avatarImage, Side.TOP, 0, 0);
    }

    @FXML
    public void logOut(ActionEvent event) {
        try {
            Stage stage = (Stage) avatarImage.getScene().getWindow();
            HomeAction.getInstance().dangXuat(stage);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Đăng xuất thất bại!");
        }
    }

    @FXML
    public void navigateToProfile(ActionEvent event) {
        try {
            Stage stage = (Stage) avatarImage.getScene().getWindow();
            HandleNavigationAndAlert.getInstance().goToProfile(stage);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể chuyển đến trang cá nhân!");
        }
    }

    @FXML
    public void navigateToFinance(MouseEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().goToFinance(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện quản lý tài chính không thành công!");
        }
    }
}