package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.utils.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.time.LocalDate;
import javafx.scene.control.Alert;
import java.io.IOException;
import java.time.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class LoginAction {
    private final IUserRepository khoLuuTruNguoiDung = new UserRepositorySQLite();
    private final Lock lock = new ReentrantLock();

    private LoginAction() {}

    private static volatile LoginAction instance;

    public static LoginAction getInstance() {
        if (instance == null) {
            synchronized (LoginAction.class) {
                if (instance == null) instance = new LoginAction();
            }
        }
        return instance;
    }

    // ================================================================
    // VALIDATE ĐĂNG KÝ (chỉ validate phía client, server validate lại)
    // ================================================================
    private void checkSignUp(String name, String email, String password, LocalDate birthDate)
            throws UserNameException, EmailException, PasswordException, DateException {

        if (name == null || name.isEmpty()) throw new UserNameException("Tên đang bỏ trống!");
        String nameRegex = "^[\\p{L} .'-]{2,30}$";
        if (!name.matches(nameRegex)) throw new UserNameException("Tên không hợp lệ!");

        if (email == null || email.isEmpty()) throw new EmailException("Email đang bỏ trống!");
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$";
        if (!email.matches(emailRegex)) throw new EmailException("Email không hợp lệ!");

        if (password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-Za-z0-9@$!#%*?&]{8,}$";
        if (!password.matches(passwordRegex))
            throw new PasswordException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!#%*?&)!");

        if (birthDate == null) throw new DateException("Ngày sinh đang bỏ trống!");
        if (birthDate.isAfter(LocalDate.now())) throw new DateException("Ngày sinh không hợp lệ!");
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) throw new DateException("Bạn chưa đủ 18 tuổi!");
    }
    private void checkSignIn(String email, String password) throws EmailException, PasswordException {
        if (email == null || email.isEmpty()) throw new EmailException("Email đang bỏ trống!");
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$";
        if (!email.matches(emailRegex)) throw new EmailException("Email không hợp lệ!");

        if (password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-Za-z0-9@$!#%*?&]{8,}$";
        if (!password.matches(passwordRegex))
            throw new PasswordException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!#%*?&)!");
    }

    // ================================================================
    // ĐĂNG KÝ — gọi server qua ApiClient
    // ================================================================
    @FXML
    public void dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    // Bước 1: Validate phía client trước (nhanh, không cần network)
                    checkSignUp(name, email, password, birthdate);

                    // Bước 2: Gọi server để đăng ký
                    // Server sẽ tự hash password + lưu DB
                    LoginResponse response = ApiClient.register(
                            name,
                            email,
                            password,            // gửi plain text, server tự hash
                            birthdate.toString() // format: "2000-01-15"
                    );

                    // Bước 3: Kiểm tra kết quả từ server
                    if (response == null) {
                        HandleNavigationAndAlert.getInstance().showAlert(
                                Alert.AlertType.ERROR, "Lỗi kết nối",
                                "Không kết nối được server (localhost:8080). Hãy chắc chắn ServerApp đang chạy!");
                        return;
                    }

                    // Server báo lỗi (email trùng, thiếu field...)
                    if (!response.getThongBao().equals("Đăng ký thành công")) {
                        HandleNavigationAndAlert.getInstance().showAlert(
                                Alert.AlertType.WARNING, "Lỗi đăng ký", response.getThongBao());
                        return;
                    }

                    // Bước 4: Đăng ký thành công — chuyển sang trang đăng nhập
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.INFORMATION, "Thành công",
                            "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
                    try {
                        HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (UserNameException | EmailException | PasswordException | DateException e) {
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.WARNING, "Lỗi đăng ký", e.getMessage());
                } finally {
                    lock.unlock();
                }
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi hệ thống",
                        "Hệ thống đang bận, vui lòng thử lại sau!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi hệ thống",
                    "Đăng ký bị gián đoạn, vui lòng thử lại!");
        } catch (Exception e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi không xác định",
                    "Đã xảy ra lỗi không xác định: " + e.getMessage());
        }
    }

    // ================================================================
    // ĐĂNG NHẬP — gọi server qua ApiClient
    // ================================================================
    @FXML
    public void dangNhap(ActionEvent event, String email, String password) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                   checkSignIn(email, password);

                    // Gọi server xác thực
                    LoginResponse response = ApiClient.login(email, password);

                    if (response == null) {
                        throw new UserException(
                                "Không kết nối được server (localhost:8080). Hãy chắc chắn ServerApp đang chạy!");
                    }

                    if (response.getToken() == null) {
                        throw new UserException(response.getThongBao() != null
                                ? response.getThongBao() : "Sai email hoặc mật khẩu!");
                    }

                    // Lấy thông tin user từ DB local để tạo object NguoiDung
                    NguoiDung user = khoLuuTruNguoiDung.layTheoEmail(email);

                    // Token local (Base64) — dùng để đọc thông tin profile, finance...
                    String localToken = TokenUtil.generateToken(user);

                    // Lưu session
                    SessionManager.getInstance().setSession(user, localToken);

                    // Lưu server token (USER_...) — dùng để gọi API tạo phiên, đặt giá
                    SessionManager.getInstance().setServerToken(response.getToken());

                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.INFORMATION, "Thành công",
                            "Đăng nhập thành công! Chào mừng " + user.layHoTen() + ".");

                    try {
                        HandleNavigationAndAlert.getInstance().handleGoToHome(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (UserException | EmailException | PasswordException e) {
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.WARNING, "Lỗi đăng nhập", e.getMessage());
                } finally {
                    lock.unlock();
                }
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi hệ thống",
                        "Hệ thống đang bận, vui lòng thử lại sau!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}