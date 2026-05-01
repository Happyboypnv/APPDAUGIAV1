package com.mycompany.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Lớp BoMaHoaMatKhau cung cấp các hàm để mã hóa, tạo salt và kiểm tra mật khẩu
 * sử dụng thuật toán SHA-256 kết hợp với salt để tăng tính bảo mật
 */
public class BoMaHoaMatKhau {
    // Thuật toán mã hóa SHA-256 - một trong những thuật toán hash an toàn nhất hiện nay
    private static final String ALGORITHM = "SHA-256";

    // Độ dài của salt (ngẫu nhiên) = 16 byte, được sử dụng để tăng độ khó đoán mật khẩu
    private static final int SALT_LENGTH = 16;

    /**
     * HÀM: taoSalt()
     * MỤC ĐÍCH: Tạo một chuỗi salt ngẫu nhiên để sử dụng trong quá trình mã hóa mật khẩu
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Salt là một chuỗi ký tự ngẫu nhiên dùng để tạo ra hash khác nhau cho cùng một mật khẩu
     * - Điều này giúp tăng tính bảo mật, vì nó chống lại các cuộc tấn công rainbow table
     * - SecureRandom là một PRNG (Pseudo Random Number Generator) an toàn mã hóa
     *
     * @return Một chuỗi salt được mã hóa Base64 (dạng text dễ lưu trữ)
     */
    public static String taoSalt() {
        // Tạo một generator số ngẫu nhiên an toàn
        SecureRandom random = new SecureRandom();

        // Tạo một mảng byte có độ dài 16 để chứa salt
        byte[] salt = new byte[SALT_LENGTH];

        // Điền mảng với các byte ngẫu nhiên
        random.nextBytes(salt);

        // Mã hóa salt từ byte sang Base64 (định dạng text) để có thể lưu trữ dễ dàng
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * HÀM: maHoaMatKhau(String matKhau, String salt)
     * MỤC ĐÍCH: Mã hóa mật khẩu bằng cách kết hợp mật khẩu với salt sử dụng SHA-256
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Quá trình này là một chiều: có thể mã hóa mật khẩu nhưng không thể giải mã ngược lại
     * - Salt được thêm vào để tạo ra hash khác nhau cho cùng một mật khẩu (nếu salt khác)
     * - Kết quả là một mật khẩu đã được bảo vệ an toàn trước khi lưu vào database
     *
     * @param matKhau Mật khẩu gốc do người dùng nhập vào
     * @param salt Chuỗi salt đã được tạo từ hàm taoSalt()
     * @return Mật khẩu đã được mã hóa dưới dạng Base64
     */
    public static String maHoaMatKhau(String matKhau, String salt) {
        try {
            // Tạo một đối tượng MessageDigest sử dụng thuật toán SHA-256
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);

            // Giải mã salt từ Base64 sang byte array
            // Điều này cần thiết vì salt được lưu dưới dạng Base64
            md.update(Base64.getDecoder().decode(salt));

            // Tiếp tục cập nhật digest với các byte của mật khẩu
            // md.digest() sẽ tính toán lại hash với cả salt và mật khẩu
            // Kết quả là một mảng byte chứa hash của (salt + mật khẩu)
            byte[] hashedPassword = md.digest(matKhau.getBytes());

            // Mã hóa kết quả hash từ byte sang Base64 (định dạng text)
            // để có thể lưu trữ và so sánh dễ dàng
            return Base64.getEncoder().encodeToString(hashedPassword);

        } catch (NoSuchAlgorithmException e) {
            // Nếu SHA-256 không được hỗ trợ (hiếm khi xảy ra), ném ra exception
            throw new RuntimeException("Lỗi mã hóa mật khẩu", e);
        }
    }

    /**
     * HÀM: kiemTraMatKhau(String matKhauNhap, String matKhauMaHoa, String salt)
     * MỤC ĐÍCH: Kiểm tra xem mật khẩu do người dùng nhập có khớp với mật khẩu đã mã hóa không
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Đây là hàm dùng khi người dùng đăng nhập
     * - Không thể so sánh trực tiếp vì hash là một chiều
     * - Thay vì so sánh trực tiếp, ta mã hóa mật khẩu nhập vào với cùng salt
     * - Nếu hai hash giống nhau, tức là mật khẩu đúng
     *
     * @param matKhauNhap Mật khẩu do người dùng nhập khi đăng nhập
     * @param matKhauMaHoa Mật khẩu đã được mã hóa, lưu trữ trong database
     * @param salt Salt đã được lưu cùng với mật khẩu mã hóa
     * @return true nếu mật khẩu đúng, false nếu mật khẩu sai
     */
    public static boolean kiemTraMatKhau(String matKhauNhap, String matKhauMaHoa, String salt) {
        // Mã hóa mật khẩu vừa nhập bằng cùng salt mà đã sử dụng trước đó
        String hashedInput = maHoaMatKhau(matKhauNhap, salt);

        // So sánh mật khẩu vừa mã hóa với mật khẩu đã mã hóa lưu trong database
        // Nếu giống nhau -> mật khẩu đúng, trả về true
        // Nếu khác nhau -> mật khẩu sai, trả về false
        return hashedInput.equals(matKhauMaHoa);
    }
}
