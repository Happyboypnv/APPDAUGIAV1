package com.mycompany.Controller;

import com.mycompany.exception.Login.*;
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
    public void checkName(String name) throws UserNameException {
        if(name == null || name.isEmpty()) throw new UserNameException("Tên đang bỏ trống!");
        String nameRegex = "^[\\p{L} .'-]{2,30}$";
        // p{L} : cho phep ngon ngu tieng Viet
        //  .'- : cho phep dau cach, dau cham, dau nhay don, dau gach ngang
        // {2, 30} : do dai trong khoang thu 2 den 30
        if(!name.matches(nameRegex)) throw new UserNameException("Tên không hợp lệ");
        // check xem chuoi name co matches voi luat Regex khong
    }
    public void checkEmail(String email)throws EmailException {
        if(email == null || email.isEmpty()) throw  new EmailException("Email đang bỏ trống!");
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$";
        // email co dang la nguyenvana @ gmail . com
        // dau cong dang sau moi [] la yeu cau moi phan co it nhat mot ky tu
        if(!email.matches( emailRegex )) throw new EmailException("Email không hợp lệ!");
        if(!khoLuuTruNguoiDung.kiemTraEmail(email)) throw new EmailException("Email đã tồn tại!");
    }
    public void checkUser(String email, String password) throws UserException {
        if(khoLuuTruNguoiDung.kiemTraNguoiDung(email, password)) throw new UserException("Tài khoản đã tồn tại!");
    }
    public  void checkPassword(String password)throws PasswordException {
        if(password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-za-z0-9@$!#%*?&]{8,}$";
       if(!password.matches( passwordRegex )) throw new PasswordException("Mat khẩu không hợp lệ!");
        // ?=.*[A-Z] check nhanh co it nhat mot ki tu A-Z khong
        // ?=.*[a-z] check nhanh co it nhat mot ki tu a-z khong
        // ?=.*[0-9] check nhanh co it nhat mot ki tu 0-9 khong
        // ?=.*[@$!#%*?&] check nhanh co it nhat mot ki tu dac biet khong
        // [A-za-z0-9@$!#%*?&] cac ki tu co the duoc phep su dung trong mat khau
        // {8,} co it nhat 8 ki tu
    }
    public void checkDate(LocalDate date) throws DateException {
        if(date == null) throw new DateException("Ngày sinh đang bỏ trống!");
        if(date.isAfter(LocalDate.now())) throw new DateException("Ngày sinh không hợp lệ!");
        int age = Period.between(date, LocalDate.now()).getYears();
        if(age < 18) throw new DateException("Bạn chưa đủ 18 tuổi");
    }
    public void handleSignUp(ActionEvent event) {
        String password = null;
        try {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            password = passwordField.getText().trim();
            LocalDate date = datePicker.getValue();
            checkName(name);
            checkEmail(email);
            checkPassword(password);
            checkDate(date);
            String birth = datePicker.getValue().toString();
            NguoiDung nguoiDung = new NguoiDung(name, email, password, birth);
            khoLuuTruNguoiDung.luu(nguoiDung);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công tài khoản!");
            try {
                handleGoToSignIn(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UserNameException | EmailException | PasswordException | DateException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", e.getMessage());
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