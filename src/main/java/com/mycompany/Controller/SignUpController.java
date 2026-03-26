package com.mycompany.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Quan trọng
import javafx.fxml.FXMLLoader; // Quan trọng
import javafx.scene.Node; // Quan trọng
import javafx.scene.Parent; // Quan trọng
import javafx.scene.Scene; // Quan trọng
import javafx.scene.control.DatePicker; // Để điều khiển ô chọn ngày
import java.time.LocalDate;               // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage; // Quan trọng
import java.io.IOException; // Quan trọng

public class SignUpController {

    // Thêm @FXML để kết nối với Scene Builder
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dateOfBirthPicker;

    @FXML
    public void handleSignUp(ActionEvent actionEvent) { // actionEvent ở đây
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        // Lấy giá trị ngày dưới dạng đối tượng LocalDate
        LocalDate birthDate = dateOfBirthPicker.getValue();

// Kiểm tra nếu người dùng chưa chọn ngày
        if (birthDate == null) {
            showError("Vui lòng chọn ngày sinh!");
            return;
        }

// Ví dụ: In ra máy tính để xem kết quả
        System.out.println("Ngày sinh đã chọn: " + birthDate.toString());
        // 1. Kiểm tra rỗng
        if(name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill all the fields");
            return;
        }

        // 2. Kiểm tra độ dài mật khẩu
        if(password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // 3. Kiểm tra tên không chứa số
        if(name.matches(".*\\d.*")){
            showError("Name cannot contain numbers!");
            return;
        }

        // 4. Chuyển cảnh nếu mọi thứ hợp lệ
        try {
            // FileWriter với tham số 'true' có nghĩa là: Ghi tiếp nối (Append) vào cuối file, không xóa dữ liệu cũ
            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter("users.txt", true));

            // Ghi định dạng: email,password
            writer.write(email + "," + password);
            writer.newLine(); // Xuống dòng cho tài khoản tiếp theo
            writer.close();   // Nhớ đóng file sau khi ghi xong

        } catch (Exception e) {
            showError("Lỗi hệ thống: Không thể lưu tài khoản!");
            return; // Dừng lại không chuyển cảnh nếu bị lỗi lưu file
        }
        try {
            // Cách 1: Nếu file FXML cùng thư mục với file App.class khi biên dịch
            Parent root = FXMLLoader.load(getClass().getResource("/SignIn.fxml"));

// Cách 2 (Khuyên dùng): Nếu file FXML nằm ở thư mục resources gốc
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi chuyển cảnh: " + e.getMessage());
            showError("Could not load SignIn screen!");
        }
    }
    @FXML
    public void handleGoToSignIn(ActionEvent event) {
        try {
            // 1. Nạp file giao diện SignIn.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/SignIn.fxml"));

            // 2. Lấy ra cửa sổ (Stage) hiện tại đang mở
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            // 3. Đặt giao diện mới lên cửa sổ
            Scene scene = new Scene(root);
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            System.err.println("Lỗi khi chuyển sang màn hình Đăng nhập: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hàm hỗ trợ hiện lỗi cho ngắn gọn code
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}