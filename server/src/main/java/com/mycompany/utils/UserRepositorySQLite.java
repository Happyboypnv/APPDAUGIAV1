package com.mycompany.utils;

import com.mycompany.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * /// SELECT[cai muon lay] FROM [tu bang nao] WHERE [dieu kien loc]
 * /// INSERT INTO [tên_bảng] (tên_các_cột) VALUES (các_giá_trị_tương_ứng);
 * /// UPDATE [tên_bảng] SET [cột_cần_sửa] = [giá_trị_mới] WHERE [điều_kiện_để_tìm_đúng_người];
 * /// DELETE FROM [tên_bảng] WHERE [điều_kiện];
 * 1️⃣  LỖ HỔNG: SQL INJECTION (Lỗ hổng bảo mật)
 * 📌 NGUYÊN NHÂN:
 *    Khi ghép trực tiếp dữ liệu người dùng vào câu SQL mà không xử lý đặc biệt
 * ❌ CÁCH NGUY HIỂM (Dễ bị tấn công):
 *    String email = "phong@gmail.com";
 *    String sql = "SELECT * FROM nguoi_dung WHERE email = '" + email + "'";
 *    // Kết quả SQL: SELECT * FROM nguoi_dung WHERE email = 'phong@gmail.com'
 *    // → Cách này bình thường, nhưng nếu hacker nhập gì đó?
 *    HACKER NHẬP:  email = "' OR '1'='1"
 *    SQL BỊ BIẾN THÀNH:
 *      SELECT * FROM nguoi_dung WHERE email = '' OR '1'='1'
 *                                                ↑
 *                                    Điều kiện LUÔN ĐÚNG!
 *                      dau nhay don de bo qua viec nhap va lay luon True
 *    ⚠️  HỆ QUẢ: Trả về TẤT CẢ hàng trong bảng → hacker truy cập được tất cả tài khoản!
 * ═══════════════════════════════════════════════════════════════════════════════
 * 2️⃣  GIẢI PHÁP:   (An toàn ✅)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 📌 NGUYÊN LÝ:
 *    PreparedStatement tách RÕNG SQL từ DỮ LIỆU → hacker không thể tiêm SQL code
 *    /// nguyen ly nay co the hieu thay vi la code SQL bthg no se chuyen tat ca trong dau ? thanh xau
 *    /// -> chi dung khi xau khop voi du lieu trong database thi moi tra ve ket qua, neu khong khop thi tra ve rong
 * ✅ CÁCH AN TOÀN (Dùng PreparedStatement):
 *    String sql = "SELECT * FROM nguoi_dung WHERE email = ?";
 *    //                                                      ^
 *    //                                        Chỗ trống cho dữ liệu
 *    PreparedStatement ps = conn.prepareStatement(sql);
 *    ps.setString(1, email);  // Điền dữ liệu vào chỗ trống an toàn
 *    ResultSet rs = ps.executeQuery();
 *    QUY TRÌNH BÊN TRONG:
 *    1. Database nhận câu SQL với dấu ? (chế độ "template")
 *    2. Database biên dịch câu SQL → xác định cấu trúc của câu lệnh
 *    3. Dữ liệu (email) được gửi RIÊNG → không phải là SQL code
 *    4. Database chèn dữ liệu vào vị trí đủ an toàn
 *    HACKER NHẬP:  email = "' OR '1'='1"
 *    MÀ DATABASE NHẬN:
 *      - SQL template:  SELECT * FROM nguoi_dung WHERE email = ?
 *      - Dữ liệu:       ' OR '1'='1  (coi như string thường, khan phải SQL)
 *    ✅ KẾT QUẢ: Chỉ tìm email = "' OR '1'='1" (chuỗi ký tự thông thường)
 *                Không có hàng nào khớp → trả về rỗng
 *                Hacker không thể tấn công! 🛡️
 *     phan biet Statement va PreparedStatement:
 *     - Statement: Dùng cho câu SQL tĩnh, không có tham số → dễ bị SQL Injection nếu có dữ liệu người dùng
 *     - PreparedStatement: Dùng cho câu SQL động, có tham số → an toàn hơn, tránh SQL Injection
 */
public class UserRepositorySQLite implements IUserRepository {

