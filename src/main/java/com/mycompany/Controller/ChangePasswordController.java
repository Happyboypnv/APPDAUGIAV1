package com.mycompany.Controller;

import com.mycompany.action.ChangePasswordAction;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.TokenUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * ChangePasswordController - Controller quản lý trang Đổi mật khẩu
 *
 * MỤC ĐÍCH:
 * - Quản lý giao diện và logic của trang đổi mật khẩu
 * - Cho phép người dùng thay đổi mật khẩu hiện tại
 * - Validate mật khẩu cũ, mật khẩu mới, và xác nhận mật khẩu
 * - Lưu mật khẩu mới vào hệ thống
 *
 * TÍNH NĂNG CHÍNH:
 * - Nhập mật khẩu cũ để xác minh người dùng
 * - Nhập mật khẩu mới
 * - Xác nhận mật khẩu mới (phải giống nhau)
 * - Validate input trước khi lưu
 * - Lưu mật khẩu mới vào database
 * - Thông báo thành công/lỗi
 *
 * LUỒNG HOẠT ĐỘNG:
 * 1. Load trang → Initialize UI
 * 2. Nhập mật khẩu cũ, mật khẩu mới, xác nhận
 * 3. Click Save button
 * 4. Validate tất cả fields
 * 5. Verify mật khẩu cũ (query database)
 * 6. Check mật khẩu mới trùng xác nhận
 * 7. Hash mật khẩu mới + salt
 * 8. Lưu vào database
 * 9. Thông báo thành công
 */
public class ChangePasswordController implements Initializable {

    // @FXML FIELDS - Các thành phần UI được inject từ FXML
    @FXML private TextField olderPasswordField;    // Trường nhập mật khẩu cũ (fx:id="nameField")
    @FXML private TextField newPasswordField;      // Trường nhập mật khẩu mới (fx:id="nameField1")
    @FXML private TextField confirmPasswordField;  // Trường xác nhận mật khẩu (fx:id="nameField11")
    @FXML private Button saveButton;               // Nút lưu thay đổi

    /**
     * PHƯƠNG THỨC: initialize(URL url, ResourceBundle resourceBundle)
     * MỤC ĐÍCH: Khởi tạo trang Change Password khi load
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Khởi tạo các TextField và Button
     * - Có thể thêm placeholder text hoặc event listeners
     *
     * @param url URL của FXML file
     * @param resourceBundle ResourceBundle (có thể null)
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✅ ChangePasswordController initialized");
    }

    /**
     * PHƯƠNG THỨC: onClickedSaveButton(ActionEvent event)
     * MỤC ĐÍCH: Lưu mật khẩu mới khi click Save button
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy giá trị từ các TextField
     * 2. Validate tất cả fields không rỗng
     * 3. Validate mật khẩu mới trùng xác nhận
     * 4. Validate mật khẩu mới không trùng mật khẩu cũ
     * 5. Lấy thông tin người dùng từ session
     * 6. Verify mật khẩu cũ (query database)
     * 7. Hash mật khẩu mới với salt
     * 8. Cập nhật database
     * 9. Thông báo thành công
     *
     * XỬ LÝ LỖI:
     * - Nếu validation fail → hiển thị alert lỗi
     * - Nếu mật khẩu cũ sai → báo lỗi
     * - Nếu cập nhật thành công → báo thành công
     */
    @FXML
    public void onClickedSaveButton(ActionEvent event) {
        try {
            // 🔹 BƯỚC 1: Lấy giá trị từ các TextField
            String olderPassword = olderPasswordField.getText().trim();
            String newPassword = newPasswordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            ChangePasswordAction.getInstance().handlePasswordChange(olderPassword, newPassword, confirmPassword, saveButton);

        } catch (Exception e) {
            // Xử lý bất kỳ exception nào
            HandleNavigationAndAlert.getInstance().showAlert(
                Alert.AlertType.ERROR,
                "Lỗi hệ thống",
                "Có lỗi xảy ra: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}

