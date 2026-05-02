package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import com.mycompany.utils.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Quan trọng
import java.time.LocalDate;               // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import java.io.IOException; // Quan trọng
import java.time.*;

import com.mycompany.exception.Login.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*; // Handle nhieu nguoi dang nhap cung luc

/**
 * LoginAction - Class xử lý đăng ký và đăng nhập người dùng
 *
 * MỤC ĐÍCH:
 * - Quản lý toàn bộ logic đăng ký (sign up) và đăng nhập (sign in)
 * - Validate thông tin đầu vào
 * - Tạo và xác thực JWT token
 * - Đảm bảo thread-safe cho nhiều người dùng cùng lúc
 *
 * KẾT NỐI VỚI CONTROLLERS:
 * - SignInController gọi LoginAction.dangNhap() khi submit form đăng nhập
 * - SignUpController gọi LoginAction.dangKy() khi submit form đăng ký
 * - Sau đăng nhập thành công → chuyển đến HomeController
 * - Sau đăng ký thành công → chuyển đến SignInController
 *
 * TÍNH NĂNG CHÍNH:
 * - Đăng ký: Validate input, tạo user mới, lưu database
 * - Đăng nhập: Kiểm tra credentials, tạo session + token
 * - Validation: Regex patterns cho email, password, phone, name
 * - Thread-safe: ReentrantLock để tránh race condition
 * - Error handling: Custom exceptions với thông báo rõ ràng
 *
 * DESIGN PATTERN:
 * - Singleton: Chỉ có 1 instance LoginAction
 * - Strategy: Sử dụng IKhoLuuTruNguoiDung interface
 * - Exception handling: Custom exceptions cho từng loại lỗi
 */
