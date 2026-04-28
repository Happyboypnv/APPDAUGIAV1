package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * TokenUtil - Utility class untuk JWT Token management
 *
 * MỤC ĐÍCH:
 * - Tạo JWT token chứa thông tin người dùng
 * - Giải mã token để lấy thông tin người dùng
 * - Xác thực token (kiểm tra hợp lệ hay không)
 *
 * TOKEN FORMAT:
 * [Base64_Encoded_Data].[Signature]
 * Ví dụ: "eyJ1c2VySWQiOiIxIiwibmFtZSI6IE5ndXllbiBWYW4gQSJ9.123456"
 *
 * TOKEN CHỨA:
 * - userId: Mã người dùng
 * - email: Email
 * - name: Tên người dùng
 * - birth: Ngày sinh
 * - address: Địa chỉ
 * - phone: Số điện thoại
 * - balance: Số dư
 * - bankAccount: Số tài khoản
 * - bankName: Tên ngân hàng
 * - timestamp: Thời gian tạo token
 *
 * BẢNG MN:
 * - Token không lưu mật khẩu vì an toàn
 * - Token dùng Base64 encoding, không phải encryption (không bảo mật tuyệt đối)
 * - Nếu cần bảo mật cao hơn, nên dùng JWT library thực sự như jjwt
 */
public class TokenUtil {

    // Secret key dùng để tạo signature xác thực token
    // Nếu ai đó sửa token mà không biết secret này, signature sẽ sai
    // QUAN TRỌNG: Trong production, không nên hardcode secret vào code
    private static final String SECRET = "AiSoThiDiVeHiPiTi2026";

    /**
     * PHƯƠNG THỨC: generateToken(NguoiDung user)
     * MỤC ĐÍCH: Tạo JWT token từ object NguoiDung
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy tất cả thông tin từ user object
     * 2. Nối các field với dấu ":" thành một chuỗi
     * 3. Mã hóa chuỗi thành Base64
     * 4. Tạo signature bằng hash chuỗi + secret
     * 5. Kết hợp: encoded_data + "." + signature
     * 6. Trả về token
     *
     * FORMAT CHUỖI:
     * userid:email:name:birth:address:phone:balance:bankAccount:bankName:timestamp
     *
     * @param user NguoiDung object cần tạo token
     * @return Token string dạng [encoded].[signature]
     */
    public static String generateToken(NguoiDung user) {
        // 🔹 BƯỚC 1: Tạo chuỗi dữ liệu từ user
        // Nối các field với dấu ":"
        String tokenData = user.layMaNguoiDung() + ":" +
                           user.layThuDienTu() + ":" +
                           user.layHoTen() + ":" +
                           user.layNgaySinh() + ":" +
                           user.layDiaChi() + ":" +
                           user.laySoDienThoai() + ":" +
                           user.laySoDuKhaDung() + ":" +
                           user.laySoTaiKhoan() + ":" +
                           user.layNganHang() + ":" +
                           System.currentTimeMillis();
        // Ví dụ: "1:admin@gmail.com:Nguyễn Văn A:2000-01-15:Hà Nội:0123456789:1000000:123456:Vietcombank:1718350000000"

        // 🔹 BƯỚC 2: Mã hóa chuỗi thành Base64
        // Base64 encoder convert bytes sang Base64 string
        String encoded = Base64.getEncoder().encodeToString(tokenData.getBytes());
        // Ví dụ: "MTpKb2huOk9laW1KYW4xOTkwLTE5LTA5OjE2NDAwMDB..."

        // 🔹 BƯỚC 3: Tạo signature
        // generateSignature() tính hash của (encoded + secret)
        String signature = generateSignature(encoded);
        // Ví dụ: "123456789"

        // 🔹 BƯỚC 4: Kết hợp thành token cuối cùng
        return encoded + "." + signature;
        // Ví dụ: "MTpKb2huOm...MC50ZXN0LmNvbQ==.123456789"
    }

