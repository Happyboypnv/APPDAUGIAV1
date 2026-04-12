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
import java.time.*;

import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.models.NguoiDung;
import com.mycompany.utils.*;
public class SignUpController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker datePicker;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();
    public boolean checkName(String name){
        if(name == null || name.isEmpty()) return false;
        String nameRegex = "^[\\p{L} .'-]{2,30}$";
        // p{L} : cho phep ngon ngu tieng Viet
        //  .'- : cho phep dau cach, dau cham, dau nhay don, dau gach ngang
        // {2, 30} : do dai trong khoang thu 2 den 30
        return  name.matches(nameRegex);
        // check xem chuoi name co matches voi luat Regex khong
    }
    public boolean checkEmail(String email){
        if(email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$";
        // email co dang la nguyenvana @ gmail . com
        // dau cong dang sau moi [] la yeu cau moi phan co it nhat mot ky tu
        return  email.matches( emailRegex );
    }
    public  boolean checkPassword(String password){
        if(password == null || password.isEmpty()) return false;
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-za-z0-9@$!#%*?&]{8,}$";
        return  password.matches( passwordRegex );
        // ?=.*[A-Z] check nhanh co it nhat mot ki tu A-Z khong
        // ?=.*[a-z] check nhanh co it nhat mot ki tu a-z khong
        // ?=.*[0-9] check nhanh co it nhat mot ki tu 0-9 khong
        // ?=.*[@$!#%*?&] check nhanh co it nhat mot ki tu dac biet khong
        // [A-za-z0-9@$!#%*?&] cac ki tu co the duoc phep su dung trong mat khau
        // {8,} co it nhat 8 ki tu
    }
    public boolean checkDate(LocalDate date){
        if(date == null) return false;
        if(date.isAfter(LocalDate.now())) return false;
        int age = Period.between(date, LocalDate.now()).getYears();
        return age >= 18;
    }
    public void handleSignUp(ActionEvent event) {
        String name = nameField.getText().trim(); /// cat het khoang trong di
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        LocalDate date = datePicker.getValue();
        if(!checkName(name)) {
            showAlert(Alert.AlertType.WARNING,"Lỗi đăng ký", "Tên chưa hợp lệ!");
            return;
        }
        if(!checkEmail(email)){
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Email chưa hợp lệ!");
            return;
        }
        if(!checkPassword(password)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Mật khẩu chưa hợp lệ!");
            return;
        }
        if(!checkDate(date)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Ngày sinh chưa hợp lệ!");
            return;
        }
        if(khoLuuTruNguoiDung.kiemTraEmail(email) == false) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Email đã tồn tại!");
            return;
        }
        String birth = datePicker.getValue().toString();
        NguoiDung nguoiDung = new NguoiDung(name, email, password, birth);
        khoLuuTruNguoiDung.luu(nguoiDung);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công tài khoản!");
        try{
            handleGoToSignIn(event);
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }
    @FXML
    public void handleGoToSignIn(ActionEvent event) throws IOException {
        // 1. Tải file FXML của màn hình Sign Up
        // LƯU Ý: Thay "SignUp.fxml" bằng tên file FXML thực tế của bạn
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));

        // 2. Tạo một Scene (cảnh) mới chứa giao diện Sign Up
        Scene signUpScene = new Scene(signUpRoot);

        // 3. Lấy Stage (cửa sổ ứng dụng hiện tại) từ sự kiện click chuột
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 4. Đặt Scene mới lên Stage và hiển thị
        window.setScene(signUpScene);
        window.show();
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType); // 1. Khởi tạo một hộp thoại mới
        alert.setTitle(title);              // 2. Đặt tiêu đề cho cửa sổ Pop-up
        alert.setHeaderText(null);          // 3. Tiêu đề phụ (để null cho gọn)
        alert.setContentText(message);      // 4. Nội dung chi tiết cần thông báo
        alert.showAndWait();                // 5. Hiển thị lên và dừng code lại
    }
}