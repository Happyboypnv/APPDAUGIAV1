package com.mycompany.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

import com.mycompany.utils.*;

public class SignInController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField; // nơi nhận vào input của user
    private IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();

    @FXML
    public void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        System.out.println("🔐 Đang đăng nhập với email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ Email và Password!");
            return;
        }

        if (khoLuuTruNguoiDung.kiemTraNguoiDung(email, password)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công! Chào mừng bạn.");
            System.out.println("✅ Đăng nhập thành công với email: " + email);

            // Chuyen qua giao dien Home luon
            try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/view/Home.fxml"));
                Scene homeScene = new Scene(homeRoot);
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(homeScene);
                window.show();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện trang chủ không thành công!");
            }
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
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }
}