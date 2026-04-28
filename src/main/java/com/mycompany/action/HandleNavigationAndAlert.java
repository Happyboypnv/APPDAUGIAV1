package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader; // Quan trọng
import javafx.scene.Node; // Quan trọng
import javafx.scene.Parent; // Quan trọng
import javafx.scene.Scene; // Quan trọng
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage; // Quan trọng
import java.io.IOException; // Quan trọng
import java.time.*;
import com.mycompany.utils.SessionManager;
import com.mycompany.exception.Login.*;

/**
 * HandleNavigationAndAlert - Class xử lý điều hướng và thông báo
 *
 * MỤC ĐÍCH:
 * - Quản lý việc chuyển đổi giữa các trang (scenes) trong ứng dụng
 * - Hiển thị các hộp thoại thông báo (alerts) cho người dùng
 * - Cung cấp các phương thức điều hướng thống nhất cho toàn bộ ứng dụng
 *
 * KẾT NỐI VỚI CONTROLLERS:
 * - Được gọi từ tất cả controllers khi cần chuyển trang
 * - NavBarController: gọi các phương thức điều hướng (goToHome, goToProfile, goToFinance)
 * - SignIn/SignUp Controllers: gọi handleGoToSignIn/SignUp
 * - Các controllers khác: gọi showAlert() để hiển thị thông báo
 *
 * TÍNH NĂNG CHÍNH:
 * - Điều hướng giữa các trang FXML
 * - Hiển thị alerts với các loại: INFORMATION, WARNING, ERROR
 * - Kiểm tra trạng thái đăng nhập trước khi cho phép truy cập
 * - Xử lý Stage và Scene management
 *
 * DESIGN PATTERN:
 * - Singleton: Chỉ có 1 instance duy nhất trong toàn bộ ứng dụng
 * - Facade: Cung cấp interface đơn giản cho việc điều hướng phức tạp
 */
public class HandleNavigationAndAlert { // Class này đảm nhận nhiệm vụ di chuyển đến các trang khác nhau + in thông báo
    private static HandleNavigationAndAlert instance;

    private HandleNavigationAndAlert() {};

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance HandleNavigationAndAlert trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của HandleNavigationAndAlert
     */
    public static HandleNavigationAndAlert getInstance() {
        if (instance==null){
            instance = new HandleNavigationAndAlert();
        }
        return instance;
    }

    /**
     * handleGoToHome(Event event) - Điều hướng về trang Home
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ NavBarController.returnToHome() khi click home icon
     * - Được gọi từ LoginAction.dangNhap() sau khi đăng nhập thành công
     *
     * QUY TRÌNH:
     * 1. Load Home.fxml bằng FXMLLoader
     * 2. Tạo Scene mới với root là Home StackPane
     * 3. Lấy Stage từ event source
     * 4. Set scene mới và hiển thị
     *
     * @param event Event từ UI (MouseEvent hoặc ActionEvent)
     * @throws IOException nếu không load được FXML
     */
    public void handleGoToHome(Event event) throws IOException { // phải dùng Event vì phải nhận cả MouseEvent (nút home) và ActionEvent (còn lại)
        StackPane homeRoot = FXMLLoader.load(getClass().getResource("/view/Home.fxml"));
        Scene homeScene = new Scene(homeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    /**
     * handleGoToSignIn(ActionEvent event) - Điều hướng về trang Sign In
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ SignUpController khi chuyển sang đăng nhập
     * - Được gọi từ LoginAction.dangKy() sau khi đăng ký thành công
     *
     * @param event ActionEvent từ button click
     * @throws IOException nếu không load được FXML
     */
    public void handleGoToSignIn(ActionEvent event) throws IOException {
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    /**
     * handleGoToSignUp(ActionEvent event) - Điều hướng về trang Sign Up
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ SignInController khi chuyển sang đăng ký
     *
     * @param event ActionEvent từ button click
     * @throws IOException nếu không load được FXML
     */
    public void handleGoToSignUp(ActionEvent event) throws IOException {
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    /**
     * goToProfile(Stage stage) - Điều hướng về trang Profile
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ NavBarController.navigateToProfile() khi click menu Profile
     *
     * ĐẶC BIỆT:
     * - Kiểm tra đăng nhập trước khi cho phép truy cập
     * - Nhận Stage trực tiếp thay vì từ Event (vì menu item không phải Node)
     *
     * @param stage Stage hiện tại của ứng dụng
     * @throws IOException nếu không load được FXML
     */
    public void goToProfile(Stage stage) throws IOException {
        NguoiDung nguoiDung = SessionManager.getInstance().getCurrentUser();
        if (nguoiDung == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để xem trang cá nhân!");
            return;
        }
        Parent profileRoot = FXMLLoader.load(getClass().getResource("/view/Profile.fxml"));
        Scene profileScene = new Scene(profileRoot);

        stage.setScene(profileScene);
        stage.show();
        // Do menu item khong phai la node => k set stage truc tiep tu no duoc ma phai lay qua ImageView ben Controller
    }

    /**
     * goToFinance(Event event) - Điều hướng về trang Finance
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ NavBarController.navigateToFinance() khi click menu Finance
     *
     * @param event Event từ UI
     * @throws IOException nếu không load được FXML
     */
    public void goToFinance(Event event) throws IOException {
        StackPane financeRoot = FXMLLoader.load(getClass().getResource("/view/Finance.fxml"));
        Scene homeScene = new Scene(financeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    /**
     * showAlert(Alert.AlertType alertType, String title, String message) - Hiển thị hộp thoại thông báo
     *
     * KẾT NỐI VỚI CONTROLLERS:
     * - Được gọi từ tất cả controllers khi cần hiển thị thông báo
     * - FinanceController: thông báo nạp/rút tiền thành công/lỗi
     * - ProfileController: thông báo cập nhật thông tin
     * - LoginAction: thông báo đăng nhập/đăng ký
     * - Và nhiều controllers khác...
     *
     * CÁC LOẠI ALERT:
     * - AlertType.INFORMATION: Thông tin thành công (màu xanh)
     * - AlertType.WARNING: Cảnh báo (màu vàng)
     * - AlertType.ERROR: Lỗi (màu đỏ)
     *
     * @param alertType Loại alert (INFORMATION/WARNING/ERROR)
     * @param title Tiêu đề hộp thoại
     * @param message Nội dung thông báo
     */
    public void showAlert (Alert.AlertType alertType, String title, String message){
        Alert alert = new Alert(alertType); // 1. Khởi tạo một hộp thoại mới
        alert.setTitle(title);              // 2. Đặt tiêu đề cho cửa sổ Pop-up
        alert.setHeaderText(null);          // 3. Tiêu đề phụ (để null cho gọn)
        alert.setContentText(message);      // 4. Nội dung chi tiết cần thông báo
        alert.showAndWait();                // 5. Hiển thị lên và dừng code lại
    }
}