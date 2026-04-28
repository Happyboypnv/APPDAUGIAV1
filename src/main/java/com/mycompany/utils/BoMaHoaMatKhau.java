package com.mycompany.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class BoMaHoaMatKhau {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    // Tạo salt ngẫu nhiên
    public static String taoSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Mã hóa mật khẩu với salt
    public static String maHoaMatKhau(String matKhau, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(matKhau.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi mã hóa mật khẩu", e);
        }
    }

    // Kiểm tra mật khẩu
    public static boolean kiemTraMatKhau(String matKhauNhap, String matKhauMaHoa, String salt) {
        String hashedInput = maHoaMatKhau(matKhauNhap, salt);
        return hashedInput.equals(matKhauMaHoa);
    }
}