    /**
     * PHƯƠNG THỨC PRIVATE: generateSignature(String data)
     * MỤC ĐÍCH: Tạo signature để xác thực token
     *
     * GIẢI THÍCH:
     * - Kết hợp data + secret
     * - Tính hash của chuỗi kết hợp
     * - Nếu ai đó sửa token mà không biết secret → signature sẽ sai
     * - Signature dùng để phát hiện token bị giả mạo
     *
     * VỀ SECURITY:
     * - Phương thức này rất đơn giản, chỉ dùng để demo/test
     * - Trong production, nên dùng HMAC-SHA256 hoặc library JWT thực sự
     *
     * @param data Dữ liệu encoded
     * @return Signature string (hash code)
     */
    private static String generateSignature(String data) {
        // Nối data + secret
        String combined = data + SECRET;
        // Tính hash code và convert thành string
        // Lưu ý: hashCode() không phải hashing cryptographic, chỉ là hash general
        return String.valueOf(combined.hashCode());
    }

    /**
     * PHƯƠNG THỨC: getUserInfoFromToken(String token)
     * MỤC ĐÍCH: Giải mã token để lấy thông tin người dùng
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Tách token thành 2 phần: encoded_data và signature
     * 2. Xác thực signature (kiểm tra token có bị sửa không)
     * 3. Giải mã Base64 để lấy chuỗi gốc
     * 4. Tách chuỗi thành mảng by dấu ":"
     * 5. Tạo Map chứa các field
     * 6. Trả về Map
     *
     * RETURN NULL KHI:
     * - Token format không đúng (không có dấu ".")
     * - Signature không khớp (token bị sửa)
     * - Data không hợp lệ
     * - Exception có lỗi
     *
     * @param token Token string cần giải mã
     * @return Map<String, Object> chứa thông tin hoặc null
     */
    public static Map<String, Object> getUserInfoFromToken(String token) {
        try {
            // 🔹 BƯỚC 1: Tách token thành 2 phần
            // Token format: [encoded_data].[signature]
            String[] parts = token.split("\\.");

            // Kiểm tra: đúng 2 phần không? (nếu không, token invalid)
            if (parts.length != 2) return null;

            // Lấy phần encoded data và signature
            String encodedData = parts[0];
            String signature = parts[1];

            // 🔹 BƯỚC 2: Xác thực signature
            // Tình lại signature từ encoded data
            // So sánh với signature trong token
            if (!signature.equals(generateSignature(encodedData))) {
                // Signature không khớp = token bị sửa
                return null;
            }

            // 🔹 BƯỚC 3: Giải mã Base64
            // Base64 decoder convert Base64 string back thành original bytes
            String decodedData = new String(Base64.getDecoder().decode(encodedData));
            // Ví dụ: "1:admin@gmail.com:Nguyễn Văn A:2000-01-15:Hà Nội:..."

            // 🔹 BƯỚC 4: Tách chuỗi thành mảng
            // Tách by dấu ":"
            String[] userData = decodedData.split(":");
            // userData[0] = userId, userData[1] = email, userData[2] = name, ...

            // Kiểm tra: có tối thiểu 4 phần không?
            if (userData.length < 4) return null;

            // 🔹 BƯỚC 5: Tạo Map chứa các field
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userData[0]);
            userInfo.put("email", userData[1]);
            userInfo.put("name", userData[2]);
            userInfo.put("birth", userData[3]);
            userInfo.put("address", userData[4]);
            userInfo.put("phone", userData[5]);
            // Chuyển balance từ string sang Double
            userInfo.put("balance", Double.parseDouble(userData[6]));
            userInfo.put("bankAccount", userData[7]);
            userInfo.put("bankName", userData[8]);

            // 🔹 BƯỚC 6: Trả về Map
            return userInfo;

        } catch (Exception e) {
            // Bắt mọi exception
            // Nếu có lỗi giải mã, token invalid → trả về null
            return null;
        }
    }

    /**
     * PHƯƠNG THỨC: validateToken(String token)
     * MỤC ĐÍCH: Kiểm tra token có hợp lệ không
     *
     * GIẢI THÍCH:
     * - Cách đơn giản: gọi getUserInfoFromToken()
     * - Nếu return Map (khác null) → token hợp lệ
     * - Nếu return null → token invalid
     *
     * DÙNG CHO:
     * - Kiểm tra token trước khi dùng
     * - Phòng chống token giả mạo/sửa
     * - Kiểm tra isLoggedIn() trong SessionManager
     *
     * @param token Token cần kiểm tra
     * @return true nếu token hợp lệ, false nếu invalid
     */
    public static boolean validateToken(String token) {
        // Nếu getUserInfoFromToken() return khác null = token hợp lệ
        return getUserInfoFromToken(token) != null;
    }
}