  // Lock để đồng bộ hóa việc sinh mã người dùng trong môi trường đa luồng
  private static final Object ID_GENERATION_LOCK = new Object();
  private static final Logger logger = LoggerFactory.getLogger(UserRepositorySQLite.class);

  public UserRepositorySQLite() {
    DatabaseConnection.initialize();
  }

  /**
   * METHOD: luu()
   * Mục đích: Lưu một người dùng mới vào database.
   * Quy trình:
   * 1. Tự động sinh mã người dùng dạng: PPPT000001, PPPT000002, ...
   * 2. Set mã đó vào đối tượng User
   * 3. Thực thi câu INSERT để lưu vào database
   * @param User - Đối tượng người dùng cần lưu (chưa có mã)
   */
  @Override
  public void save(User User) {
    Connection conn = null;
    try {
      conn = DatabaseConnection.getConnection();

      // Kiểm tra email trong cùng transaction
      if (!isEmailAvailable(User.getEmail())) {
        logger.error("Email đã tồn tại: " + User.getEmail());
        return;
      }

      String maMoi = RandomIDGenerator();
      User.setUserId(maMoi);

      String sql = "INSERT INTO nguoi_dung " +
          "(ma_nguoi_dung, ho_ten, thu_dien_tu, mat_khau, salt, ngay_sinh, " +
          "dia_chi, so_dien_thoai,so_du_thuc_te, so_du_dong_bang, so_tai_khoan_ngan_hang, ten_ngan_hang, duong_dan_avatar, role, is_banned) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, maMoi);
        ps.setString(2, User.getFullName());
        ps.setString(3, User.getEmail());
        ps.setString(4, User.getPassword());
        ps.setString(5, User.getSalt());
        ps.setString(6, User.getDateOfBirth());
        ps.setString(7, User.getAddress());
        ps.setString(8, User.getPhoneNumber());
        ps.setDouble(9, User.getActualBalance());
        ps.setDouble(10, User.getFrozenBalance());
        ps.setString(11, User.getBankAccountNumber());
        ps.setString(12, User.getBankName());
        ps.setString(13, User.getAvatarPath());
        ps.setInt(14, User.getRole());
        ps.setInt(15, User.getIsBanned());

        logger.info("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
            maMoi, User.getFullName(), User.getEmail(), User.getPassword(), User.getSalt(),
            User.getDateOfBirth(), User.getAddress(), User.getPhoneNumber(),
            User.getAvailableBalance(), User.getBankAccountNumber(), User.getBankName(),
            User.getAvatarPath());

        int rowsAffected = ps.executeUpdate();
        logger.info(" INSERT thành công, số dòng ảnh hưởng: " + rowsAffected);
      }

      // Commit 1 lần duy nhất sau khi tất cả thao tác hoàn tất
      conn.commit();
      logger.info("COMMIT thành công cho user: " + maMoi);

    } catch (SQLException e) {
      // Rollback nếu có lỗi để giải phóng write lock
      if (conn != null) {
        try {
          conn.rollback();
          logger.warn("ROLLBACK do lỗi lưu user: " + e.getMessage());
        } catch (SQLException ex) {
          logger.error("Lỗi rollback: " + ex.getMessage());
        }
      }
      logger.error("Lỗi lưu người dùng: " + e.getMessage());
    }
  }

  @Override
  public void update(User User) {
    if (User == null || User.getUserId() == null) {
      logger.error("Không thể cập nhật: Người dùng null hoặc không có mã");
      return;
    }

    String sql = "UPDATE nguoi_dung SET " +
        "ho_ten = ?, thu_dien_tu = ?, mat_khau = ?, salt = ?, ngay_sinh = ?, " +
        "dia_chi = ?, so_dien_thoai = ?, so_du_thuc_te = ?, so_du_dong_bang = ?, so_du_kha_dung = ?, " +
        "so_tai_khoan_ngan_hang = ?, ten_ngan_hang = ?, duong_dan_avatar = ?, role = ?, is_banned = ? " +
        "WHERE ma_nguoi_dung = ?";

    Connection conn = null;
    try {
      conn = DatabaseConnection.getConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, User.getFullName());
        ps.setString(2, User.getEmail());
        ps.setString(3, User.getPassword());
        ps.setString(4, User.getSalt());
        ps.setString(5, User.getDateOfBirth());
        ps.setString(6, User.getAddress());
        ps.setString(7, User.getPhoneNumber());
        ps.setDouble(8, User.getActualBalance());
        ps.setDouble(9, User.getFrozenBalance());
        ps.setDouble(10, User.getAvailableBalance());
        ps.setString(11, User.getBankAccountNumber());
        ps.setString(12, User.getBankName());
        ps.setString(13, User.getAvatarPath());
        ps.setInt(14, User.getRole());
        ps.setInt(15, User.getIsBanned());
        ps.setString(16, User.getUserId());
        int rowsAffected = ps.executeUpdate();
        logger.info("Cập nhật user thành công, số dòng ảnh hưởng: " + rowsAffected);
      }
      conn.commit();
      logger.info("COMMIT cập nhật user: " + User.getUserId());
    } catch (SQLException e) {
      if (conn != null) {
        try { conn.rollback(); } catch (SQLException ex) { logger.error("Lỗi rollback update: " + ex.getMessage()); }
      }
      logger.error("Lỗi cập nhật người dùng: " + e.getMessage());
    }
  }

  public void authorizeAdmin(String email) {
    if (email == null || email.isBlank()) {
      logger.error("KhÃ´ng thá»ƒ cáº¥p quyá»n admin cho email rá»—ng");
      return;
    }

    String sql = "UPDATE nguoi_dung SET role = 1 WHERE thu_dien_tu = ?";
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
      ps.setString(1, email);
      int rowsAffected = ps.executeUpdate();
      ps.getConnection().commit();
      if (rowsAffected > 0) {
        logger.info("Cáº¥p quyá»n admin cho email {} thÃ nh cÃ´ng", email);
      } else {
        logger.warn("KhÃ´ng tÃ¬m tháº¥y email Ä‘á»ƒ cáº¥p quyá»n admin: {}", email);
      }
    } catch (SQLException e) {
      logger.error("Lá»—i cáº¥p quyá»n admin DB: " + e.getMessage());
    }
  }

  public void delete(User User) {
    if (User == null || User.getUserId() == null) {
      logger.error("Không thể xóa: Người dùng null hoặc không có mã");
      return;
    }

    Connection conn = null;
    try {
      conn = DatabaseConnection.getConnection();
      try (PreparedStatement ps = conn.prepareStatement("DELETE FROM nguoi_dung WHERE ma_nguoi_dung = ?")) {
        ps.setString(1, User.getUserId());
        int rowsAffected = ps.executeUpdate();
        logger.info("Xóa user thành công, số dòng ảnh hưởng: " + rowsAffected);
      }
      conn.commit();
      logger.info("COMMIT xóa user: " + User.getUserId());
    } catch (SQLException e) {
      if (conn != null) {
        try { conn.rollback(); } catch (SQLException ex) { logger.error("Lỗi rollback delete: " + ex.getMessage()); }
      }
      logger.error("Lỗi xóa người dùng: " + e.getMessage());
    }
  }

  /**
   * METHOD: layTatCa()
   * Mục đích: Lấy toàn bộ danh sách người dùng từ database.
   * Cấu trúc dữ liệu trả về:
   * - Map<String, User>
   * + Key   = email (thu_dien_tu) → tìm kiếm nhanh bằng email
   * + Value = đối tượng User tương ứng
   * Lợi ích của Map:
   * - Tìm người dùng bằng email: O(1) thay vì O(n)
   * - Kiểm tra email tồn tại: if (map.containsKey(email))
   *
   * @return Map - Key = email, Value = User object
   */
  @Override
  public Map<String, User> findAll() {
    // HashMap: hiệu suất cao, không đảm bảo thứ tự
    Map<String, User> result = new HashMap<>();

    // SELECT * = lấy tất cả cột từ bảng nguoi_dung
    String sql = "SELECT * FROM nguoi_dung";

    // try-with-resources: tự động đóng Statement và ResultSet
    try (Statement stmt   = DatabaseConnection.getConnection().createStatement();
         ResultSet rs     = stmt.executeQuery(sql)) {
      // while (rs.next()): lặp cho đến khi hết dữ liệu
      // rs.next() = di chuyển con trỏ xuống dòng tiếp theo
      //           = trả true nếu còn dòng, false nếu hết
      while (rs.next()) {
        // Lấy dữ liệu từ cột của dòng hiện tại
        // rs.getString("tên_cột") = lấy giá trị cột theo tên
        User nd = new User(
            rs.getString("ho_ten"),       // Tên đầy đủ
            rs.getString("thu_dien_tu"),  // Email
            rs.getString("mat_khau"),     // Password (đã mã hóa)
            rs.getString("ngay_sinh")     // Ngày sinh (dạng text)
        );

        // Set mã người dùng từ DB
        nd.setUserId(rs.getString("ma_nguoi_dung"));
        // Set thêm các field khác
        nd.setAddress(rs.getString("dia_chi"));
        nd.setPhoneNumber(rs.getString("so_dien_thoai"));
        nd.setActualBalance(rs.getDouble("so_du_thuc_te"));
        nd.setFrozenBalance(rs.getDouble("so_du_dong_bang"));
        nd.setBankAccountNumber(rs.getString("so_tai_khoan_ngan_hang")); // ⭐ thêm
        nd.setBankName(rs.getString("ten_ngan_hang"));
        nd.setAvatarPath(rs.getString("duong_dan_avatar")); // ⭐ Avatar path
        nd.setRole(rs.getInt("role"));
        nd.setIsBanned(rs.getInt("is_banned"));

        // FIX QUAN TRỌNG (App Restart Issue):
        // - Trước: Không retrieve/set salt từ database
        // - Sau: Retrieve và set salt từ database
        // - Lý do: Khi app restart, salt cần được load để password verification hoạt động
        // - Vấn đề: Nếu salt = null, login sẽ fail vì BoMaHoaMatKhau.kiemTraMatKhau() cần salt
        nd.setSalt(rs.getString("salt"));

        // Thêm vào Map: email làm key, User làm value
        // Nếu email chưa tồn tại → thêm vào
        // Nếu email đã tồn tại → cập nhật (ghi đè)
        result.put(nd.getEmail(), nd);
      }
    } catch (SQLException e) {
      // Nếu có lỗi → in lỗi, nhưng vẫn trả về Map rỗng
      logger.error("Lỗi đọc danh sách: " + e.getMessage());
    }

    // Trả về Map (có thể rỗng nếu DB không có người dùng hoặc có lỗi)
    return result;
  }
  @Override
  public int countAllUsers() {
    String sql = "SELECT COUNT(*) FROM nguoi_dung";
    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      if (rs.next()) {
        return rs.getInt(1); // Trả về con số đếm duy nhất
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   * METHOD: kiemTraUser()
   * Mục đích: Kiểm tra đăng nhập - xác minh email VÀ password có khớp không.
   * THAY ĐỔI QUAN TRỌNG (SQLite Migration):
   * - Trước: return matKhauTrongDB.equals(password) → so sánh plain text
   * - Sau: BoMaHoaMatKhau.kiemTraMatKhau() → verify hash với salt
   * Quy trình mới:
   * 1. Tìm người dùng theo email trong database
   * 2. Nếu tìm thấy → lấy salt và hash từ DB
   * 3. Sử dụng BoMaHoaMatKhau.kiemTraMatKhau() để verify password
   * 4. Nếu cả email và password đều khớp → return true (đăng nhập thành công)
   * 5. Nếu email không tồn tại hoặc password sai → return false (đăng nhập thất bại)
   * Tối ưu: SELECT mat_khau, salt (chỉ lấy dữ liệu cần thiết)
   * @param email    - Email người dùng nhập vào
   * @param password - Password người dùng nhập vào (plain text)
   * @return true = đăng nhập thành công, false = thất bại
   */
  @Override
  public boolean verifyCredentials(String email, String password) {
    // THAY ĐỔI: Chỉ lấy cột mat_khau và salt (cần thiết cho kiểm tra)
    // Trước: SELECT * → lấy tất cả cột (chậm hơn)
    // Sau: SELECT mat_khau, salt → chỉ lấy cần thiết (nhanh hơn)
    String sql = "SELECT mat_khau, salt FROM nguoi_dung WHERE thu_dien_tu = ?";
    // try-with-resources: tự động đóng PreparedStatement
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
      // Điền email vào placeholder ?
      // ps.setString(1, giá_trị) → đặt giá trị cho dấu ? thứ 1
      ps.setString(1, email);

      // ===== BƯỚC 3: Lấy kết quả =====
      // Wrap ResultSet trong try-with-resources riêng để auto-close
      try (ResultSet rs = ps.executeQuery()) {
        // rs.next() = kiểm tra xem có dòng nào khớp không
        // true = tìm thấy email, false = email không tồn tại
        if (rs.next()) {
          // THAY ĐỔI: Lấy password hash và salt từ cơ sở dữ liệu
          // Trước: Chỉ có mat_khau (plain text)
          // Sau: mat_khau (hashed) + salt (cho verification)
          String matKhauHashTrongDB = rs.getString("mat_khau");
          String saltTrongDB = rs.getString("salt");

          logger.info(" Tìm thấy user với email: " + email);
          logger.info(" Password hash trong DB: " + matKhauHashTrongDB);
          logger.info(" Salt trong DB: " + saltTrongDB);
          logger.info(" Password nhập vào: " + password);

          // THAY ĐỔI QUAN TRỌNG: Sử dụng BoMaHoaMatKhau để verify password
          // Trước: matKhauTrongDB.equals(password) → plain text comparison
          // Sau: BoMaHoaMatKhau.kiemTraMatKhau() → cryptographic verification
          // matKhauHashTrongDB != null && saltTrongDB != null → đảm bảo không null
          // BoMaHoaMatKhau.kiemTraMatKhau() → verify password với hash + salt
          return matKhauHashTrongDB != null && saltTrongDB != null &&
              com.mycompany.utils.PasswordEncoder.checkPassword(password, matKhauHashTrongDB, saltTrongDB);
        } else {
          logger.info(" Không tìm thấy user với email: " + email);
        }
      }
    } catch (SQLException e) {
      // Nếu có lỗi kết nối hoặc SQL → in lỗi
      logger.error("Lỗi kiểm tra đăng nhập: " + e.getMessage());
    }

    // Email không tồn tại hoặc password sai → đăng nhập thất bại
    return false;
  }
  @Override
  public boolean isBankAccountAvailable(String bankAccount) {
    // ===== BƯỚC 1: Chuẩn bị câu SELECT =====
    // SELECT 1 = chỉ trả về số 1 (là trick để check existence)
    // WHERE so_tai_khoan_ngan_hang = ? = tìm tài khoản cần kiểm tra
    String sql = "SELECT 1 FROM nguoi_dung WHERE so_tai_khoan_ngan_hang = ?";
    // try-with-resources: tự động đóng PreparedStatement
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
      // Điền tài khoản vào placeholder ?
      ps.setString(1, bankAccount);

      // ===== BƯỚC 3: Kiểm tra kết quả =====
      // Wrap ResultSet trong try-with-resources riêng để auto-close
      try (ResultSet rs = ps.executeQuery()) {
        // rs.next() = kiểm tra xem câu SELECT có trả về dòng nào không
        // true = tìm thấy tk (tk ĐÃ tồn tại)
        // false = không tìm thấy (tk chưa tồn tại)

        // return !rs.next() = đảo ngược logic
        // Nếu rs.next() = true (tk tồn tại) → return false
        // Nếu rs.next() = false (tk chưa có) → return true
        return !rs.next();
      }
    } catch (SQLException e) {
      // Nếu có lỗi kết nối → cho phép đăng ký (để không block user)
      // Quy tắc an toàn: nếu không chắc → cho phép user thử
      logger.error("Lỗi kiểm tra tài khoản: " + e.getMessage());
      return true;
    }
  }
  /**
   * METHOD: kiemTraEmail()
   * Mục đích: Kiểm tra email đã tồn tại trong database chưa.
   * Giá trị trả về:
   * - true  = email CHƯA tồn tại → cho phép đăng ký
   * - false = email ĐÃ tồn tại   → báo lỗi trùng email
   * Trick tối ưu: "SELECT 1" thay vì "SELECT *"
   *   - SELECT 1 = chỉ kiểm tra có dòng nào khớp không (không lấy dữ liệu thật)
   *   - Nhanh hơn SELECT * (không tải dữ liệu không cần thiết)
   *
   * @param email - Email cần kiểm tra
   * @return true = email chưa tồn tại (có thể đăng ký)
   *         false = email đã tồn tại (không thể đăng ký)
   */
  @Override
  public boolean isEmailAvailable(String email) {
    // ===== BƯỚC 1: Chuẩn bị câu SELECT =====
    // SELECT 1 = chỉ trả về số 1 (là trick để check existence)
    // WHERE thu_dien_tu = ? = tìm email cần kiểm tra
    String sql = "SELECT 1 FROM nguoi_dung WHERE thu_dien_tu = ?";
    // try-with-resources: tự động đóng PreparedStatement
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
      // Điền email vào placeholder ?
      ps.setString(1, email);

      // ===== BƯỚC 3: Kiểm tra kết quả =====
      // Wrap ResultSet trong try-with-resources riêng để auto-close
      try (ResultSet rs = ps.executeQuery()) {
        // rs.next() = kiểm tra xem câu SELECT có trả về dòng nào không
        // true = tìm thấy email (email ĐÃ tồn tại)
        // false = không tìm thấy (email chưa tồn tại)

        // return !rs.next() = đảo ngược logic
        // Nếu rs.next() = true (email tồn tại) → return false
        // Nếu rs.next() = false (email chưa có) → return true
        return !rs.next();
      }
    } catch (SQLException e) {
      // Nếu có lỗi kết nối → cho phép đăng ký (để không block user)
      // Quy tắc an toàn: nếu không chắc → cho phép user thử
      logger.error("Lỗi kiểm tra email: " + e.getMessage());
      return true;
    }
  }

  /**
   * METHOD (PRIVATE): sinhMaMoi()
   * Mục đích: Tự động sinh mã người dùng tăng dần.
   * Quy trình:
   * 1. Đếm số người dùng hiện có: SELECT COUNT(*)
   * 2. Lấy số đếm, cộng thêm 1 để tạo số thứ tự mới
   * 3. Format thành: "PPTT" + 6 chữ số (vd: PPTT000001)
   * Ví dụ:
   * - DB có 5 người dùng → sinh mã PPTT000006
   * - DB có 42 người dùng → sinh mã PPTT000043
   * - DB có 0 người dùng (lần đầu) → sinh mã PPPT000001
   * Fallback: Nếu có lỗi bất ngờ → return "PPTT000001"
   * Thread-safe: Sử dụng synchronized để tránh trùng lặp ID trong đa luồng
   *
   * @return String - Mã người dùng mới (dạng PPTT000001)
   */
  private String RandomIDGenerator() {
    synchronized (ID_GENERATION_LOCK) {
      String sql = "SELECT MAX(CAST(SUBSTR(ma_nguoi_dung, 5) AS INTEGER)) " +
          "FROM nguoi_dung";
      try (Statement stmt = DatabaseConnection.getConnection().createStatement();
           ResultSet rs   = stmt.executeQuery(sql)) {
        if (rs.next()) {
          int maxVal = rs.getInt(1); // getInt trả 0 nếu MAX = NULL (bảng rỗng)
          return String.format("PPTT%06d", maxVal + 1);
        }
      }
      catch (SQLException e) {
        logger.error("Lỗi sinh mã người dùng mới: " + e.getMessage());
      }
      return "PPTT000001";
    }
  }
  public User findByEmail(String email) {
    String sql = "SELECT * FROM nguoi_dung WHERE thu_dien_tu = ?";
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          User nd = new User(
              rs.getString("ho_ten"),
              rs.getString("thu_dien_tu"),
              rs.getString("mat_khau"),
              rs.getString("ngay_sinh")
          );
          nd.setUserId(rs.getString("ma_nguoi_dung"));
          nd.setAddress(rs.getString("dia_chi"));
          nd.setPhoneNumber(rs.getString("so_dien_thoai"));
          nd.setActualBalance(rs.getDouble("so_du_thuc_te"));
          nd.setFrozenBalance(rs.getDouble("so_du_dong_bang"));
          nd.setBankAccountNumber(rs.getString("so_tai_khoan_ngan_hang")); // ⭐ thêm
          nd.setBankName(rs.getString("ten_ngan_hang"));
          nd.setAvatarPath(rs.getString("duong_dan_avatar"));
          nd.setRole(rs.getInt("role"));
          nd.setIsBanned(rs.getInt("is_banned"));
          nd.setSalt(rs.getString("salt"));
          return nd;
        }
      }
    } catch (SQLException e) {
      logger.error("Lỗi layTheoEmail: " + e.getMessage());
    }
    return null;
  }



  /**
   * METHOD: migratePlainTextPasswords()
   * Mục đích: Migrate mật khẩu plain text (từ JSON) sang hashed passwords
   * THAY ĐỔI QUAN TRỌNG (SQLite Migration):
   * - Lý do thêm: Users cũ từ JSON có password plain text
   * - Vấn đề: Login logic mới expect hashed passwords
   * - Giải pháp: Tự động migrate khi app khởi động
   * Quy trình migration:
   * 1. Tìm tất cả users có salt = null hoặc rỗng (users cũ)
   * 2. Đối với mỗi user đó:
   *    - Lấy password hiện tại (plain text từ JSON)
   *    - Tạo salt mới (16 bytes random)
   *    - Hash password với salt mới (SHA-256)
   *    - Cập nhật DB với hash và salt mới
   * 3. Users mới: Đã có salt từ lúc đăng ký
   * Khi nào gọi: Trong App.java, sau khi DB init, trước khi load UI
   * Thread-safe: Chạy một lần khi app start, không có concurrent issues
   * Kết quả: Tất cả users đều có password hashed + salt
   */
  public void migrateLegacyPasswords() {
    logger.info("🔄 Bắt đầu migrate mật khẩu plain text...");

    // THAY ĐỔI: Query để tìm users cần migrate
    // salt IS NULL OR salt = '' → users từ JSON migration
    String sql = "SELECT ma_nguoi_dung, mat_khau FROM nguoi_dung WHERE salt IS NULL OR salt = ''";
    try (Statement stmt = DatabaseConnection.getConnection().createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      int migratedCount = 0;
      while (rs.next()) {
        String maUser = rs.getString("ma_nguoi_dung");
        String plainPassword = rs.getString("mat_khau");

        // THAY ĐỔI: Tạo salt và hash mới cho migration
        // BoMaHoaMatKhau.taoSalt() → 16 bytes random Base64
        // BoMaHoaMatKhau.maHoaMatKhau() → SHA-256 hash
        String newSalt = com.mycompany.utils.PasswordEncoder.createSalt();
        String hashedPassword = com.mycompany.utils.PasswordEncoder.passwordEncoder(plainPassword, newSalt);

        // THAY ĐỔI: Cập nhật DB với password đã hash
        // Sử dụng PreparedStatement để tránh SQL injection
        String updateSql = "UPDATE nguoi_dung SET mat_khau = ?, salt = ? WHERE ma_nguoi_dung = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(updateSql)) {
          ps.setString(1, hashedPassword);  // Password đã hash
          ps.setString(2, newSalt);          // Salt mới
          ps.setString(3, maUser);     // WHERE condition
          ps.executeUpdate();
          ps.getConnection().commit();      // THAY ĐỔI: Explicit commit cho WAL mode
          migratedCount++;
          logger.info("✅ Migrated user: " + maUser);
        }
      }

      logger.info("🎉 Hoàn thành migrate " + migratedCount + " users");

    } catch (SQLException e) {
      logger.error("❌ Lỗi migrate passwords: " + e.getMessage());
    }
  }

  /**
   * Đặt giá: Tăng frozen_balance, cập nhật available cùng lúc, kiểm tra available_balance >= amount.
   * Chạy trong TRANSACTION để tránh race condition.
   * @return true nếu thành công, false nếu không đủ tiền
   */
  @Override
  public boolean holdBalance(String userId, String maPhien, double amount) {
    String sql = "UPDATE nguoi_dung " +
        "SET so_du_dong_bang = so_du_dong_bang + ?, " +
        "    so_du_kha_dung = so_du_thuc_te - (so_du_dong_bang + ?) " + // cập nhật available dựa trên frozen mới
        "WHERE ma_nguoi_dung = ? " +
        "AND (so_du_thuc_te - so_du_dong_bang) >= ?"; // kiểm tra available >= amount

    try {
      Connection conn = DatabaseConnection.getConnection();
      conn.setAutoCommit(false);
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setDouble(1, amount); // so_du_dong_bang + ?
        ps.setDouble(2, amount); // dùng trong so_du_kha_dung expression
        ps.setString(3, userId);
        ps.setDouble(4, amount); // điều kiện available >= amount
        int rows = ps.executeUpdate();
        if (rows == 0) {
          conn.rollback();
          return false; // không đủ tiền hoặc user không tồn tại
        }
        // Ghi escrow log
        logEscrow(conn, userId, maPhien, amount);
        conn.commit();
        logger.info("holdBalance thành công: {} frozen +{} VNĐ", userId, amount);
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(false); // restore
      }
    } catch (SQLException e) {
      logger.error("Lỗi holdBalance: " + e.getMessage());
      return false;
    }
  }

  /**
   * Bị vượt giá (Outbid): Giải phóng frozen của người cũ.
   */
  @Override
  public void releaseHold(String userId, String maPhien, double amount) {
    String sql = "UPDATE nguoi_dung " +
        "SET so_du_dong_bang = CASE WHEN so_du_dong_bang >= ? THEN so_du_dong_bang - ? ELSE 0 END, " +
        "    so_du_kha_dung = so_du_thuc_te - CASE WHEN so_du_dong_bang >= ? THEN so_du_dong_bang - ? ELSE 0 END " +
        "WHERE ma_nguoi_dung = ?";
    try {
      Connection conn = DatabaseConnection.getConnection();
      conn.setAutoCommit(false);
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        // dùng cùng amount cho cả chỗ trừ
        ps.setDouble(1, amount);
        ps.setDouble(2, amount);
        ps.setDouble(3, amount);
        ps.setDouble(4, amount);
        ps.setString(5, userId);
        ps.executeUpdate();
        deleteEscrowLog(conn, userId, maPhien);
        conn.commit();
        logger.info("releaseHold thành công: {} release {} VNĐ", userId, amount);
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(false);
      }
    } catch (SQLException e) {
      logger.error("Lỗi releaseHold: " + e.getMessage());
    }
  }

  /**
   * Thắng cuộc: Khấu trừ actual_balance, xóa frozen tương ứng, cập nhật available.
   */
  @Override
  public boolean deductOnWin(String userId, String maPhien, double amount) {
    String sql = "UPDATE nguoi_dung " +
        "SET so_du_thuc_te = so_du_thuc_te - so_du_dong_bang, " +
        "    so_du_kha_dung = so_du_thuc_te - so_du_dong_bang, " +
        "    so_du_dong_bang = 0 " +
        "WHERE ma_nguoi_dung = ? AND so_du_dong_bang >= ? AND so_du_thuc_te >= so_du_dong_bang";
    try {
      Connection conn = DatabaseConnection.getConnection();
      conn.setAutoCommit(false);
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, userId);
        ps.setDouble(2, amount);
        int rows = ps.executeUpdate();
        if (rows == 0) {
          conn.rollback();
          logger.warn("deductOnWin thất bại: userId={} amount={}", userId, amount);
          return false;
        }
        deleteEscrowLog(conn, userId, maPhien);
        conn.commit();
        logger.info("deductOnWin thành công: {} trừ {} VNĐ", userId, amount);
        return true;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(false);
      }
    } catch (SQLException e) {
      logger.error("Lỗi deductOnWin: " + e.getMessage());
      return false;
    }
  }

  // Helper: ghi escrow log
  private void logEscrow(Connection conn, String userId, String maPhien, double amount) throws SQLException {
    String sql = "INSERT INTO escrow_log (ma_nguoi_dung, ma_phien, so_tien_dong_bang, thoi_gian) VALUES (?,?,?,?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, maPhien);
      ps.setDouble(3, amount);
      ps.setString(4, java.time.LocalDateTime.now().toString());
      ps.executeUpdate();
    }
  }

  // Helper: xóa escrow log khi giải phóng
  private void deleteEscrowLog(Connection conn, String userId, String maPhien) throws SQLException {
    String sql = "DELETE FROM escrow_log WHERE ma_nguoi_dung = ? AND ma_phien = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, maPhien);
      ps.executeUpdate();
    }
  }
}
