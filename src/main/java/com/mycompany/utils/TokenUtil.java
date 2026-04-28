package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TokenUtil {
    private static final String SECRET = "AiSoThiDiVeHiPiTi2026";

    public static String generateToken(NguoiDung user) {
        String tokenData = user.layMaNguoiDung() + ":" + user.layThuDienTu() + ":" + user.layHoTen() + ":"
                + user.layNgaySinh() + ":"  +user.layDiaChi() + ":"
                + user.laySoDienThoai() + ":"  + user.laySoDuKhaDung() + ":"  + user.laySoTaiKhoan() + ":" + user.layNganHang() + ":" + System.currentTimeMillis();
        // [userid, email, name, birth, address, phone, balance, bankAccount, timestamp]
        String encoded = Base64.getEncoder().encodeToString(tokenData.getBytes());
        return encoded + "." + generateSignature(encoded);
    }

    private static String generateSignature(String data) {
        // Simple signature using hash
        String combined = data + SECRET;
        return String.valueOf(combined.hashCode());
    }

    public static Map<String, Object> getUserInfoFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) return null;

            String encodedData = parts[0];
            String signature = parts[1];

            // Verify signature
            if (!signature.equals(generateSignature(encodedData))) {
                return null;
            }

            String decodedData = new String(Base64.getDecoder().decode(encodedData));
            String[] userData = decodedData.split(":");

            if (userData.length < 4) return null;

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userData[0]);
            userInfo.put("email", userData[1]);
            userInfo.put("name", userData[2]);
            userInfo.put("birth",userData[3]);
            userInfo.put("address", userData[4]);
            userInfo.put("phone", userData[5]);
            userInfo.put("balance", Double.parseDouble(userData[6]));
            userInfo.put("bankAccount",userData[7]);
            userInfo.put("bankName", userData[8]);
            return userInfo;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validateToken(String token) {
        return getUserInfoFromToken(token) != null;
    }
}
