package com.mycompany.action;

import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class ChangePasswordAction {
    private static volatile ChangePasswordAction instance;
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ChangePasswordAction.class);
    private ChangePasswordAction() {}

    public static ChangePasswordAction getInstance() {
        if (instance == null) {
            synchronized (ChangePasswordAction.class) {
                if (instance == null) instance = new ChangePasswordAction();
            }
        }
        return instance;
    }

    public void handlePasswordChange(String olderPassword, String newPassword, String confirmPassword, Button saveButton) {
        // Validate không rỗng
        if (olderPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi thông tin", "Vui lòng điền tất cả các trường!");
            return;
        }

        // Validate mật khẩu mới trùng xác nhận
        if (!newPassword.equals(confirmPassword)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi thông tin", "Mật khẩu mới và xác nhận không trùng nhau!");
            return;
        }

        // Validate mật khẩu mới không trùng cũ
        if (olderPassword.equals(newPassword)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi thông tin", "Mật khẩu mới không được trùng với mật khẩu cũ!");
            return;
        }

        // Validate độ mạnh mật khẩu
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-Za-z0-9@$!#%*?&]{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi thông tin",
                    "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return;
        }

        // Gửi lên server — server tự xác minh mật khẩu cũ và hash mật khẩu mới
        String token = SessionManager.getInstance().getServerToken();
        boolean success = ApiClient.changePassword(olderPassword, newPassword, token);

        if (!success) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi thông tin",
                    "Mật khẩu cũ không đúng hoặc không kết nối được server!");
            return;
        }

        HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu đã được thay đổi thành công!");

        try {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            HandleNavigationAndAlert.getInstance().goToProfile(stage);
        } catch (Exception e) {
            logger.error("Lỗi quay lại profile: " + e.getMessage());
        }
    }
}