package com.mycompany.utils;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.User;
import javafx.scene.control.Alert;

import java.util.Map;
import java.util.prefs.Preferences;

/**
 * UserProfileUpdater - Cập nhật thông tin người dùng QUA SERVER (HTTP).
 *
 * Luồng đúng:
 *   UI → UserProfileUpdater.updateUser() → ApiClient.updateProfile/updateBalance()
 *        → PUT /api/users/profile  hoặc  PUT /api/users/balance
 *        → Server → SQLite
 *
 * KHÔNG còn gọi UserRepositorySQLite trực tiếp.
 */
public class UserProfileUpdater {

    private static final Preferences prefs =
            Preferences.userNodeForPackage(UserProfileUpdater.class);

    private UserProfileUpdater() {}

    private static volatile UserProfileUpdater instance;
    public static UserProfileUpdater getInstance() {
        if (instance == null) {
            synchronized (UserProfileUpdater.class) {
                if (instance == null) instance = new UserProfileUpdater();
            }
        }
        return instance;
    }

    /**
     * Cập nhật thông tin người dùng lên server, rồi đồng bộ lại SessionManager + token.
     *
     * @param updates  Map key-value các field cần thay đổi:
     *                 "name", "phone", "address", "bankAccount", "bankName", "avatar", "balance"
     */
    public void updateUser(Map<String, String> updates) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi", "Bạn chưa đăng nhập!");
                return;
            }

            String token = SessionManager.getInstance().getServerToken();

            // ── Tách "balance" ra gọi riêng endpoint /api/users/balance ──
            if (updates.containsKey("balance")) {
                double newBalance = Double.parseDouble(updates.get("balance"));
                boolean ok = ApiClient.updateBalance(newBalance, token);
                if (!ok) {
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.ERROR, "Lỗi cập nhật", "Không thể cập nhật số dư!");
                    return;
                }
                currentUser.setAvailableBalance(newBalance);
            }

            // ── Các field profile còn lại → /api/users/profile ──
            Map<String, String> profileUpdates = new java.util.HashMap<>(updates);
            profileUpdates.remove("balance"); // đã xử lý ở trên

            if (!profileUpdates.isEmpty()) {
                boolean ok = ApiClient.updateProfile(profileUpdates, token);
                if (!ok) {
                    HandleNavigationAndAlert.getInstance().showAlert(
                            Alert.AlertType.ERROR, "Lỗi cập nhật", "Không thể cập nhật thông tin!");
                    return;
                }
                // Cập nhật object in-memory
                if (profileUpdates.containsKey("name"))        currentUser.setFullName(profileUpdates.get("name"));
                if (profileUpdates.containsKey("phone"))       currentUser.setPhoneNumber(profileUpdates.get("phone"));
                if (profileUpdates.containsKey("address"))     currentUser.setAddress(profileUpdates.get("address"));
                if (profileUpdates.containsKey("bankAccount")) currentUser.setBankAccountNumber(profileUpdates.get("bankAccount"));
                if (profileUpdates.containsKey("bankName"))    currentUser.setBankName(profileUpdates.get("bankName"));
                if (profileUpdates.containsKey("avatar"))      currentUser.setAvatarPath(profileUpdates.get("avatar"));
            }

            // Tạo token mới phản ánh dữ liệu đã cập nhật, lưu vào session + prefs
            String newToken = TokenUtil.generateToken(currentUser);
            SessionManager.getInstance().setSession(currentUser, newToken);
            prefs.put("auth_token", newToken);

        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi cập nhật",
                    "Không thể cập nhật thông tin người dùng! " + e.getMessage());
        }
    }
}