package com.mycompany.action;

import com.mycompany.exception.Login.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ProfileActionTest {

    private ProfileAction profileAction;

    @BeforeEach
    void setUp() {
        profileAction = ProfileAction.getInstance();
    }

    // ===== checkInfo() — Tên =====

    @Test
    @DisplayName("Thông tin hợp lệ không được throw exception")
    void checkInfo_validInput_shouldNotThrow() {
        assertDoesNotThrow(() ->
                profileAction.checkInfo("Nguyễn Văn An", "0901234567", "Quận 1, Hà Nội"));
    }

    @Test
    @DisplayName("Tên null phải throw UserNameException")
    void checkInfo_nullName_shouldThrow() {
        assertThrows(UserNameException.class, () ->
                profileAction.checkInfo(null, "0901234567", "Quận 1, Hà Nội"));
    }

    @Test
    @DisplayName("Tên rỗng phải throw UserNameException")
    void checkInfo_emptyName_shouldThrow() {
        assertThrows(UserNameException.class, () ->
                profileAction.checkInfo("", "0901234567", "Quận 1, Hà Nội"));
    }

    @Test
    @DisplayName("Tên có ký tự đặc biệt không hợp lệ phải throw UserNameException")
    void checkInfo_nameWithInvalidChars_shouldThrow() {
        assertThrows(UserNameException.class, () ->
                profileAction.checkInfo("Name123@#", "0901234567", "Quận 1, Hà Nội"));
    }

    // ===== checkInfo() — Số điện thoại =====

    @ParameterizedTest
    @DisplayName("Số điện thoại không hợp lệ phải throw PhoneNumberException")
    @ValueSource(strings = {
            "090123",          // Quá ngắn (< 10 số)
            "0901234567890123", // Quá dài (> 15 số)
            "090123456a",      // Có chữ cái
            "090-123-456",     // Có dấu gạch ngang
            ""
    })
    void checkInfo_invalidPhone_shouldThrow(String phone) {
        assertThrows(PhoneNumberException.class, () ->
                profileAction.checkInfo("Nguyen Van A", phone, "Quận 1, Hà Nội"));
    }

    @Test
    @DisplayName("Số điện thoại 10 chữ số hợp lệ không được throw exception")
    void checkInfo_validPhone10Digits_shouldNotThrow() {
        assertDoesNotThrow(() ->
                profileAction.checkInfo("Nguyen Van A", "0901234567", "Quận 1, Hà Nội"));
    }

    // ===== checkInfo() — Địa chỉ =====

    @Test
    @DisplayName("Địa chỉ thiếu dấu phẩy phải throw AddressException")
    void checkInfo_addressWithoutComma_shouldThrow() {
        assertThrows(AddressException.class, () ->
                profileAction.checkInfo("Nguyen Van A", "0901234567", "Quan 1 Ha Noi"));
    }

    @Test
    @DisplayName("Địa chỉ rỗng phải throw AddressException")
    void checkInfo_emptyAddress_shouldThrow() {
        assertThrows(AddressException.class, () ->
                profileAction.checkInfo("Nguyen Van A", "0901234567", ""));
    }

    @Test
    @DisplayName("Địa chỉ có định dạng đúng 'Xã, Thành phố' không được throw")
    void checkInfo_validAddressFormat_shouldNotThrow() {
        assertDoesNotThrow(() ->
                profileAction.checkInfo("Nguyen Van A", "0901234567", "Hội An, Đà Nẵng"));
    }
}