public class LoginAction {
    private static LoginAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();
    private final Lock lock = new ReentrantLock();
    private LoginAction() {};

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance LoginAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của LoginAction
     */
    public static LoginAction getInstance() {
        if (instance==null) {
            instance = new LoginAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec dang ky va dang nhap cho do ton tai nguyen, tranh xung dot
    }

    /**
     * checkSignUp(String name, String email, String password, LocalDate birthDate) - Validate thông tin đăng ký
     *
     * MỤC ĐÍCH:
     * - Kiểm tra tất cả thông tin đăng ký có hợp lệ không
     * - Throw custom exceptions nếu có lỗi
     *
     * VALIDATION RULES:
     * - Name: 2-30 ký tự, hỗ trợ tiếng Việt, space, dấu
     * - Email: Format chuẩn, chưa tồn tại trong hệ thống
     * - Password: 8+ ký tự, ít nhất 1 hoa, 1 thường, 1 số, 1 đặc biệt
     * - BirthDate: Không null, không phải tương lai, >= 18 tuổi
     *
     * @param name Họ tên người dùng
     * @param email Email đăng ký
     * @param password Mật khẩu
     * @param birthDate Ngày sinh
     * @throws UserNameException nếu tên không hợp lệ
     * @throws EmailException nếu email không hợp lệ/đã tồn tại
     * @throws PasswordException nếu mật khẩu không hợp lệ
     * @throws DateException nếu ngày sinh không hợp lệ
     */
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

    /**
     * checkSignIn(String email, String password) - Validate thông tin đăng nhập
     *
     * MỤC ĐÍCH:
     * - Kiểm tra thông tin đăng nhập có hợp lệ không
     * - Throw custom exceptions nếu có lỗi
     *
     * VALIDATION RULES:
     * - Email: Không được null/empty
     * - Password: Không được null/empty
     * - Credentials: Phải khớp với database
     *
     * @param email Email đăng nhập
     * @param password Mật khẩu
     * @throws EmailException nếu email null/empty
     * @throws PasswordException nếu password null/empty
     * @throws UserException nếu email/password không khớp
     */
    private void checkSignIn(String email,String password) throws EmailException,PasswordException, UserException {
        if(email == null || email.isEmpty()) throw new EmailException("Email đang bỏ trống!");
        if(password == null || password.isEmpty()) throw new PasswordException("Mật khẩu đang bỏ trống!");
        if (!khoLuuTruNguoiDung.kiemTraNguoiDung(email, password)) throw new UserException("Sai email hoặc mật khẩu");
    }

    /**
     * dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) - Xử lý đăng ký
     *
     * THAY ĐỔI QUAN TRỌNG (SQLite Migration):
     * - Trước: NguoiDung nguoiDung = new NguoiDung(name, email, password, birthdate.toString())
     *          → Lưu password plain text
     * - Sau: Tạo salt + hash password trước khi tạo NguoiDung object
     *
     * Quy trình mới:
     * 1. Validate thông tin đăng ký (name, email, password, birthdate)
     * 2. Tạo salt ngẫu nhiên (16 bytes Base64)
     * 3. Hash password với salt (SHA-256)
     * 4. Tạo NguoiDung object với password đã hash + salt
     * 5. Lưu vào database
     * 6. Thông báo thành công + chuyển sang trang đăng nhập
     *
     * Bảo mật được cải thiện:
     * - Rainbow table attack: Không thể vì mỗi user có salt khác nhau
     * - Brute force: Phải crack từng hash riêng biệt
     * - Dictionary attack: Salt làm cho dictionary attack kém hiệu quả
     *
     * @param event ActionEvent từ button click
     * @param name Họ tên
     * @param email Email
     * @param password Mật khẩu (plain text từ form)
     * @param birthdate Ngày sinh
     */
    @FXML
    public void dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) { // Thử lấy lock trong 0.5s, nếu không được sẽ trả về false và không thực hiện đăng ký
                try {
                    checkSignUp(name, email, password, birthdate); // check thông tin đky

                    // THAY ĐỔI QUAN TRỌNG: Hash password với salt trước khi lưu
                    // Lý do: Bảo mật - không lưu plain text password
                    // Cách hoạt động:
                    // 1. BoMaHoaMatKhau.taoSalt() → tạo 16 bytes random, encode Base64
                    // 2. BoMaHoaMatKhau.maHoaMatKhau(password, salt) → SHA-256 hash
                    // 3. Tạo NguoiDung với password đã hash và salt
                    String salt = BoMaHoaMatKhau.taoSalt();
                    String hashedPassword = BoMaHoaMatKhau.maHoaMatKhau(password, salt);
                    NguoiDung nguoiDung = new NguoiDung(name, email, hashedPassword, birthdate.toString());
                    nguoiDung.setSalt(salt);  // THAY ĐỔI: Set salt vào NguoiDung object

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

    /**
     * dangNhap(ActionEvent event, String email, String password) - Xử lý đăng nhập
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ SignInController khi submit form đăng nhập
     *
     * QUY TRÌNH:
     * 1. Thử lấy lock trong 500ms (thread-safe)
     * 2. Validate thông tin đăng nhập
     * 3. Lấy thông tin user từ database
     * 4. Tạo JWT token
     * 5. Set session trong SessionManager
     * 6. Thông báo thành công + log
     * 7. Chuyển sang trang Home
     *
     * THREAD-SAFE:
     * - Sử dụng tryLock() để tránh block
     * - Unlock trong finally block
     *
     * XỬ LÝ LỖI:
     * - Validation errors → Custom exceptions → Alert warnings
     * - Lock timeout → Alert system busy
     * - InterruptedException → Thread interrupt + alert
     * - IOException khi navigation → Print stack trace + alert
     * - Other exceptions → Generic error alert
     *
     * @param event ActionEvent từ button click
     * @param email Email đăng nhập
     * @param password Mật khẩu
     */
    @FXML
    public void dangNhap(ActionEvent event, String email, String password) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) { // Thử lấy lock trong 0.5s, nếu không được sẽ trả về false và không thực hiện đăng nhập
                try {
                    checkSignIn(email,password);
                    // Lấy thông tin người dùng và tạo token
                    KhoLuuTruNguoiDungSQLite storage = (KhoLuuTruNguoiDungSQLite) khoLuuTruNguoiDung;
                    NguoiDung user = storage.layTatCa().get(email); // Lấy thông tin user từ database
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
            e.printStackTrace();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", "Đã xảy ra lỗi không xác định, vui lòng thử lại!");
        }
    }

}