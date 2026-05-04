package com.mycompany.action;

import com.mycompany.exception.Login.PasswordException;
import com.mycompany.models.NguoiDung;
import com.mycompany.utils.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.Map;

public class ChangePasswordAction {
    private static ChangePasswordAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();

    private ChangePasswordAction() {}

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance ChangePasswordAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của ChangePasswordAction
     */
    public static ChangePasswordAction getInstance() {
        if (instance == null) {
            instance = new ChangePasswordAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o trang ca nhan
    }

    public void handlePasswordChange(String olderPassword, String newPassword, String confirmPassword, Button saveButton) {
        // 🔹 BƯỚC 2: Validate không rỗng
        if (olderPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi thông tin",
                    "Vui lòng điền tất cả các trường!"
            );
            return;
        }

        // 🔹 BƯỚC 3: Validate mật khẩu mới trùng xác nhận
        if (!newPassword.equals(confirmPassword)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi thông tin",
                    "Mật khẩu mới và xác nhận không trùng nhau!"
            );
            return;
        }

        // 🔹 BƯỚC 4: Validate mật khẩu mới không trùng cũ
        if (olderPassword.equals(newPassword)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi thông tin",
                    "Mật khẩu mới không được trùng với mật khẩu cũ!"
            );
            return;
        }

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-Za-z0-9@$!#%*?&]{8,}$";
        if (!newPassword.matches( passwordRegex )) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi thông tin",
                    "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!"
            );
            return;
        }

        // 🔹 BƯỚC 5: Lấy thông tin người dùng từ session
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> userInfo = TokenUtil.getUserInfoFromToken(token);

        if (userInfo == null) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi",
                    "Không thể lấy thông tin người dùng!"
            );
            return;
        }

        String email = (String) userInfo.get("email");

        // 🔹 BƯỚC 6:
        // Cần thêm logic để verify mật khẩu cũ với database
        // Gợi ý: Sử dụng KhoLuuTruNguoiDungSQLite.kiemTraNguoiDung(email, olderPassword)
        // Nếu không match → báo lỗi "Mật khẩu cũ không đúng"

        if (!khoLuuTruNguoiDung.kiemTraNguoiDung(email, olderPassword)) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Lỗi thông tin",
                    "Mật khẩu cũ không đúng!"
            );
            return;
        }

        // 🔹 BƯỚC 7:
        // Cần thêm logic để:
        // 1. Tạo salt mới
        // 2. Hash mật khẩu mới với salt
        // 3. Cập nhật database
        // 4. Thông báo thành công

        String newSalt = BoMaHoaMatKhau.taoSalt();
        String hashedNewPassword = BoMaHoaMatKhau.maHoaMatKhau(newPassword, newSalt);

        NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();
        currentUser.setMatKhau(hashedNewPassword);
        currentUser.setSalt(newSalt);

        khoLuuTruNguoiDung.capNhatNguoiDung(currentUser);

        HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.INFORMATION,
                "Thành công",
                "Mật khẩu đã được thay đổi thành công!"
        );

        // 🔹 BƯỚC 8: Quay lại trang Profile sau khi lưu thành công
        try {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            HandleNavigationAndAlert.getInstance().goToProfile(stage);
        } catch (Exception e) {
            // Nếu có lỗi quay lại, chỉ log mà không crash
            System.err.println("Lỗi quay lại profile: " + e.getMessage());
        }
    }
}