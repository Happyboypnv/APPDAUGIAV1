package com.mycompany.utils;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.NguoiDung;
import javafx.scene.control.Alert;


import java.util.Map;
import java.util.prefs.Preferences; // save thong tin sau khi app dong

public class CapNhatThongTinNguoiDung {
    private static CapNhatThongTinNguoiDung instance;
    private static final Preferences prefs =
            Preferences.userNodeForPackage(CapNhatThongTinNguoiDung.class);
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();

    private CapNhatThongTinNguoiDung() {}

    public static CapNhatThongTinNguoiDung getInstance() {
        if (instance==null){
            instance = new CapNhatThongTinNguoiDung();
        }
        return instance;
    } // singleton

    /**
     * Updates multiple fields at once (e.g. a full profile save).
     * This method persists changes to the JSON file and updates the session.
     */
    public void updateUser(Map<String, String> updates) { // list các field cần update + nội dung mới
        try {
            // Get current user from session
            NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn chưa đăng nhập!");
                return;
            }

            // Update user fields with new values
            if (updates.containsKey("name")) {
                currentUser.setHoTen(updates.get("name"));
            }
            if (updates.containsKey("phone")) {
                currentUser.setSoDienThoai(updates.get("phone"));
            }
            if (updates.containsKey("address")) {
                currentUser.setDiaChi(updates.get("address"));
            }
            if (updates.containsKey("bankAccount")) {
                currentUser.setSoTaiKhoan(updates.get("bankAccount"));
            }
            if (updates.containsKey("balance")) {
                currentUser.setSoDuKhaDung(Double.parseDouble(updates.get("balance")));
            }
            if (updates.containsKey("bankName")) {
                currentUser.setNganHang(updates.get("bankName"));
            }

            // ⭐ CRITICAL: Save updated user back to JSON file - Save vào file JSON, không thì không lưu cho lần đăng nhập tiếp
            khoLuuTruNguoiDung.capNhatNguoiDung(currentUser);

            // Generate new token with updated info and update session
            String newToken = TokenUtil.generateToken(currentUser);
            SessionManager.getInstance().setSession(currentUser, newToken);
            prefs.put("auth_token", newToken);

        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", "Không thể cập nhật thông tin người dùng! " + e.getMessage());
        }
    }

}