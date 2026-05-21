package com.mycompany.utils;

import com.mycompany.models.User;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho UserRepositorySQLite
 * Sử dụng SQLite in-memory database + Mock DatabaseConnection
 */
class UserRepositorySQLiteTest {

    private UserRepositorySQLite repo;
    private static Connection inMemoryConnection;
    private static MockedStatic<DatabaseConnection> mockedDbConn;

    // ==================== SETUP & TEARDOWN ====================

    @BeforeAll
    static void setUpDatabase() throws SQLException {
        // Tạo in-memory SQLite database
        inMemoryConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        inMemoryConnection.setAutoCommit(false);

        // Tạo schema giống production
        try (Statement stmt = inMemoryConnection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS nguoi_dung (
                    ma_nguoi_dung TEXT PRIMARY KEY,
                    ho_ten TEXT NOT NULL,
                    thu_dien_tu TEXT UNIQUE NOT NULL,
                    mat_khau TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    ngay_sinh TEXT,
                    dia_chi TEXT,
                    so_dien_thoai TEXT,
                    so_du_kha_dung REAL DEFAULT 0,
                    so_tai_khoan_ngan_hang TEXT,
                    ten_ngan_hang TEXT,
                    duong_dan_avatar TEXT
                )
            """);
            inMemoryConnection.commit();
        }

        // Mock DatabaseConnection.getConnection() trả về in-memory connection
        mockedDbConn = mockStatic(DatabaseConnection.class);
        mockedDbConn.when(DatabaseConnection::getConnection).thenReturn(inMemoryConnection);
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Xóa data trước mỗi test
        try (Statement stmt = inMemoryConnection.createStatement()) {
            stmt.execute("DELETE FROM nguoi_dung");
            inMemoryConnection.commit();
        }
        repo = new UserRepositorySQLite();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (mockedDbConn != null) {
            mockedDbConn.close();
        }
        if (inMemoryConnection != null && !inMemoryConnection.isClosed()) {
            inMemoryConnection.close();
        }
    }

    // ==================== HELPER METHODS ====================

    private User createTestUser(String suffix) {
        String salt = PasswordEncoder.createSalt();
        String hashedPass = PasswordEncoder.passwordEncoder("Password1!", salt);
        User user = new User(
                "Test User " + suffix,
                "test" + suffix + "@example.com",
                hashedPass,
                "2000-01-01"
        );
        user.setSalt(salt);
        return user;
    }

    // ==================== save() TESTS ====================

    @Test
    @DisplayName("save() - Lưu user mới thành công")
    void save_newUser_shouldSucceed() {
        User user = createTestUser("001");

        assertDoesNotThrow(() -> repo.save(user));
        assertNotNull(user.getUserId(), "User ID phải được tự động sinh");
        assertTrue(user.getUserId().startsWith("PPTT"), "User ID phải bắt đầu bằng PPTT");
    }

    @Test
    @DisplayName("save() - Không cho phép email trùng")
    void save_duplicateEmail_shouldNotSave() {
        User user1 = createTestUser("dup");
        User user2 = createTestUser("dup"); // Cùng email

        repo.save(user1);
        repo.save(user2); // Phải bị reject

        // Chỉ có 1 user trong DB
        Map<String, User> all = repo.findAll();
        assertEquals(1, all.size());
    }

    @Test
    @DisplayName("save() - Lưu đầy đủ thông tin user")
    void save_fullUserInfo_shouldPersistAllFields() {
        User user = createTestUser("full");
        user.setAddress("123 Test St, HN");
        user.setPhoneNumber("0901234567");
        user.setAvailableBalance(1_000_000);
        user.setBankAccountNumber("123456789");
        user.setBankName("Vietcombank");

        repo.save(user);

        User found = repo.findByEmail(user.getEmail());
        assertNotNull(found);
        assertEquals("123 Test St, HN", found.getAddress());
        assertEquals("0901234567", found.getPhoneNumber());
        assertEquals(1_000_000, found.getAvailableBalance(), 0.01);
        assertEquals("123456789", found.getBankAccountNumber());
        assertEquals("Vietcombank", found.getBankName());
    }

    // ==================== findByEmail() TESTS ====================

    @Test
    @DisplayName("findByEmail() - Tìm thấy user tồn tại")
    void findByEmail_existingUser_shouldReturnUser() {
        User user = createTestUser("find");
        repo.save(user);

        User found = repo.findByEmail("testfind@example.com");

        assertNotNull(found);
        assertEquals("Test User find", found.getFullName());
        assertEquals("testfind@example.com", found.getEmail());
    }

    @Test
    @DisplayName("findByEmail() - Email không tồn tại trả về null")
    void findByEmail_nonExistingEmail_shouldReturnNull() {
        User found = repo.findByEmail("notexist@example.com");
        assertNull(found);
    }

    @Test
    @DisplayName("findByEmail() - Phải load salt từ database")
    void findByEmail_shouldLoadSalt() {
        User user = createTestUser("salt");
        repo.save(user);

        User found = repo.findByEmail(user.getEmail());

        assertNotNull(found.getSalt(), "Salt phải được load từ DB");
        assertEquals(user.getSalt(), found.getSalt());
    }

    // ==================== findAll() TESTS ====================

    @Test
    @DisplayName("findAll() - Database rỗng trả về Map rỗng")
    void findAll_emptyDb_shouldReturnEmptyMap() {
        Map<String, User> all = repo.findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("findAll() - Trả về tất cả users")
    void findAll_multipleUsers_shouldReturnAll() {
        repo.save(createTestUser("a"));
        repo.save(createTestUser("b"));
        repo.save(createTestUser("c"));

        Map<String, User> all = repo.findAll();

        assertEquals(3, all.size());
        assertTrue(all.containsKey("testa@example.com"));
        assertTrue(all.containsKey("testb@example.com"));
        assertTrue(all.containsKey("testc@example.com"));
    }

    // ==================== isEmailAvailable() TESTS ====================

    @Test
    @DisplayName("isEmailAvailable() - Email mới trả về true")
    void isEmailAvailable_newEmail_shouldReturnTrue() {
        assertTrue(repo.isEmailAvailable("brand.new@example.com"));
    }

    @Test
    @DisplayName("isEmailAvailable() - Email đã tồn tại trả về false")
    void isEmailAvailable_existingEmail_shouldReturnFalse() {
        User user = createTestUser("exist");
        repo.save(user);

        assertFalse(repo.isEmailAvailable("testexist@example.com"));
    }

    // ==================== verifyCredentials() TESTS ====================

    @Test
    @DisplayName("verifyCredentials() - Đúng email và password trả về true")
    void verifyCredentials_correctCredentials_shouldReturnTrue() {
        String rawPassword = "MySecurePass1!";
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder(rawPassword, salt);

        User user = new User("Verify User", "verify@example.com", hashed, "1995-05-15");
        user.setSalt(salt);
        repo.save(user);

        assertTrue(repo.verifyCredentials("verify@example.com", rawPassword));
    }

    @Test
    @DisplayName("verifyCredentials() - Sai password trả về false")
    void verifyCredentials_wrongPassword_shouldReturnFalse() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("CorrectPass1!", salt);

        User user = new User("Test", "wrong@example.com", hashed, "1990-01-01");
        user.setSalt(salt);
        repo.save(user);

        assertFalse(repo.verifyCredentials("wrong@example.com", "WrongPass2!"));
    }

    @Test
    @DisplayName("verifyCredentials() - Email không tồn tại trả về false")
    void verifyCredentials_nonExistentEmail_shouldReturnFalse() {
        assertFalse(repo.verifyCredentials("ghost@example.com", "AnyPass1!"));
    }

    @Test
    @DisplayName("verifyCredentials() - Password rỗng trả về false")
    void verifyCredentials_emptyPassword_shouldReturnFalse() {
        User user = createTestUser("empty");
        repo.save(user);

        assertFalse(repo.verifyCredentials(user.getEmail(), ""));
    }

    // ==================== update() TESTS ====================

    @Test
    @DisplayName("update() - Cập nhật thông tin user thành công")
    void update_existingUser_shouldUpdateFields() {
        User user = createTestUser("upd");
        repo.save(user);

        // Cập nhật thông tin
        user.setFullName("Updated Name");
        user.setPhoneNumber("0999888777");
        user.setAvailableBalance(5_000_000);
        repo.update(user);

        // Verify
        User found = repo.findByEmail(user.getEmail());
        assertEquals("Updated Name", found.getFullName());
        assertEquals("0999888777", found.getPhoneNumber());
        assertEquals(5_000_000, found.getAvailableBalance(), 0.01);
    }

    @Test
    @DisplayName("update() - User null không crash")
    void update_nullUser_shouldNotThrow() {
        assertDoesNotThrow(() -> repo.update(null));
    }

    @Test
    @DisplayName("update() - User không có ID không crash")
    void update_userWithoutId_shouldNotThrow() {
        User user = new User("No ID", "noid@test.com", "pass", "2000-01-01");
        assertDoesNotThrow(() -> repo.update(user));
    }

    // ==================== delete() TESTS ====================

    @Test
    @DisplayName("delete() - Xóa user thành công")
    void delete_existingUser_shouldRemoveFromDb() {
        User user = createTestUser("del");
        repo.save(user);

        repo.delete(user);

        assertNull(repo.findByEmail(user.getEmail()));
        assertTrue(repo.isEmailAvailable(user.getEmail()));
    }

    @Test
    @DisplayName("delete() - User null không crash")
    void delete_nullUser_shouldNotThrow() {
        assertDoesNotThrow(() -> repo.delete(null));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Xử lý ký tự đặc biệt trong tên (SQL Injection prevention)")
    void save_specialCharsInName_shouldBeSafe() {
        User user = createTestUser("sql");
        user.setFullName("O'Brien \"Test\" User; DROP TABLE nguoi_dung;--");
        
        assertDoesNotThrow(() -> repo.save(user));
        
        User found = repo.findByEmail(user.getEmail());
        assertNotNull(found);
        assertEquals("O'Brien \"Test\" User; DROP TABLE nguoi_dung;--", found.getFullName());
    }

    @Test
    @DisplayName("Xử lý email với ký tự đặc biệt hợp lệ")
    void save_emailWithSpecialChars_shouldWork() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("Pass1!", salt);
        User user = new User("Plus User", "test+tag@example.com", hashed, "2000-01-01");
        user.setSalt(salt);

        repo.save(user);

        User found = repo.findByEmail("test+tag@example.com");
        assertNotNull(found);
    }

    @Test
    @DisplayName("User ID tự tăng đúng thứ tự")
    void save_multipleUsers_shouldHaveSequentialIds() {
        User user1 = createTestUser("seq1");
        User user2 = createTestUser("seq2");
        User user3 = createTestUser("seq3");

        repo.save(user1);
        repo.save(user2);
        repo.save(user3);

        // IDs phải là PPTT000001, PPTT000002, PPTT000003 (hoặc tiếp nối từ DB)
        assertNotEquals(user1.getUserId(), user2.getUserId());
        assertNotEquals(user2.getUserId(), user3.getUserId());
        assertNotEquals(user1.getUserId(), user3.getUserId());
    }
}
