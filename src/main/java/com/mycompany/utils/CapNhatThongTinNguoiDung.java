package com.mycompany.utils;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.NguoiDung;
import javafx.scene.control.Alert;


import java.util.Map;
import java.util.prefs.Preferences; // save thong tin sau khi app dong

/**
 * CapNhatThongTinNguoiDung - Utility class để cập nhật thông tin người dùng
 *
 * MỤC ĐÍCH:
 * - Quản lý việc cập nhật thông tin người dùng từ giao diện (UI) vào hệ thống
 * - Cập nhật cả dữ liệu trong sessionManager và file JSON
 * - Cơ chế Singleton để đảm bảo chỉ có một instance duy nhất
 *
 * TÍNH NĂNG CHÍNH:
 * - Cập nhật: Tên, SĐT, Địa chỉ, Tài khoản ngân hàng, Số dư, Tên ngân hàng
 * - Tự động lưu vào file JSON để dữ liệu bền vững
 * - Cập nhật token JWT sau khi thay đổi thông tin
 * - Hiển thị thông báo lỗi cho người dùng
 */
public class CapNhatThongTinNguoiDung {

    // Singleton instance - đảm bảo chỉ có một CapNhatThongTinNguoiDung trong suốt ứng dụng
    private static CapNhatThongTinNguoiDung instance;

    // Preferences là Java API để lưu trữ cài đặt ứng dụng an toàn (dùng registry trên Windows)
    // Lưu trữ auth_token để khôi phục session sau khi app đóng
    private static final Preferences prefs =
            Preferences.userNodeForPackage(CapNhatThongTinNguoiDung.class);

    // Interface để lưu trữ dữ liệu người dùng - hiện tại dùng JSON
    // Có thể thay thế bằng database trong tương lai (design pattern: Strategy)
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();

    /**
     * Constructor private để ngăn tạo instance trực tiếp
     * Người dùng phải dùng getInstance() để có được instance duy nhất
     */
    private CapNhatThongTinNguoiDung() {}

    /**
     * PHƯƠNG THỨC: getInstance()
     * MỤC ĐÍCH: Singleton pattern - đảm bảo chỉ có một instance của class này
     *
     * GIẢI THÍCH:
     * - Kiểm tra nếu instance chưa được tạo thì tạo mới
     * - Lần sau gọi chỉ trả về instance cũ
     * - Thread-safe pattern: kiểm tra rồi tạo
     *
     * @return Instance duy nhất của CapNhatThongTinNguoiDung
     */
    public static CapNhatThongTinNguoiDung getInstance() {
        if (instance==null){
            instance = new CapNhatThongTinNguoiDung();
        }
        return instance;
    } // singleton

    /**
     * PHƯƠNG THỨC: updateUser(Map<String, String> updates)
     * MỤC ĐÍCH: Cập nhật nhiều trường thông tin của người dùng cùng lúc
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy người dùng hiện tại từ SessionManager
     * 2. Kiểm tra xem người dùng đã đăng nhập hay chưa
     * 3. Cập nhật các trường tương ứng nếu có trong map
     * 4. ⭐ LƯU VÀO FILE JSON - QUAN TRỌNG NHẤT (nếu không sẽ mất dữ liệu)
     * 5. Tạo token JWT mới với thông tin đã cập nhật
     * 6. Cập nhật SessionManager với dữ liệu mới
     * 7. Lưu token vào Preferences để session bền vững
     *
     * CÁC TRƯỜNG CÓ THỂ CẬP NHẬT:
     * - "name": Tên người dùng (Họ và tên)
     * - "phone": Số điện thoại
     * - "address": Địa chỉ
     * - "bankAccount": Số tài khoản ngân hàng
     * - "balance": Số dư khả dụng (phải là số)
     * - "bankName": Tên ngân hàng
     *
     * @param updates Map chứa cặp key-value của các trường cần cập nhật
     *                Ví dụ: updates.put("name", "Nguyễn Văn A");
     */
    public void updateUser(Map<String, String> updates) {
        try {
            // 🔹 BƯỚC 1: Lấy người dùng hiện tại từ session
            // SessionManager lưu trữ thông tin người dùng đã đăng nhập
            NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();

            // 🔹 BƯỚC 2: Kiểm tra người dùng tồn tại
            // Nếu null = chưa đăng nhập, không thể cập nhật
            if (currentUser == null) {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn chưa đăng nhập!");
                return;
            }

            // 🔹 BƯỚC 3: Cập nhật từng trường nếu có trong map
            // Dùng containsKey() để kiểm tra trước khi cập nhật

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
                // Chuyển từ String sang Double để lưu số
                currentUser.setSoDuKhaDung(Double.parseDouble(updates.get("balance")));
            }
            if (updates.containsKey("bankName")) {
                currentUser.setNganHang(updates.get("bankName"));
            }

            // 🔹 BƯỚC 4: ⭐ LƯU VÀO FILE JSON - QUAN TRỌNG NHẤT!!!
            // Nếu bỏ qua bước này, dữ liệu chỉ thay đổi trong RAM
            // Khi app đóng, tất cả thay đổi sẽ mất vĩnh viễn
            // capNhatNguoiDung() sẽ cập nhật bản ghi cũ (giữ nguyên ID)
            khoLuuTruNguoiDung.capNhatNguoiDung(currentUser);

            // 🔹 BƯỚC 5: Tạo token JWT mới với dữ liệu đã cập nhật
            // Token chứa tất cả thông tin người dùng dưới dạng Base64
            // Được sử dụng để xác thực và kiểm tra quyền
            String newToken = TokenUtil.generateToken(currentUser);

            // 🔹 BƯỚC 6: Cập nhật SessionManager với dữ liệu mới
            // Các controller khác có thể lấy thông tin từ SessionManager
            SessionManager.getInstance().setSession(currentUser, newToken);

            // 🔹 BƯỚC 7: Lưu token vào Preferences
            // Preferences tồn tại ngay cả sau khi app đóng
            // Lần tới mở app, có thể khôi phục session từ token này
            prefs.put("auth_token", newToken);

        } catch (Exception e) {
            // Bắt tất cả exception và hiển thị cho người dùng
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", "Không thể cập nhật thông tin người dùng! " + e.getMessage());
        }
    }

}