package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.HomeAction;
import com.mycompany.models.User;
import com.mycompany.utils.SessionManager;
import javafx.application.Platform;
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
    private Label activeNavItem = null;

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

        Image home = new Image(getClass().getResource("/image/square.png").toExternalForm());
        homeIcon.setImage(home);
        Platform.runLater(this::highlightCurrentPage);
    }

    private void highlightCurrentPage() {
        if (navBar == null || navBar.getScene() == null) return;
        Parent root = navBar.getScene().getRoot();
        if (root == null) return;
        if (root.lookup("#transactionListView") != null) {
            setActiveNav(transactionHistory);
        } else if (root.lookup("#depositButton") != null) {
            setActiveNav(financeManagement);
        } else if (root.lookup("#tenPhienField") != null ||
            root.lookup("#createAuctionButton") != null) {
            setActiveNav(createAuction);
        }
    }

    private void setActiveNav(Label item) {
        if (activeNavItem != null) {
            activeNavItem.getStyleClass().remove("nav-item-active");
            if (!activeNavItem.getStyleClass().contains("nav-item"))
                activeNavItem.getStyleClass().add("nav-item");
        }
        activeNavItem = item;
        if (item != null) {
            item.getStyleClass().remove("nav-item");
            if (!item.getStyleClass().contains("nav-item-active"))
                item.getStyleClass().add("nav-item-active");
        }
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
                // FIX: Load default avatar from /image/ path in resources (NOT /resources/)
                URL resourceUrl = getClass().getResource("/" + avatarPath);
                if (resourceUrl != null) {
                    avatarImage.setImage(new Image(resourceUrl.toExternalForm()));
                } else {
                    logger.warn("Cannot find default avatar in resources: /" + avatarPath);
                }
            }
            // Trường hợp 2: Nếu là ảnh do user thay đổi -> Đọc từ thư mục user_data
            else {
                String projectDir = System.getProperty("user.dir");
                File externalFile = new File(projectDir + File.separator + "user_data" + File.separator + avatarPath);

                if (externalFile.exists()) {
                    avatarImage.setImage(new Image(externalFile.toURI().toString()));
                    logger.info("✅ Avatar found at: " + externalFile.getAbsolutePath());
                } else {
                    // If file not found, fallback to default avatar in resources
                    logger.warn("Avatar file not found at: " + externalFile.getAbsolutePath() + ", using default.");
                    URL resourceUrl = getClass().getResource("/image/default_avatar.jpg");
                    if (resourceUrl != null) {
                        avatarImage.setImage(new Image(resourceUrl.toExternalForm()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error loading avatar: " + e.getMessage());
            try {
                // Fallback: load default avatar from resources
                URL resourceUrl = getClass().getResource("/image/default_avatar.jpg");
                if (resourceUrl != null) {
                    Image avt = new Image(resourceUrl.toExternalForm());
                    avatarImage.setImage(avt);
                }
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
    public void navigateToLichSuGiaoDich(MouseEvent event) {
        setActiveNav(transactionHistory);
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