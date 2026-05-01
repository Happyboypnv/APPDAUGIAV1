package com.mycompanytest.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.BufferedReader;
import java.io.FileReader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SignInController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ Email và Password!");
            return;
        }

        boolean isLoginSuccess = false; // Biến cờ hiệu để đánh dấu đăng nhập thành công hay chưa

        // ==========================================
        // ĐỌC FILE users.txt ĐỂ TÌM TÀI KHOẢN
        // ==========================================
        try {
            BufferedReader reader = new BufferedReader(new FileReader("users.txt"));
            String line;

            // Đọc từng dòng cho đến khi hết file
            while ((line = reader.readLine()) != null) {
                // Cắt dòng văn bản bằng dấu phẩy
                String[] parts = line.split(",");

                // Đảm bảo dòng có đủ 2 phần (email và password)
                if (parts.length == 2) {
                    String savedEmail = parts[0];
                    String savedPassword = parts[1];

                    // So sánh dữ liệu trong file với dữ liệu người dùng nhập
                    if (savedEmail.equals(email) && savedPassword.equals(password)) {
                        isLoginSuccess = true; // Tìm thấy tài khoản hợp lệ
                        break; // Thoát vòng lặp luôn, không cần tìm tiếp
                    }
                }
            }
            reader.close();

        } catch (Exception e) {
            // Lỗi này xảy ra nếu file users.txt chưa được tạo (chưa có ai đăng ký)
            System.out.println("Chưa có cơ sở dữ liệu hoặc lỗi đọc file.");
        }

        // ==========================================
        // XỬ LÝ KẾT QUẢ ĐĂNG NHẬP
        // ==========================================
        if (isLoginSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công! Chào mừng bạn.");
            System.out.println("Đăng nhập thành công với email: " + email);

            // TẠI ĐÂY: Bạn có thể viết thêm code chuyển sang giao diện TRANG CHỦ (Template.fxml)

        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Email hoặc mật khẩu không chính xác!");
        }
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void switchToSignUp(ActionEvent event) throws IOException {
        // 1. Tải file FXML của màn hình Sign Up
        // LƯU Ý: Thay "SignUp.fxml" bằng tên file FXML thực tế của bạn
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));

        // 2. Tạo một Scene (cảnh) mới chứa giao diện Sign Up
        Scene signUpScene = new Scene(signUpRoot);

        // 3. Lấy Stage (cửa sổ ứng dụng hiện tại) từ sự kiện click chuột
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 4. Đặt Scene mới lên Stage và hiển thị
        window.setScene(signUpScene);
        window.show();
    }
}