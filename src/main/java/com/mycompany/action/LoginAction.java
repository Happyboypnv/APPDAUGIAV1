package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Quan trọng
import java.time.LocalDate;               // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import java.io.IOException; // Quan trọng
import java.time.*;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.utils.TokenUtil;
import com.mycompany.utils.SessionManager;
import com.mycompany.exception.Login.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*; // Handle nhieu nguoi dang nhap cung luc

public class LoginAction {
    private static LoginAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();
    private final Lock lock = new ReentrantLock();
    private LoginAction() {};

    public static LoginAction getInstance() {
        if (instance==null) {
            instance = new LoginAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec dang ky va dang nhap cho do ton tai nguyen, tranh xung dot
    }
    
    private void checkSignUp(String name, String email, String password, LocalDate birthDate) throws UserNameException,EmailException,PasswordException,DateException {
        if(name == null || name.isEmpty()) throw new UserNameException("Tên đang bỏ trống!");
        String nameRegex = "^[\\p{L} .'-]{2,30}$";
        // p{L} : cho phep ngon ngu tieng Viet
        //  .'- : cho phep dau cach, dau cham, dau nhay don, dau gach ngang
        // {2, 30} : do dai trong khoang thu 2 den 30
        if(!name.matches(nameRegex)) throw new UserNameException("Tên không hợp lệ");
        // check xem chuoi name co matches voi luat Regex khong
        
        if(email == null || email.isEmpty()) throw  new EmailException("Email đang bỏ trống!");
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]+$";
        // email co dang la nguyenvana @ gmail . com
        // dau cong dang sau moi [] la yeu cau moi phan co it nhat mot ky tu
        if(!email.matches( emailRegex )) throw new EmailException("Email không hợp lệ!");
        if(!khoLuuTruNguoiDung.kiemTraEmail(email)) throw new EmailException("Email đã tồn tại!");

        if(password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!#%*?&])[A-Za-z0-9@$!#%*?&]{8,}$";
        if(!password.matches( passwordRegex )) throw new PasswordException("Mat khẩu không hợp lệ!");
        // ?=.*[A-Z] check nhanh co it nhat mot ki tu A-Z khong
        // ?=.*[a-z] check nhanh co it nhat mot ki tu a-z khong
        // ?=.*[0-9] check nhanh co it nhat mot ki tu 0-9 khong
        // ?=.*[@$!#%*?&] check nhanh co it nhat mot ki tu dac biet khong
        // [A-za-z0-9@$!#%*?&] cac ki tu co the duoc phep su dung trong mat khau
        // {8,} co it nhat 8 ki tu

        if(birthDate == null) throw new DateException("Ngày sinh đang bỏ trống!");
        if(birthDate.isAfter(LocalDate.now())) throw new DateException("Ngày sinh không hợp lệ!");
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if(age < 18) throw new DateException("Bạn chưa đủ 18 tuổi");
    }

    private void checkSignIn(String email,String password) throws EmailException,PasswordException, UserException {
        if(email == null || email.isEmpty()) throw new EmailException("Email đang bỏ trống!");
        if(password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        if (!khoLuuTruNguoiDung.kiemTraNguoiDung(email, password)) throw new UserException("Sai email hoặc mật khẩu");
    }

    @FXML
    public void dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) { // Thử lấy lock trong 0.5s, nếu không được sẽ trả về false và không thực hiện đăng ký
                try {
                    checkSignUp(name, email, password, birthdate); // check thông tin đky
                    String birth = birthdate.toString();
                    NguoiDung nguoiDung = new NguoiDung(name, email, password, birth);
                    khoLuuTruNguoiDung.luu(nguoiDung); // lưu người dùng (sau lưu vào db)
                    HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công tài khoản!");
                    try {
                        HandleNavigationAndAlert.getInstance().handleGoToSignIn(event); // chuyển sang sign In
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (UserNameException | EmailException | PasswordException | DateException e) {
                    HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", e.getMessage());
                } finally {
                    lock.unlock(); // Đảm bảo luôn giải phóng lock sau khi hoàn thành công việc
                }
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Hệ thống đang bận, vui lòng thử lại sau!");
            }
        } catch (InterruptedException e)  {
            Thread.currentThread().interrupt();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đăng ký bị gián đoạn, vui lòng thử lại!");
        } catch (Exception e) {
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", "Đã xảy ra lỗi không xác định, vui lòng thử lại!");
        }
    }

    @FXML
    public void dangNhap(ActionEvent event, String email, String password) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) { // Thử lấy lock trong 0.5s, nếu không được sẽ trả về false và không thực hiện đăng nhập
                try {
                    checkSignIn(email,password);
                    // Lấy thông tin người dùng và tạo token
                    KhoLuuTruNguoiDungJson storage = (KhoLuuTruNguoiDungJson) khoLuuTruNguoiDung;
                    NguoiDung user = storage.layNguoiDungTheoEmail(email);
                    String token = TokenUtil.generateToken(user);
                    SessionManager.getInstance().setSession(user, token);

                    HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công! Chào mừng bạn.");
                    System.out.println("Đăng nhập thành công với email: " + email);
                    System.out.println("Token: " + token);
                    // Chuyen qua giao dien Home luon
                    try {
                        HandleNavigationAndAlert.getInstance().handleGoToHome(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                        HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện trang chủ không thành công!");
                    }
                } catch (UserException | EmailException | PasswordException e) {
                    HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", e.getMessage());
                } finally {
                    lock.unlock(); // Đảm bảo luôn giải phóng lock sau khi hoàn thành công việc
                }
            } else {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Hệ thống đang bận, vui lòng thử lại sau!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đăng nhập bị gián đoạn, vui lòng thử lại!");
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", "Đã xảy ra lỗi không xác định, vui lòng thử lại!");
        }
    }

}