package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;

/**
 * SessionManager - Quản lý session (phiên đăng nhập) của người dùng
 *
 * MỤC ĐÍCH:
 * - Lưu trữ thông tin người dùng hiện tại đang đăng nhập
 * - Lưu trữ JWT token của người dùng
 * - Cung cấp các phương thức kiểm tra xem đã đăng nhập hay chưa
 * - Singleton pattern: chỉ có 1 session trong suốt ứng dụng
 *
 * VỊ TRÍ DÙNG:
 * - Khi người dùng đăng nhập thành công → setSession()
 * - Khi cần thông tin người dùng hiện tại → getCurrentUser()
 * - Khi kiểm tra xem đã đăng nhập hay chưa → isLoggedIn()
 * - Khi người dùng đăng xuất → logout()
 *
 * LƯU Ý:
 * - Session này chỉ lưu trong RAM
 * - Khi app đóng, session sẽ mất
 * - Để persistent login, cần lưu token vào Preferences
 */
public class SessionManager {

    // Singleton instance - đảm bảo chỉ có 1 SessionManager
    private static SessionManager instance;

    // Người dùng hiện tại đang đăng nhập
    // null = chưa đăng nhập
    private NguoiDung currentUser;

    // JWT token của người dùng hiện tại
    // Token chứa toàn bộ thông tin người dùng đã mã hóa Base64
    private String currentToken;

    /**
     * Constructor private - ngăn tạo instance trực tiếp
     * Phải dùng getInstance() để lấy singleton instance
     */
    private SessionManager() {}

    /**
     * PHƯƠNG THỨC: getInstance()
     * MỤC ĐÍCH: Singleton pattern - lấy instance duy nhất của SessionManager
     *
     * GIẢI THÍCH:
     * - Lần đầu gọi: tạo instance mới
     * - Lần sau gọi: trả về instance cũ
     * - Đảm bảo chỉ có 1 SessionManager duy nhất trong app
     *
     * @return Instance duy nhất
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * PHƯƠNG THỨC: setSession(NguoiDung user, String token)
     * MỤC ĐÍCH: Đặt thông tin session khi người dùng đăng nhập
     *
     * GIẢI THÍCH:
     * - Gọi sau khi xác thực thành công (username + password đúng)
     * - Lưu user object và token vào session
     * - Các controller khác có thể lấy thông tin từ đây
     *
     * @param user Object NguoiDung của người dùng vừa đăng nhập
     * @param token JWT Token của người dùng
     */
    public void setSession(NguoiDung user, String token) {
        this.currentUser = user;
        this.currentToken = token;
    }

    /**
     * PHƯƠNG THỨC: getCurrentUser()
     * MỤC ĐÍCH: Lấy object NguoiDung của người dùng hiện tại
     *
     * GIẢI THÍCH:
     * - Trả về null nếu chưa đăng nhập
     * - Có thể truy cập tất cả thông tin: tên, email, SĐT, v.v.
     * - Dùng để hiển thị thông tin profile
     *
     * @return NguoiDung object hoặc null
     */
    public NguoiDung getCurrentUser() {
        return currentUser;
    }

    /**
     * PHƯƠNG THỨC: getCurrentToken()
     * MỤC ĐÍCH: Lấy JWT token của người dùng hiện tại
     *
     * GIẢI THÍCH:
     * - Token là chuỗi Base64 chứa thông tin người dùng
     * - Format: [encoded_data].[signature]
     * - Dùng để xác thực request và kiểm tra quyền
     * - Nếu token hết hạn hoặc bị sửa, validateToken() sẽ return false
     *
     * @return Token string hoặc null
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * PHƯƠNG THỨC: getCurrentUserInfo()
     * MỤC ĐÍCH: Lấy thông tin người dùng dưới dạng Map từ token
     *
     * GIẢI THÍCH:
     * - Token được giải mã thành Map<String, Object>
     * - Chứa: userId, email, name, birth, address, phone, balance, bankAccount, bankName
     * - Nếu token null, trả về null
     * - Nếu token invalid, trả về null
     *
     * @return Map<String, Object> chứa thông tin người dùng hoặc null
     */
    public Map<String, Object> getCurrentUserInfo() {
        if (currentToken != null) {
            // TokenUtil.getUserInfoFromToken() giải mã token thành Map
            return TokenUtil.getUserInfoFromToken(currentToken);
        }
        return null;
    }

    /**
     * PHƯƠNG THỨC: isLoggedIn()
     * MỤC ĐÍCH: Kiểm tra xem người dùng đã đăng nhập hay chưa
     *
     * GIẢI THÍCH:
     * Người dùng được coi là "đã đăng nhập" khi:
     * 1. currentUser != null (user object tồn tại)
     * 2. currentToken != null (token tồn tại)
     * 3. TokenUtil.validateToken() return true (token hợp lệ)
     *
     * Nếu các điều kiện trên đều thỏa, trả về true
     * Ngược lại trả về false
     *
     * DÙNG CHO:
     * - Kiểm tra trước khi vào các trang protected
     * - Hiển thị/ẩn UI elements tùy theo login status
     * - Redirect về login page nếu chưa đăng nhập
     *
     * @return true nếu đã đăng nhập hợp lệ, false nếu chưa
     */
    public boolean isLoggedIn() {
        // Kiểm tra:
        // - currentUser != null: user object tồn tại
        // - currentToken != null: token tồn tại
        // - TokenUtil.validateToken(currentToken): token hợp lệ (không bị sửa/hết hạn)
        return currentUser != null && currentToken != null && TokenUtil.validateToken(currentToken);
    }

    /**
     * PHƯƠNG THỨC: logout()
     * MỤC ĐÍCH: Xóa session khi người dùng đăng xuất
     *
     * GIẢI THÍCH:
     * - Xóa currentUser (set = null)
     * - Xóa currentToken (set = null)
     * - isLoggedIn() sẽ trả về false
     * - Người dùng sẽ phải đăng nhập lại để vào các trang protected
     *
     * DÙNG CHO:
     * - Khi người dùng click button "Đăng xuất"
     * - Cleanup session trước khi quay về login page
     */
    public void logout() {
        this.currentUser = null;
        this.currentToken = null;
    }
}
