package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.LoginAction;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class SignInController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void signIn(ActionEvent event) {
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText().trim();
        LoginAction.getInstance().signIn(event, email, password);
    }

    @FXML
    public void goToSignUp(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToSignUp(event);
        } catch (IOException ex) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện đăng ký không thành công!");
        }
    }
}