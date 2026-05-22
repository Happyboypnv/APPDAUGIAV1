package com.mycompany.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    // ===== createSalt() =====

    @Test
    @DisplayName("createSalt() phải trả về chuỗi không rỗng")
    void createSalt_shouldReturnNonEmptyString() {
        String salt = PasswordEncoder.createSalt();
        assertNotNull(salt);
        assertFalse(salt.isEmpty());
    }

    @Test
    @DisplayName("createSalt() phải tạo salt khác nhau mỗi lần gọi")
    void createSalt_shouldReturnDifferentValuesEachCall() {
        String salt1 = PasswordEncoder.createSalt();
        String salt2 = PasswordEncoder.createSalt();
        assertNotEquals(salt1, salt2,
                "Hai salt liên tiếp không được trùng nhau");
    }

    @Test
    @DisplayName("createSalt() phải là chuỗi Base64 hợp lệ")
    void createSalt_shouldBeValidBase64() {
        String salt = PasswordEncoder.createSalt();
        assertDoesNotThrow(() ->
                        java.util.Base64.getDecoder().decode(salt),
                "Salt phải là Base64 hợp lệ"
        );
    }

    // ===== passwordEncoder() =====

    @Test
    @DisplayName("passwordEncoder() cùng password + salt phải cho cùng kết quả")
    void passwordEncoder_sameInputShouldGiveSameOutput() {
        String salt = PasswordEncoder.createSalt();
        String hash1 = PasswordEncoder.passwordEncoder("MyPassword123!", salt);
        String hash2 = PasswordEncoder.passwordEncoder("MyPassword123!", salt);
        assertEquals(hash1, hash2,
                "Cùng password + salt phải cho cùng hash");
    }

    @Test
    @DisplayName("passwordEncoder() password khác nhau phải cho hash khác nhau")
    void passwordEncoder_differentPasswordsShouldGiveDifferentHashes() {
        String salt = PasswordEncoder.createSalt();
        String hash1 = PasswordEncoder.passwordEncoder("Password1!", salt);
        String hash2 = PasswordEncoder.passwordEncoder("Password2!", salt);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("passwordEncoder() cùng password nhưng salt khác phải cho hash khác")
    void passwordEncoder_samePasswordDifferentSaltShouldGiveDifferentHashes() {
        String salt1 = PasswordEncoder.createSalt();
        String salt2 = PasswordEncoder.createSalt();
        String hash1 = PasswordEncoder.passwordEncoder("SamePassword1!", salt1);
        String hash2 = PasswordEncoder.passwordEncoder("SamePassword1!", salt2);
        assertNotEquals(hash1, hash2,
                "Cùng password nhưng salt khác phải cho hash khác (chống rainbow table)");
    }

    @Test
    @DisplayName("passwordEncoder() không được trả về null")
    void passwordEncoder_shouldNeverReturnNull() {
        String salt = PasswordEncoder.createSalt();
        assertNotNull(PasswordEncoder.passwordEncoder("test", salt));
    }

    // ===== checkPassword() =====

    @Test
    @DisplayName("checkPassword() mật khẩu đúng phải trả về true")
    void checkPassword_correctPasswordShouldReturnTrue() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("CorrectPass1!", salt);
        assertTrue(PasswordEncoder.checkPassword("CorrectPass1!", hashed, salt));
    }

    @Test
    @DisplayName("checkPassword() mật khẩu sai phải trả về false")
    void checkPassword_wrongPasswordShouldReturnFalse() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("CorrectPass1!", salt);
        assertFalse(PasswordEncoder.checkPassword("WrongPass1!", hashed, salt));
    }

    @Test
    @DisplayName("checkPassword() mật khẩu trống phải trả về false")
    void checkPassword_emptyPasswordShouldReturnFalse() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("CorrectPass1!", salt);
        assertFalse(PasswordEncoder.checkPassword("", hashed, salt));
    }

    @Test
    @DisplayName("checkPassword() sai salt phải trả về false")
    void checkPassword_wrongSaltShouldReturnFalse() {
        String correctSalt = PasswordEncoder.createSalt();
        String wrongSalt   = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("Password1!", correctSalt);
        assertFalse(PasswordEncoder.checkPassword("Password1!", hashed, wrongSalt));
    }

    @Test
    @DisplayName("checkPassword() phân biệt chữ hoa/thường")
    void checkPassword_shouldBeCaseSensitive() {
        String salt = PasswordEncoder.createSalt();
        String hashed = PasswordEncoder.passwordEncoder("Password1!", salt);
        assertFalse(PasswordEncoder.checkPassword("password1!", hashed, salt),
                "Phải phân biệt chữ hoa/thường");
    }
}