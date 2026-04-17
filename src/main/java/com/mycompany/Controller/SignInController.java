package com.mycompany.Controller;

import com.mycompany.exception.Login.PasswordException;
import com.mycompany.exception.Login.UserException;
import com.mycompany.exception.Login.UserNameException;
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
    private IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();

    public void checkEmail(String email) throws UserNameException {
        if(email == null || email.isEmpty()) throw new UserNameException("Email đang bỏ trống!");
    }

    public void checkPassword(String password)throws PasswordException {
        if(password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
    }

    public void checkUser(String email, String password) throws UserException {
        if (!khoLuuTruNguoiDung.kiemTraNguoiDung(email, password)) throw new UserException("Sai email hoặc mật khẩu");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            checkEmail(email);
            checkPassword(password);
            checkUser(email, password);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công! Chào mừng bạn.");
            System.out.println("Đăng nhập thành công với email: " + email);
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
        } catch (UserException | UserNameException | PasswordException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", e.getMessage());
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