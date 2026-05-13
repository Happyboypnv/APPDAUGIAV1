package com.mycompany.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class BoMaHoaMatKhauTest {

    @Test
    @DisplayName("Salt phải khác nhau mỗi lần tạo")
    void taoSalt_moiLanGoiPhaicKhacNhau() {
        // ARRANGE — không cần gì

        // ACT
        String salt1 = BoMaHoaMatKhau.taoSalt();
        String salt2 = BoMaHoaMatKhau.taoSalt();

        // ASSERT
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2); // 2 salt phải khác nhau
    }

    @Test
    @DisplayName("Mã hóa cùng mật khẩu + cùng salt → luôn ra cùng kết quả")
    void maHoaMatKhau_cuongMatKhauCungSalt_raKetQuaGiongNhau() {
        // ARRANGE
        String matKhau = "Password123!";
        String salt = BoMaHoaMatKhau.taoSalt();

        // ACT
        String hash1 = BoMaHoaMatKhau.maHoaMatKhau(matKhau, salt);
        String hash2 = BoMaHoaMatKhau.maHoaMatKhau(matKhau, salt);

        // ASSERT
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Mã hóa cùng mật khẩu nhưng khác salt → ra kết quả khác nhau")
    void maHoaMatKhau_khacSalt_raKetQuaKhacNhau() {
        String matKhau = "Password123!";
        String salt1 = BoMaHoaMatKhau.taoSalt();
        String salt2 = BoMaHoaMatKhau.taoSalt();

        String hash1 = BoMaHoaMatKhau.maHoaMatKhau(matKhau, salt1);
        String hash2 = BoMaHoaMatKhau.maHoaMatKhau(matKhau, salt2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Kiểm tra mật khẩu đúng → trả về true")
    void kiemTraMatKhau_matKhauDung_trueVe() {
        // ARRANGE
        String matKhauGoc = "Password123!";
        String salt = BoMaHoaMatKhau.taoSalt();
        String hash = BoMaHoaMatKhau.maHoaMatKhau(matKhauGoc, salt);

        // ACT
        boolean ketQua = BoMaHoaMatKhau.kiemTraMatKhau(matKhauGoc, hash, salt);

        // ASSERT
        assertTrue(ketQua);
    }

    @Test
    @DisplayName("Kiểm tra mật khẩu sai → trả về false")
    void kiemTraMatKhau_matKhauSai_falseVe() {
        String matKhauGoc = "Password123!";
        String matKhauSai = "SaiMatKhau999!";
        String salt = BoMaHoaMatKhau.taoSalt();
        String hash = BoMaHoaMatKhau.maHoaMatKhau(matKhauGoc, salt);

        boolean ketQua = BoMaHoaMatKhau.kiemTraMatKhau(matKhauSai, hash, salt);

        assertFalse(ketQua);
    }
}