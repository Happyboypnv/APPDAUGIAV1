package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import com.mycompany.server.dto.LoginResponse;
import com.mycompany.utils.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; // Quan trọng
import java.time.LocalDate;               // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import java.io.IOException; // Quan trọng
import java.time.*;


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
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();
    private final Lock lock = new ReentrantLock();
    private LoginAction() {};

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance LoginAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của LoginAction
     */
    private static volatile LoginAction instance;
    public static LoginAction getInstance() {
        if (instance == null) {
            synchronized (LoginAction.class) {
                if (instance == null) instance = new LoginAction();
            }
        }
        return instance;
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
     * dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) - Xử lý đăng ký (Client-Server)
     *
     * ARCHITECTURE: Client-Server for Registration
     * ====================================================
     * SAU (Client-Server):
     * - User fills form on Computer A
     * - Data sent to Server (HTTP POST /api/users/register)
     * - Server creates account + hashes password + saves to server DB
     * - Computer A saves locally for offline access
     * - Same user on Computer B → Server has account → Can login
     * - Both computers sync from server, local DB is backup only
     *
     * QUY TRÌNH:
     * 1. Validate input (name, email, password, birthdate on client side)
     * 2. GỌI SERVER: ApiClient.register() → POST /api/users/register
     * 3. Server xử lý:
     *    - Check email không trùng
     *    - Hash password + create salt
     *    - Save user to server database
     *    - Return LoginResponse {token, thongBao}
     * 4. Nếu server return thành công → Save locally as backup
     * 5. Thông báo thành công + chuyển sang đăng nhập
     *
     * THREAD-SAFE:
     * - tryLock(500ms) để tránh concurrent signup race condition
     * - unlock() trong finally block
     *
     * XỬ LÝ LỖI:
     * - Validation errors → Custom exceptions
     * - Email already exists → Server trả lỗi
     * - Network error → "Không kết nối được server"
     * - Lock timeout → "Hệ thống đang bận"
     *
     * @param event ActionEvent từ Sign Up button
     * @param name Họ tên
     * @param email Email (sẽ được server check duplicate)
     * @param password Mật khẩu plain text
     * @param birthdate Ngày sinh
     */
    @FXML
    public void dangKy(ActionEvent event, String name, String email, String password, LocalDate birthdate) {
        try {
            // 🔹 BƯỚC 1: Thử lấy lock để tránh race condition
            if (!lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi hệ thống",
                        "Hệ thống đang bận, vui lòng thử lại sau!");
                return;
            }

            try {
                // 🔹 BƯỚC 2: Validate thông tin đăng ký (client-side validation)
                checkSignUp(name, email, password, birthdate);

                // 🔹 BƯỚC 3: GỌI SERVER để tạo account (Client-Server Architecture)
                // ApiClient.register() sẽ:
                // - POST name, email, password, birthdate đến /api/users/register trên server
                // - Server validate email không trùng
                // - Server hash password + tạo salt
                // - Server save user vào server database (SQLite)
                // - Server return LoginResponse {token, thongBao}
                //
                // QUAN TRỌNG: Nếu success → User có thể login từ bất kỳ computer nào
                System.out.println("[LoginAction] 📤 Sending signup request to server...");
                LoginResponse response = ApiClient.register(
                        name.trim(),
                        email.trim(),
                        password,
                        birthdate.toString()  // Format: YYYY-MM-DD
                );

                // 🔹 BƯỚC 4: Kiểm tra response từ server
                if (response == null) {
                    // Lỗi kết nối mạng (ApiClient đã log chi tiết)
                    throw new UserException("Không thể kết nối đến server. Kiểm tra IP/Port server.");
                }

                // 🔹 BƯỚC 5: Kiểm tra xem server accept signup hay không
                String thongBao = response.getThongBao();
                if (thongBao == null || thongBao.isEmpty()) {
                    thongBao = "Lỗi từ server: Không có phản hồi";
                }

                // Nếu server trả lỗi (email đã tồn tại, password không hợp lệ, etc)
                // → thongBao sẽ chứa chi tiết lỗi
                boolean isSuccess = response.getToken() != null && !response.getToken().trim().isEmpty();

                if (!isSuccess) {
                    // Server từ chối đăng ký - hiển thị lỗi từ server
                    System.err.println("[LoginAction] ❌ Server rejected signup: " + thongBao);
                    throw new UserException(thongBao);
                }

                System.out.println("[LoginAction] ✅ Server accepted signup successfully");

                // 🔹 BƯỚC 6: Signup thành công trên server
                // Giờ lưu local copy để offline access (optional nhưng tốt cho UX)
                // Password sẽ lưu dưới dạng hash (không plain text)
                try {
                    String salt = BoMaHoaMatKhau.taoSalt();
                    String hashedPassword = BoMaHoaMatKhau.maHoaMatKhau(password, salt);
                    NguoiDung localUser = new NguoiDung(name.trim(), email.trim(), hashedPassword, birthdate.toString());
                    localUser.setSalt(salt);
                    khoLuuTruNguoiDung.luu(localUser);
                    System.out.println("[LoginAction] 💾 Account also saved to local DB for offline access");
                } catch (Exception e) {
                    // Local save failed but server save succeeded
                    // This is OK - user can still login, just won't have offline access
                    System.err.println("[LoginAction] ⚠️ Local save failed (not critical): " + e.getMessage());
                }

                // 🔹 BƯỚC 7: Hiển thị thông báo thành công
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.INFORMATION, "Thành công",
                        "Đăng ký thành công! Tài khoản có thể dùng trên bất kỳ thiết bị nào.");

                // 🔹 BƯỚC 8: Chuyển hướng sang trang đăng nhập
                try {
                    HandleNavigationAndAlert.getInstance().handleGoToSignIn(event);
                } catch (IOException e) {
                    System.err.println("[LoginAction] ❌ Navigation error: " + e.getMessage());
                    e.printStackTrace();
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.ERROR, "Lỗi giao diện",
                            "Không thể chuyển sang trang đăng nhập. Vui lòng thử lại.");
                }

            } catch (UserNameException e) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.WARNING, "Lỗi tên", e.getMessage());

            } catch (EmailException e) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.WARNING, "Lỗi email", e.getMessage());

            } catch (PasswordException e) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.WARNING, "Lỗi mật khẩu", e.getMessage());

            } catch (DateException e) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.WARNING, "Lỗi ngày sinh", e.getMessage());

            } catch (UserException e) {
                // Server error hoặc network error
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.WARNING, "Lỗi đăng ký", e.getMessage());

            } catch (Exception e) {
                System.err.println("[LoginAction] ❌ Signup error: " + e.getMessage());
                e.printStackTrace();
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi không xác định",
                        "Đã xảy ra lỗi: " + e.getMessage());

            } finally {
                // Luôn giải phóng lock
                lock.unlock();
            }

        } catch (InterruptedException e) {
            // Lock bị interrupt
            System.err.println("[LoginAction] ❌ Signup interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi hệ thống",
                    "Đăng ký bị gián đoạn. Vui lòng thử lại.");
        }
    }

    /**
     * dangNhap(ActionEvent event, String email, String password) - Xử lý đăng nhập (Client-Server)
     *
     * ARCHITECTURE: Client-Server Model
     * - Client (JavaFX): Gửi email/password → nhận token
     * - Server (HTTP): Xác thực credentials → trả token + user info
     * - Client không bao giờ trực tiếp truy cập DB (tất cả qua server)
     *
     * QUY TRÌNH:
     * 1. Validate input (email/password không null/empty)
     * 2. Gọi ApiClient.login() → Server xác thực credentials
     * 3. Server trả LoginResponse với:
     *    - token: JWT token để dùng cho các request sau
     *    - thongBao: Thông báo lỗi nếu fail
     * 4. Nếu token != null → Đăng nhập thành công
     * 5. Lấy user info từ server (hoặc từ response)
     * 6. Lưu token + user vào SessionManager
     * 7. Chuyển hướng sang Home
     *
     * THREAD-SAFE:
     * - tryLock(500ms) để tránh concurrent login race condition
     * - unlock() trong finally block
     *
     * XỬ LÝ LỖI:
     * - Null input → throw EmailException/PasswordException
     * - Server error → thongBao từ response
     * - Network error → "Không kết nối được server"
     * - Lock timeout → "Hệ thống đang bận"
     * - Navigation error → catch IOException
     *
     * @param event ActionEvent từ Login button
     * @param email Email person
     * @param password Mật khẩu plain text (sẽ được server hash)
     */
