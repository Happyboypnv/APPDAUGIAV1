package com.mycompany.action;

import com.mycompany.utils.ApiClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

import com.mycompany.utils.SessionManager;

import java.util.concurrent.locks.*;

public class HomeAction {
    private final Lock lock = new ReentrantLock();
    private HomeAction() {};

    private static volatile HomeAction instance;
    public static HomeAction getInstance() {
        if (instance == null) {
            synchronized (HomeAction.class) {
                if (instance == null) instance = new HomeAction();
            }
        }
        return instance;
    }

    @FXML
    public void logOut(Stage stage) throws IOException {
        // CÁCH FIX: Kiểm tra an toàn xem có đang đăng nhập và user có tồn tại không
        if (SessionManager.getInstance().isLoggedIn() && SessionManager.getInstance().getCurrentUser() != null) {

            // Nếu bạn cần lấy email để làm gì đó trong tương lai, hãy lấy ở bên trong khối if này
            // String email = SessionManager.getInstance().getCurrentUser().getEmail();

            // Lấy token để báo cho Server biết là client này muốn đăng xuất
            String token = SessionManager.getInstance().getServerToken();

            if (token != null && !token.isEmpty()) {
                try {
                    ApiClient.logout(token);
                } catch (Exception ignored) {
                    // Nếu server lỗi thì kệ, vẫn tiến hành xóa session ở máy Client
                }
            }

            // Xóa sạch session cục bộ
            SessionManager.getInstance().logout();

            // ĐÃ SỬA: Thêm /resources vào trước đường dẫn để không bị lỗi Location is required
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
            Scene signUpScene = new Scene(signUpRoot);
            stage.setScene(signUpScene);
            stage.show();

            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bạn đã đăng xuất thành công!");
        } else {
            // Nếu chưa đăng nhập hoặc session rỗng thì báo lỗi
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa đăng nhập hoặc phiên đã hết hạn!");

            // Xóa dự phòng và chuyển về màn hình đăng nhập cho an toàn
            SessionManager.getInstance().logout();
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
            stage.setScene(new Scene(signUpRoot));
            stage.show();
        }
    }
}