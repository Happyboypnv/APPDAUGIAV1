package com.mycompany.Controller;

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

    @FXML private TextField emailBox;
    @FXML private PasswordField passwordBox; // nơi nhận vào input của user

    @FXML
    public void handleLogin(ActionEvent event) {
        String email = emailBox.getText().trim();
        String password = passwordBox.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ Email và Password!");
            return;
        }

        boolean isLoginSuccess = false; // flag check xem co dang nhap thanh cong khong
        try {
            String firstChar = String.valueOf(email.charAt(0)).toLowerCase();
            BufferedReader reader = new BufferedReader(new FileReader(UserDatabaseStarter.getFile(firstChar))); // chi search tu file co chu cai dau nhu the thoi => do phai search nhieu
            String line;

            // het file thi dung
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length == 2) {
                    String savedEmail = parts[0];
                    String savedPassword = parts[1];

                    if (savedEmail.equals(email) && savedPassword.equals(password)) {
                        isLoginSuccess = true;
                        break;
                    }
                }
            }
            reader.close();

        } catch (IOException e) {
            System.out.println("Chưa có cơ sở dữ liệu hoặc lỗi đọc file.");
        }

        if (isLoginSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công! Chào mừng bạn.");
            System.out.println("Đăng nhập thành công với email: " + email);

            // Chuyen qua giao dien Home luon
            try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/Home.fxml"));
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
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/SignUp.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }
}