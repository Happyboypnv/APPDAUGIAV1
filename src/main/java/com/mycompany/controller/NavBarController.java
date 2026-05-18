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
 *
 * MỤC ĐÍCH:
 * - Quản lý các thành phần UI của thanh điều hướng bên trái
 * - Xử lý các sự kiện click trên avatar, home icon, và các menu item
 * - Cung cấp chức năng điều hướng giữa các trang
 * - Hiển thị context menu khi click vào avatar
 *
 * TÍNH NĂNG CHÍNH:
 * - Hiển thị avatar hình tròn với hình ảnh mặc định
 * - Hiển thị home icon
 * - Context menu với Profile và Đăng xuất
 * - Điều hướng đến các trang: Home, Profile, Finance
 * - Xử lý đăng xuất
 *
 * CÁCH HOẠT ĐỘNG:
 * - Controller này được sử dụng bởi NavbarComponent.fxml
 * - NavbarComponent được include vào các trang khác (Home, Profile, Finance)
 * - Mỗi trang có navbar riêng biệt nhưng dùng chung controller này
 */
public class NavBarController implements Initializable { // Controller chung cho các trang có thanh điều hướng, để tránh trùng code, giúp dễ bảo trì hơn

    // Initializable cho phép chúng ta thực hiện các thao tác khởi tạo sau khi tất cả các @FXML đã được liên kết với controller, đảm bảo rằng avatarImage đã sẵn sàng để sử dụng khi chúng ta thiết lập hình ảnh và clip.
    // Đảm bảo tất cả UI (ảnh, video...) được load trước khi thao tác tiếp, tránh null pointer
    // Ở đây còn có tác dụng khởi tạo menu item không qua scene buider, vì nếu tạo trong scene buider sẽ bị vướng dấu mũi tên thừa khi click vào ảnh, còn tạo trong controller thì sẽ không bị
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NavBarController.class);
    // @FXML FIELDS - Các thành phần UI được inject từ FXML
    @FXML
    private ImageView avatarImage; // Hình ảnh avatar của người dùng (hình tròn)

    @FXML
    private ImageView homeIcon; // Icon home ở góc trên trái

    @FXML
    private VBox navBar; // Container chính chứa tất cả các thành phần navbar

    @FXML
    private Label createAuction; // Label "Tạo phiên đấu giá"

    @FXML
    private Label transactionHistory; // Label "Lịch sử giao dịch"

    @FXML
    private Label financeManagement; // Label "Tài chính"

    // Context menu hiển thị khi click vào avatar
    private ContextMenu contextMenu;

    /**
     * PHƯƠNG THỨC: initialize(URL location, ResourceBundle resources)
     * MỤC ĐÍCH: Khởi tạo navbar khi FXML được load
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Load và set hình ảnh avatar mặc định
     * 2. Tạo hình tròn clip cho avatar
     * 3. Tạo context menu với Profile và Đăng xuất
     * 4. Load và set hình ảnh home icon
     *
     * @param location URL của FXML file
     * @param resources ResourceBundle (có thể null)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 🔹 BƯỚC 1: Load và set avatar image
        // getClass().getResource() tìm file trong resources folder
        // toExternalForm() chuyển URL thành string path
        User currentUser = SessionManager.getInstance().getCurrentUser();

        loadAvatarImage(currentUser);

        // 🔹 BƯỚC 2: Tạo hình tròn cho avatar
        // Circle(centerX, centerY, radius) - tâm ở (20,20), bán kính 20
        Circle clip = new Circle(20, 20, 20);
        avatarImage.setClip(clip); // Cắt avatar thành hình tròn

        // 🔹 BƯỚC 3: Tạo context menu items
        MenuItem profileItem = new MenuItem("Profile");
        MenuItem logoutItem = new MenuItem("Đăng xuất");

        // Gắn event handlers cho menu items
        profileItem.setOnAction(this::navigateToProfile);
        logoutItem.setOnAction(this::logOut);

        // Tạo context menu và thêm items
        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(profileItem, logoutItem);

        // 🔹 BƯỚC 4: Load và set home icon
        Image home = new Image(getClass().getResource("/image/square.png").toExternalForm());
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

            // Trường hợp 1: Nếu là ảnh mặc định ban đầu -> Đọc từ resource tĩnh của bạn
            if (avatarPath.equals("image/default_avatar.jpg")) {
                URL resourceUrl = getClass().getResource("/" + avatarPath);
                if (resourceUrl != null) {
                    avatarImage.setImage(new Image(resourceUrl.toExternalForm()));
                }
            }
            // Trường hợp 2: Nếu là ảnh do user thay đổi -> Đọc từ thư mục lưu trữ vĩnh viễn trong dự án
            else {
                String projectDir = System.getProperty("user.dir");
                File externalFile = new File(projectDir + File.separator + "user_data" + File.separator + avatarPath);

                if (externalFile.exists()) {
                    avatarImage.setImage(new Image(externalFile.toURI().toString()));
                } else {
                    // Nếu không tìm thấy file, quay về ảnh mặc định trong resource
                    avatarImage.setImage(new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm()));
                }
            }
        } catch (Exception e) {
            // If any error, use default avatar
            try {
                Image avt = new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm());
                avatarImage.setImage(avt);
            } catch (Exception ignored) {}
            }
    }



    /**
     * PHƯƠNG THỨC: returnToHome(MouseEvent event)
     * MỤC ĐÍCH: Xử lý sự kiện click vào home icon
     *
     * GIẢI THÍCH:
     * - Điều hướng về trang chủ (Home page)
     * - Sử dụng HandleNavigationAndAlert để xử lý navigation
     * - Hiển thị alert nếu có lỗi
     *
     * @param event MouseEvent từ click
     */
    @FXML
    public void returnToHome(MouseEvent event) {
        cleanupBiddingRoom((Node) event.getSource()); // ⭐ cleanup trước
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện trang chủ không thành công!");
        }
    }

    /**
     * PHƯƠNG THỨC: handleAvatarClick(MouseEvent event)
     * MỤC ĐÍCH: Hiển thị context menu khi click vào avatar
     *
     * GIẢI THÍCH:
     * - Hiển thị menu dropdown với Profile và Đăng xuất
     * - Menu xuất hiện ở phía TOP của avatar (trên cùng)
     * - Offset (0, 0) nghĩa là sát vào avatar
     *
     * @param event MouseEvent từ click
     */
    @FXML
    private void handleAvatarClick(MouseEvent event) {
        // Hiển thị menu ngay bên dưới ImageView khi click
        // Side.TOP giúp menu hiện lên trên, sát mép phải của ảnh
        contextMenu.show(avatarImage, Side.TOP, 0, 0);
    }
    /**
     * navigateToCreateAuction(MouseEvent event)
     * Điều hướng đến trang Tạo Phiên Đấu Giá khi click vào label trong sidebar.
     */
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
    /**
     * PHƯƠNG THỨC: logOut(ActionEvent event)
     * MỤC ĐÍCH: Xử lý đăng xuất người dùng
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy Stage từ avatarImage
     * 2. Gọi HomeAction.dangXuat() để xử lý đăng xuất
     * 3. Hiển thị alert nếu có lỗi
     *
     * @param event ActionEvent từ menu item
     */
    @FXML
    public void logOut(ActionEvent event) {
        try {
            // Lấy Stage từ scene của avatarImage
            Stage stage = (Stage) avatarImage.getScene().getWindow();
            HomeAction.getInstance().logOut(stage);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Đăng xuất thất bại!");
        }
    }

    /**
     * PHƯƠNG THỨC: navigateToProfile(ActionEvent event)
     * MỤC ĐÍCH: Điều hướng đến trang Profile
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy Stage từ avatarImage
     * 2. Gọi HandleNavigationAndAlert.goToProfile()
     * 3. Hiển thị alert nếu có lỗi
     *
     * @param event ActionEvent từ menu item
     */
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

    /**
     * PHƯƠNG THỨC: navigateToFinance(MouseEvent event)
     * MỤC ĐÍCH: Điều hướng đến trang Finance (Quản lý tài chính)
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Gọi HandleNavigationAndAlert.goToFinance()
     * 2. Hiển thị alert nếu có lỗi
     *
     * @param event MouseEvent từ label click
     */
    @FXML
    public void navigateToFinance(MouseEvent event) {
        cleanupBiddingRoom((Node) event.getSource()); // ⭐ Cleanup trước khi điều hướng
        try {
            HandleNavigationAndAlert.getInstance().goToFinance(event);
        } catch (IOException e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện quản lý tài chính không thành công!");
        }
    }

    /**
     * PHƯƠNG THỨC: goToCreatePhienDauGia(MouseEvent event)
     * MỤC ĐÍCH: Điều hướng đến trang tạo phiên đấu giá
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Gọi HandleNavigationAndAlert.goToCreatePhienDauGia()
     * 2. Hiển thị alert nếu có lỗi
     *
     * @param event MouseEvent từ label click
     */
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
            // Lấy scene hiện tại
            Scene currentScene = sourceNode.getScene();
            if (currentScene == null) return;

            // Tìm root node
            Parent root = currentScene.getRoot();
            if (root == null) return;

            // Kiểm tra xem controller hiện tại có phải BiddingRoom không
            // bằng cách tìm fx:id đặc trưng của BiddingRoom
            Node placeBidButton = root.lookup("#placeBidButton");
            if (placeBidButton != null) {
                // Đang ở BiddingRoom - tìm controller qua userData
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
}