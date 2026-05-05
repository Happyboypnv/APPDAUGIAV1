package com.mycompany.utils;

import com.mycompany.models.NguoiDung;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TokenUtil {

    private static final String SECRET = "AiSoThiDiVeHiPiTi2026";
    // FIX BUG 1+2: Đổi separator sang "|" — không bao giờ xuất hiện trong
    // tên người, email, địa chỉ, số điện thoại tiếng Việt
    private static final String SEP = "|";

    public static String generateToken(NguoiDung user) {
        String tokenData = nvl(user.layMaNguoiDung())  + SEP +
                nvl(user.layThuDienTu())    + SEP +
                nvl(user.layHoTen())        + SEP +
                nvl(user.layNgaySinh())     + SEP +
                nvl(user.getDiaChi())       + SEP +  // FIX BUG 3
                nvl(user.getSoDienThoai())  + SEP +  // FIX BUG 3
                user.getSoDuKhaDung()       + SEP +
                nvl(user.getSoTaiKhoan())   + SEP +  // FIX BUG 3
                nvl(user.getNganHang())     + SEP +  // FIX BUG 3
                System.currentTimeMillis();

        String encoded   = Base64.getEncoder().encodeToString(
                tokenData.getBytes(StandardCharsets.UTF_8) // charset cố định
        );
        String signature = generateSignature(encoded);
        return encoded + "." + signature;
    }

    public static Map<String, Object> getUserInfoFromToken(String token) {
        try {
            // FIX BUG 1+2: limit=2 để tránh split thêm nếu encoded có dấu "."
            // chi tach 2 phan
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) return null;

            String encodedData = parts[0];
            String signature   = parts[1];

            if (!signature.equals(generateSignature(encodedData))) return null;

            String decodedData = new String(
                    Base64.getDecoder().decode(encodedData),
                    StandardCharsets.UTF_8
            );

            // FIX BUG 1: split bằng "|" thay vì ":"
            String[] userData = decodedData.split("\\|", -1);
            // tach het

            // FIX BUG 2: check đủ 10 phần thay vì chỉ 4
            if (userData.length < 10) return null;

            return getStringObjectMap(userData);

        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> getStringObjectMap(String[] userData) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId",      userData[0]);
        info.put("email",       userData[1]);
        info.put("name",        userData[2]);
        info.put("birth",       userData[3]);
        info.put("address",     emptyToNull(userData[4])); // "" → null thật
        info.put("phone",       emptyToNull(userData[5]));
        info.put("balance",     Double.parseDouble(userData[6]));
        info.put("bankAccount", emptyToNull(userData[7]));
        info.put("bankName",    emptyToNull(userData[8]));
        // userData[9] là timestamp — dùng cho Bug 5 nếu cần check expiry
        return info;
    }

    // FIX BUG 3: null → "" khi tạo token
    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // Ngược lại khi đọc token: "" → null thật
    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static String generateSignature(String data) {
        // Bug 4 vẫn còn ở đây — hashCode() yếu
        // Chấp nhận cho BTL, production thì dùng HMAC-SHA256
        return String.valueOf((data + SECRET).hashCode());
    }

    public static boolean validateToken(String token) {
        return getUserInfoFromToken(token) != null;
    }
}