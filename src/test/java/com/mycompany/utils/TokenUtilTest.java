package com.mycompany.utils;

import com.mycompany.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {

    private User fullUser;
    private User minimalUser;

    @BeforeEach
    void setUp() {
        // User đầy đủ thông tin
        fullUser = new User(
                "Nguyễn Văn An",
                "nguyenvanan@gmail.com",
                "hashedPassword",
                "2000-01-15"
        );
        fullUser.setUserId("PPTT000001");
        fullUser.setAddress("Phường 1, Hà Nội");
        fullUser.setPhoneNumber("0901234567");
        fullUser.setAvailableBalance(1_000_000);
        fullUser.setBankAccountNumber("1234567890");
        fullUser.setBankName("MB Bank");

        // User chỉ có thông tin cơ bản (address, phone = null)
        minimalUser = new User(
                "Trần Thị Bình",
                "binhtt@gmail.com",
                "hashedPassword",
                "1999-05-20"
        );
        minimalUser.setUserId("PPTT000002");
    }

    // ===== generateToken() + getUserInfoFromToken() — round-trip =====

    @Test
    @DisplayName("generateToken() không được trả về null")
    void generateToken_shouldNotReturnNull() {
        assertNotNull(TokenUtil.generateToken(fullUser));
    }

    @Test
    @DisplayName("Token phải chứa dấu chấm ngăn cách encoded.signature")
    void generateToken_shouldContainDot() {
        String token = TokenUtil.generateToken(fullUser);
        assertTrue(token.contains("."),
                "Token phải có format encoded.signature");
    }

    @Test
    @DisplayName("Round-trip: generateToken → getUserInfoFromToken phải giữ nguyên email")
    void roundTrip_shouldPreserveEmail() {
        String token = TokenUtil.generateToken(fullUser);
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        assertNotNull(info);
        assertEquals("nguyenvanan@gmail.com", info.get("email"));
    }

    @Test
    @DisplayName("Round-trip: phải giữ nguyên tên tiếng Việt")
    void roundTrip_shouldPreserveVietnameseName() {
        String token = TokenUtil.generateToken(fullUser);
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        assertEquals("Nguyễn Văn An", info.get("name"));
    }

    @Test
    @DisplayName("Round-trip: phải giữ nguyên địa chỉ có dấu phẩy")
    void roundTrip_shouldPreserveAddressWithComma() {
        String token = TokenUtil.generateToken(fullUser);
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        assertEquals("Phường 1, Hà Nội", info.get("address"));
    }

    @Test
    @DisplayName("Round-trip: phải giữ nguyên số dư")
    void roundTrip_shouldPreserveBalance() {
        String token = TokenUtil.generateToken(fullUser);
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        assertEquals(1_000_000.0, (Double) info.get("balance"), 0.01);
    }

    @Test
    @DisplayName("Round-trip: user không có address/phone phải trả về null cho các field đó")
    void roundTrip_nullFieldsShouldReturnNull() {
        String token = TokenUtil.generateToken(minimalUser);
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        assertNotNull(info);
        assertNull(info.get("address"),
                "address null phải được giữ nguyên là null");
        assertNull(info.get("phone"),
                "phone null phải được giữ nguyên là null");
    }

    // ===== validateToken() =====

    @Test
    @DisplayName("validateToken() token hợp lệ phải trả về true")
    void validateToken_validTokenShouldReturnTrue() {
        String token = TokenUtil.generateToken(fullUser);
        assertTrue(TokenUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken() token rỗng phải trả về false")
    void validateToken_emptyTokenShouldReturnFalse() {
        assertFalse(TokenUtil.validateToken(""));
    }

    @Test
    @DisplayName("validateToken() token null phải trả về false")
    void validateToken_nullTokenShouldReturnFalse() {
        assertFalse(TokenUtil.validateToken(null));
    }

    @Test
    @DisplayName("validateToken() token bị chỉnh sửa phải trả về false")
    void validateToken_tamperedTokenShouldReturnFalse() {
        String token = TokenUtil.generateToken(fullUser);
        // Thêm ký tự vào cuối token để giả mạo
        String tamperedToken = token + "HACKED";
        assertFalse(TokenUtil.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("validateToken() token thiếu signature phải trả về false")
    void validateToken_missingSignatureShouldReturnFalse() {
        String token = TokenUtil.generateToken(fullUser);
        // Chỉ lấy phần encoded, bỏ signature
        String encodedOnly = token.split("\\.")[0];
        assertFalse(TokenUtil.validateToken(encodedOnly));
    }

    @Test
    @DisplayName("getUserInfoFromToken() token giả mạo phải trả về null")
    void getUserInfoFromToken_fakeTokenShouldReturnNull() {
        assertNull(TokenUtil.getUserInfoFromToken("abc.wrongsignature"));
    }
}