//    @FXML
//    public void dangNhap(ActionEvent event, String email, String password) {
//        try {
//            // 🔹 BƯỚC 1: Thử lấy lock để tránh race condition (timeout 500ms)
//            if (!lock.tryLock(500, TimeUnit.MILLISECONDS)) {
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.ERROR, "Lỗi hệ thống",
//                        "Hệ thống đang bận, vui lòng thử lại sau!");
//                return;
//            }
//
//            try {
//                // 🔹 BƯỚC 2: Validate input (basic null/empty check)
//                if (email == null || email.trim().isEmpty()) {
//                    throw new EmailException("Email đang bỏ trống!");
//                }
//                if (password == null || password.trim().isEmpty()) {
//                    throw new PasswordException("Mật khẩu đang bỏ trống!");
//                }
//
//                // 🔹 BƯỚC 3: GỌISERVER để xác thực (Client-Server Architecture)
//                // ApiClient.login() sẽ:
//                // - POST email + password đến /api/users/login trên server
//                // - Server xác thực credentials (so sánh hash password)
//                // - Server trả LoginResponse chứa token (nếu thành công)
//                LoginResponse response = ApiClient.login(email.trim(), password);
//
//                // 🔹 BƯỚC 4: Kiểm tra response từ server
//                if (response == null) {
//                    // Lỗi kết nối mạng (ApiClient đã log chi tiết)
//                    throw new UserException("Không thể kết nối đến server. Kiểm tra IP/Port server.");
//                }
//
//                // 🔹 BƯỚC 5: Kiểm tra token (token != null = đăng nhập thành công)
//                String token = response.getToken();
//                if (token == null || token.trim().isEmpty()) {
//                    // Đăng nhập thất bại - lỗi từ server
//                    String errorMsg = response.getThongBao();
//                    if (errorMsg == null || errorMsg.isEmpty()) {
//                        errorMsg = "Sai email hoặc mật khẩu. Vui lòng thử lại.";
//                    }
//                    throw new UserException(errorMsg);
//                }
//
//                // 🔹 BƯỚC 6: Đăng nhập thành công - Lấy thông tin user
//                // Có 2 cách:
//                // Cách A: Lấy user info từ response server (nếu server trả)
//                // Cách B: Gọi ApiClient.getUser(email, token) để lấy đầy đủ info
//                // Để đơn giản, tạm thời lấy từ local DB (user đã được sync trước)
//                // Nếu cần info mới nhất, dùng ApiClient.getUser()
//
//                NguoiDung user = khoLuuTruNguoiDung.layTheoEmail(email.trim());
//
//                // Nếu user chưa có trong local DB (user mới từ server),
//                // có thể tạo object NguoiDung từ response hoặc tạo default
//                if (user == null) {
//                    // User mới - tạo object từ thông tin có sẵn
//                    // (Lý tưởng: server trả kèm full user info trong response)
//                    user = new NguoiDung("Unknown", email.trim(), password, "1990-01-01");
//                    System.err.println("[LoginAction] ⚠️ User not found locally, using minimal info");
//                }
//
//                // 🔹 BƯỚC 7: Lưu session (token + user)
//                // SessionManager sẽ:
//                // - Lưu NguoiDung object để dùng trong app
//                // - Lưu token để dùng cho subsequent API calls (Profile, Finance, etc)
//                SessionManager.getInstance().setSession(user, token);
//
//                // 🔹 BƯỚC 8: Hiển thị thông báo thành công
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.INFORMATION, "Thành công",
//                        "Đăng nhập thành công! Chào mừng " + user.layHoTen() + ".");
//
//                // 🔹 BƯỚC 9: Chuyển hướng sang trang Home
//                try {
//                    HandleNavigationAndAlert.getInstance().handleGoToHome(event);
//                } catch (IOException e) {
//                    System.err.println("[LoginAction] ❌ Lỗi khi chuyển sang Home: " + e.getMessage());
//                    e.printStackTrace();
//                    HandleNavigationAndAlert.getInstance().showAlert(
//                            Alert.AlertType.ERROR, "Lỗi giao diện",
//                            "Không thể tải trang Home. Vui lòng thử lại.");
//                }
//
//            } catch (UserException e) {
//                // Lỗi authentication từ server (email/password sai, account disabled, etc)
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.WARNING, "Lỗi đăng nhập", e.getMessage());
//
//            } catch (EmailException e) {
//                // Email validation error
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.WARNING, "Lỗi email", e.getMessage());
//
//            } catch (PasswordException e) {
//                // Password validation error
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.WARNING, "Lỗi mật khẩu", e.getMessage());
//
//            } catch (Exception e) {
//                // Lỗi không xác định
//                System.err.println("[LoginAction] ❌ Lỗi đăng nhập không xác định: " + e.getMessage());
//                e.printStackTrace();
//                HandleNavigationAndAlert.getInstance().showAlert(
//                        Alert.AlertType.ERROR, "Lỗi không xác định",
//                        "Đã xảy ra lỗi: " + e.getMessage());
//
//            } finally {
//                // Luôn giải phóng lock dù thành công hay thất bại
//                lock.unlock();
//            }
//
//        } catch (InterruptedException e) {
//            // Lock bị interrupt
//            System.err.println("[LoginAction] ❌ Đăng nhập bị gián đoạn: " + e.getMessage());
//            Thread.currentThread().interrupt();
//            HandleNavigationAndAlert.getInstance().showAlert(
//                    Alert.AlertType.ERROR, "Lỗi hệ thống",
//                    "Đăng nhập bị gián đoạn. Vui lòng thử lại.");
//        }
//    }

    @FXML
    public void dangNhap(ActionEvent event, String email, String password) {
        try {
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    checkSignIn(email, password); // check thông tin đăng nhập

                    // ĐỔI MỚI: Gọi server qua ApiClient thay vì DB trực tiếp
                    LoginResponse response = ApiClient.login(email, password);

                    // Kiểm tra kết quả từ server
                    if (response == null || response.getToken() == null) {
                        String thongBao = (response != null) ? response.getThongBao()
                                : "Không kết nối được server";
                        throw new UserException(thongBao);
                    }

                    // Đăng nhập thành công — lấy thông tin user từ DB local
                    // (vẫn cần object NguoiDung cho SessionManager)
                    NguoiDung user = khoLuuTruNguoiDung.layTheoEmail(email);

                    // Tạo token theo format TokenUtil để các trang Profile/Finance đọc được
                    String token = TokenUtil.generateToken(user);

                    // Lưu cả 2: token server (để gọi API) và token local (để đọc thông tin)
                    // Tạm thời dùng token local cho SessionManager
                    SessionManager.getInstance().setSession(user, token);

                    HandleNavigationAndAlert.getInstance()
                            .showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                    "Đăng nhập thành công! Chào mừng bạn.");

                    try {
                        HandleNavigationAndAlert.getInstance().handleGoToHome(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (UserException | EmailException | PasswordException e) {
                    HandleNavigationAndAlert.getInstance()
                            .showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", e.getMessage());
                } finally {
                    lock.unlock();
                }
            } else {
                HandleNavigationAndAlert.getInstance()
                        .showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống",
                                "Hệ thống đang bận, vui lòng thử lại sau!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}