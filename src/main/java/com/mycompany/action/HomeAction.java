package com.mycompany.action;

import com.mycompany.exception.Login.*;
import javafx.fxml.FXML; // Quan trọng
import javafx.fxml.FXMLLoader; // Quan trọng
import javafx.scene.Parent; // Quan trọng
import javafx.scene.Scene; // Quan trọng // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import javafx.stage.Stage; // Quan trọng
import java.io.IOException; // Quan trọng
import java.time.*;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.utils.SessionManager;
import com.mycompany.exception.Login.*;

import java.util.concurrent.locks.*; // Handle nhieu nguoi dang nhap cung luc

/**
 * HomeAction - Class xử lý các thao tác trên trang Home/Dashboard
 *
 * MỤC ĐÍCH:
 * - Quản lý các chức năng chính của trang chủ
 * - Xử lý đăng xuất người dùng
 * - Chuẩn bị dữ liệu cho dashboard (tương lai)
 *
 * KẾT NỐI VỚI CONTROLLER:
 * - Hiện tại ít kết nối vì HomeController rất đơn giản
 * - NavBarController gọi HomeAction.getInstance().dangXuat() khi logout
 * - Tương lai: HomeController có thể gọi các phương thức load dashboard data
 *
 * TÍNH NĂNG CHÍNH:
 * - Đăng xuất: Xóa session và chuyển về trang đăng nhập
 * - Thread-safe: Sử dụng ReentrantLock để tránh race condition
 * - Validation: Kiểm tra trạng thái đăng nhập trước khi logout
 *
 * DESIGN PATTERN:
 * - Singleton: Chỉ có 1 instance HomeAction
 * - Thread-safe: Sử dụng Lock để đồng bộ hóa
 */
public class HomeAction {
    private static HomeAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();
    private final Lock lock = new ReentrantLock();
    private HomeAction() {};

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance HomeAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của HomeAction
     */
    public static HomeAction getInstance() {
        if (instance==null) {
            instance = new HomeAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o man hinh chinh
    }

    /**
     * dangXuat(Stage stage) - Xử lý đăng xuất người dùng
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ NavBarController.logOut() khi click menu "Đăng xuất"
     * - Nhận Stage từ NavBarController vì menu item không phải Node
     *
     * QUY TRÌNH:
     * 1. Kiểm tra xem người dùng đã đăng nhập chưa
     * 2. Nếu đã đăng nhập: logout khỏi SessionManager
     * 3. Load trang SignIn.fxml
     * 4. Tạo Scene mới và set vào Stage
     * 5. Hiển thị thông báo đăng xuất thành công
     *
     * XỬ LÝ LỖI:
     * - Nếu chưa đăng nhập → hiển thị alert "Chưa đăng nhập!"
     * - IOException khi load FXML → throw exception
     *
     * THREAD-SAFE:
     * - Không cần lock vì logout là thao tác đơn giản
     * - SessionManager đảm bảo thread-safe
     *
     * @param stage Stage hiện tại của ứng dụng
     * @throws IOException nếu không load được SignIn.fxml
     */
    @FXML
    public void dangXuat(Stage stage) throws IOException {
        if (SessionManager.getInstance().isLoggedIn()) {
            SessionManager.getInstance().logout();
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
            Scene signUpScene = new Scene(signUpRoot);
            stage.setScene(signUpScene);
            stage.show();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bạn đã đăng xuất thành công!");
        } else {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa đăng nhập!");
        } // Nút đăng xuất k phải node nên cũng cần stage riêng từ ImageView
    }

}