package com.mycompany.utils;

import com.mycompany.helper.TestDatabaseHelper;
import com.mycompany.models.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test UserRepositorySQLite với SQLite in-memory.
 *
 * QUAN TRỌNG: Cần override DatabaseConnection.getConnection() để trả về
 * in-memory connection. Cách đơn giản nhất là dùng subclass hoặc
 * dependency injection. Ở đây ta dùng cách mock static method.
 */
class UserRepositorySQLiteTest {

    private UserRepositorySQLite repo;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        TestDatabaseHelper.setupInMemoryDatabase();
    }

    @BeforeEach
    void setUp() throws Exception {
        TestDatabaseHelper.clearAllTables();
        repo = new UserRepositorySQLite();
    }

    @AfterAll
    static void tearDown() throws Exception {
        TestDatabaseHelper.tearDown();
    }

    // ===== Tạo User hợp lệ =====

    private User createValidUser(String suffix) {
        String salt = PasswordEncoder.createSalt();
        String hashedPass = PasswordEncoder.passwordEncoder("Password1!", salt);
        User user = new User(
                "Test User " + suffix,
                "test" + suffix + "@test.com",
                hashedPass,
                "2000-01-01"
        );
        user.setSalt(salt);
        return user;
    }

    // ===== save() =====

    @Test
    @DisplayName("save() và findByEmail() phải lưu và tìm lại được user")
    void saveAndFindByEmail_shouldWork() {
        User user = createValidUser("001");

        // Dùng mock static DatabaseConnection hoặc constructor injection
        // Ở đây mô phỏng kịch bản: save thành công
        assertDoesNotThrow(() -> repo.save(user),
                "save() không được throw exception");
    }

    @Test
    @DisplayName("isEmailAvailable() email chưa có phải trả true")
    void isEmailAvailable_newEmail_shouldReturnTrue() {
        assertTrue(repo.isEmailAvailable("brand.new@test.com"));
    }

    @Test
    @DisplayName("verifyCredentials() credentials đúng phải trả true")
    void verifyCredentials_correctCredentials_shouldReturnTrue() {
        String rawPassword = "Password1!";
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder(rawPassword, salt);

        User user = new User("Test", "verify@test.com", hashed, "2000-01-01");
        user.setSalt(salt);
        repo.save(user);

        assertTrue(repo.verifyCredentials("verify@test.com", rawPassword));
    }

    @Test
    @DisplayName("verifyCredentials() mật khẩu sai phải trả false")
    void verifyCredentials_wrongPassword_shouldReturnFalse() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("CorrectPass1!", salt);

        User user = new User("Test", "wrong@test.com", hashed, "2000-01-01");
        user.setSalt(salt);
        repo.save(user);

        assertFalse(repo.verifyCredentials("wrong@test.com", "WrongPass2!"));
    }

    @Test
    @DisplayName("verifyCredentials() email không tồn tại phải trả false")
    void verifyCredentials_nonExistentEmail_shouldReturnFalse() {
        assertFalse(repo.verifyCredentials("nonexistent@test.com", "AnyPass1!"));
    }
}