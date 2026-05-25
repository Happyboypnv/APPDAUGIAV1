package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.HomeAction;
import com.mycompany.models.User;
import com.mycompany.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * NavBarController - Controller quản lý thanh điều hướng (Navigation Bar)
 */
public class NavBarController implements Initializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NavBarController.class);

    @FXML
    private ImageView avatarImage;

    @FXML
    private ImageView homeIcon;

    @FXML
    private VBox navBar;

    @FXML
    private Label createAuction;

    @FXML
    private Label transactionHistory;

    @FXML
    private Label financeManagement;

    private ContextMenu contextMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        loadAvatarImage(currentUser);

        // Tạo hình tròn cho avatar
        Circle clip = new Circle(20, 20, 20);
        avatarImage.setClip(clip);

        // Tạo context menu items
        MenuItem profileItem = new MenuItem("Profile");
        MenuItem logoutItem = new MenuItem("Đăng xuất");

        profileItem.setOnAction(this::navigateToProfile);
        logoutItem.setOnAction(this::logOut);

        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(profileItem, logoutItem);

        // 🔹 ĐÃ SỬA: Thêm /resources vào trước đường dẫn file ảnh home
        Image home = new Image(getClass().getResource("/resources/image/square.png").toExternalForm());
        homeIcon.setImage(home);
    }

    private void loadAvatarImage(User currentUser) {
        try {
            String avatarPath;
            if (currentUser != null && currentUser.getAvatarPath() != null) {
                avatarPath = currentUser.getAvatarPath();
            } else {
                avatarPath = "image/default_avatar.jpg";
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING,"Ko tìm thấy đường dẫn", "Không lấy được đường dẫn từ user!");
            }

            // Trường hợp 1: Nếu là ảnh mặc định ban đầu -> Đọc từ resource tĩnh
            if (avatarPath.equals("image/default_avatar.jpg")) {
                // 🔹 ĐÃ SỬA: Thêm /resources vào trước đường dẫn resource tĩnh ban đầu
                URL resourceUrl = getClass().getResource("/resources/" + avatarPath);
                if (resourceUrl != null) {
                    avatarImage.setImage(new Image(resourceUrl.toExternalForm()));
                }
            }
            // Trường hợp 2: Nếu là ảnh do user thay đổi
            else {
                String projectDir = System.getProperty("user.dir");
                File externalFile = new File(projectDir + File.separator + "user_data" + File.separator + avatarPath);

                if (externalFile.exists()) {
                    avatarImage.setImage(new Image(externalFile.toURI().toString()));
                } else {
                    // 🔹 ĐÃ SỬA: Thêm /resources vào trước đường dẫn fallback dự phòng ảnh mặc định
                    avatarImage.setImage(new Image(getClass().getResource("/resources/image/default_avatar.jpg").toExternalForm()));
                }
            }
        } catch (Exception e) {
            try {
                // 🔹 ĐÃ SỬA: Thêm /resources vào trước đường dẫn fallback trong khối catch lỗi ngoại lệ
                Image avt = new Image(getClass().getResource("/resources/image/default_avatar.jpg").toExternalForm());
                avatarImage.setImage(avt);
            } catch (Exception ignored) {}
        }
    }

    @FXML
    public void returnToHome(MouseEvent event) {
        cleanupBiddingRoom((Node) event.getSource());
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện trang chủ không thành công!");
        }
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        contextMenu.show(avatarImage, Side.TOP, 0, 0);
    }

    @FXML
    public void navigateToCreateAuction(MouseEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().goToCreateAuction(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi giao diện",
                    "Không thể mở trang tạo phiên đấu giá!"
            );
        }
    }

    @FXML
    public void logOut(ActionEvent event) {
        try {
            Stage stage = (Stage) avatarImage.getScene().getWindow();
            HomeAction.getInstance().logOut(stage);
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
        cleanupBiddingRoom((Node) event.getSource());
        try {
            HandleNavigationAndAlert.getInstance().goToFinance(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện quản lý tài chính không thành công!");
        }
    }

    @FXML
    public void goToCreatePhienDauGia(MouseEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().goToCreatePhienDauGia(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện tạo phiên đấu giá không thành công!");
        }
    }

    private void cleanupBiddingRoom(Node sourceNode) {
        try {
            Scene currentScene = sourceNode.getScene();
            if (currentScene == null) return;

            Parent root = currentScene.getRoot();
            if (root == null) return;

            Node placeBidButton = root.lookup("#placeBidButton");
            if (placeBidButton != null) {
                Object userData = root.getUserData();
                if (userData instanceof BiddingRoomController) {
                    ((BiddingRoomController) userData).onClose();
                    logger.info("✅ BiddingRoom WebSocket đã được cleanup");
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi cleanup BiddingRoom: " + e.getMessage());
        }
    }

    @FXML
    public void navigateToTransactionHistory(MouseEvent event) {
        cleanupBiddingRoom((Node) event.getSource());
        try {
            HandleNavigationAndAlert.getInstance().goToTransactionHistory(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR, "Lỗi giao diện",
                "Không thể mở trang lịch sử giao dịch!"
            );
        }
    }
}