package com.mycompany.Controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.LoginAction;
import com.mycompany.exception.Login.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Quan trọng
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker; // Để điều khiển ô chọn ngày

import java.io.IOException;
import java.time.LocalDate;               // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.time.*;
import com.mycompany.utils.*;

public class SignUpController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker datePicker;

    @FXML
    public void signUp(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase(); // email chữ thường hết
        String password = passwordField.getText().trim();
        LocalDate date = datePicker.getValue();
        LoginAction.getInstance().dangKy(event, name, email, password, date);
    }

    @FXML
    public void goToSignIn(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện đăng nhập không thành công!");
        }
    }
}