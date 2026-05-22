package com.mycompany.action;

import com.mycompany.exception.Login.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Method;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test riêng phần validation của LoginAction.
 * Dùng reflection để gọi private method checkSignUp() và checkSignIn().
 */
class LoginActionValidationTest {

    private LoginAction loginAction;
    private Method checkSignUp;
    private Method checkSignIn;

    @BeforeEach
    void setUp() throws Exception {
        loginAction = LoginAction.getInstance();

        checkSignUp = LoginAction.class.getDeclaredMethod(
                "checkSignUp", String.class, String.class, String.class, LocalDate.class);
        checkSignUp.setAccessible(true);

        checkSignIn = LoginAction.class.getDeclaredMethod(
                "checkSignIn", String.class, String.class);
        checkSignIn.setAccessible(true);
    }

    // ===== checkSignUp() — Tên =====

    @Test
    @DisplayName("Tên hợp lệ tiếng Việt không được throw exception")
    void checkSignUp_validVietnameseName_shouldNotThrow() {
        assertDoesNotThrow(() -> checkSignUp.invoke(loginAction,
                "Nguyễn Văn An", "test@gmail.com", "Password1!", LocalDate.of(2000, 1, 1)));
    }

    @Test
    @DisplayName("Tên rỗng phải throw UserNameException")
    void checkSignUp_emptyName_shouldThrowUserNameException() {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "", "test@gmail.com", "Password1!", LocalDate.of(2000, 1, 1)));
        assertTrue(ex.getCause() instanceof UserNameException);
    }

    @Test
    @DisplayName("Tên 1 ký tự phải throw UserNameException (quá ngắn)")
    void checkSignUp_oneCharName_shouldThrowUserNameException() {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "A", "test@gmail.com", "Password1!", LocalDate.of(2000, 1, 1)));
        assertTrue(ex.getCause() instanceof UserNameException);
    }

    // ===== checkSignUp() — Email =====

    @ParameterizedTest
    @DisplayName("Email không hợp lệ phải throw EmailException")
    @ValueSource(strings = {
            "notanemail",
            "missing@domain",
            "@nodomain.com",
            "spaces in@email.com",
            ""
    })
    void checkSignUp_invalidEmail_shouldThrowEmailException(String email) {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "Nguyen Van A", email, "Password1!", LocalDate.of(2000, 1, 1)));
        assertTrue(ex.getCause() instanceof EmailException);
    }

    @Test
    @DisplayName("Email hợp lệ không được throw exception")
    void checkSignUp_validEmail_shouldNotThrow() {
        assertDoesNotThrow(() -> checkSignUp.invoke(loginAction,
                "Nguyen Van A", "valid@gmail.com", "Password1!", LocalDate.of(2000, 1, 1)));
    }

    // ===== checkSignUp() — Mật khẩu =====

    @ParameterizedTest
    @DisplayName("Mật khẩu yếu phải throw PasswordException")
    @ValueSource(strings = {
            "short1!",           // Quá ngắn
            "nouppercase1!",     // Không có chữ hoa
            "NOLOWERCASE1!",     // Không có chữ thường
            "NoSpecialChar1",    // Không có ký tự đặc biệt
            "NoNumber!Abc",      // Không có số
            ""
    })
    void checkSignUp_weakPassword_shouldThrowPasswordException(String password) {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "Nguyen Van A", "test@gmail.com", password, LocalDate.of(2000, 1, 1)));
        assertTrue(ex.getCause() instanceof PasswordException);
    }

    @Test
    @DisplayName("Mật khẩu đủ mạnh không được throw exception")
    void checkSignUp_strongPassword_shouldNotThrow() {
        assertDoesNotThrow(() -> checkSignUp.invoke(loginAction,
                "Nguyen Van A", "test@gmail.com", "StrongPass1!", LocalDate.of(2000, 1, 1)));
    }

    // ===== checkSignUp() — Ngày sinh =====

    @Test
    @DisplayName("Ngày sinh null phải throw DateException")
    void checkSignUp_nullDate_shouldThrowDateException() {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "Nguyen Van A", "test@gmail.com", "Password1!", null));
        assertTrue(ex.getCause() instanceof DateException);
    }

    @Test
    @DisplayName("Ngày sinh trong tương lai phải throw DateException")
    void checkSignUp_futureDateOfBirth_shouldThrowDateException() {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "Nguyen Van A", "test@gmail.com", "Password1!",
                        LocalDate.now().plusDays(1)));
        assertTrue(ex.getCause() instanceof DateException);
    }

    @Test
    @DisplayName("Tuổi dưới 18 phải throw DateException")
    void checkSignUp_under18_shouldThrowDateException() {
        Exception ex = assertThrows(Exception.class, () ->
                checkSignUp.invoke(loginAction,
                        "Nguyen Van A", "test@gmail.com", "Password1!",
                        LocalDate.now().minusYears(17)));
        assertTrue(ex.getCause() instanceof DateException);
    }

    @Test
    @DisplayName("Đúng 18 tuổi không được throw exception")
    void checkSignUp_exactly18YearsOld_shouldNotThrow() {
        assertDoesNotThrow(() -> checkSignUp.invoke(loginAction,
                "Nguyen Van A", "test@gmail.com", "Password1!",
                LocalDate.now().minusYears(18)));
    }
}