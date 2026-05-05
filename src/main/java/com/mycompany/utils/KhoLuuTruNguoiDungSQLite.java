package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
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
 * 2️⃣  GIẢI PHÁP: PREPAREDSTATEMENT (An toàn ✅)
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
 *      - Dữ liệu:       ' OR '1'='1  (coi như string thường, không phải SQL)
 *    ✅ KẾT QUẢ: Chỉ tìm email = "' OR '1'='1" (chuỗi ký tự thông thường)
 *                Không có hàng nào khớp → trả về rỗng
 *                Hacker không thể tấn công! 🛡️
 *     phan biet Statement va PreparedStatement:
 *     - Statement: Dùng cho câu SQL tĩnh, không có tham số → dễ bị SQL Injection nếu có dữ liệu người dùng
 *     - PreparedStatement: Dùng cho câu SQL động, có tham số → an toàn hơn, tránh SQL Injection
 */
public class KhoLuuTruNguoiDungSQLite implements IKhoLuuTruNguoiDung {

    // Lock để đồng bộ hóa việc sinh mã người dùng trong môi trường đa luồng
    private static final Object ID_GENERATION_LOCK = new Object();

    /**
     * METHOD: luu()
     * Mục đích: Lưu một người dùng mới vào database.
     * Quy trình:
     * 1. Tự động sinh mã người dùng dạng: PPPT000001, PPPT000002, ...
     * 2. Set mã đó vào đối tượng NguoiDung
     * 3. Thực thi câu INSERT để lưu vào database
     * @param nguoiDung - Đối tượng người dùng cần lưu (chưa có mã)
     */
    @Override
    public void luu(NguoiDung nguoiDung) {
        // Hàm sinhMaMoi() sẽ đếm số người dùng hiện có rồi +1
        // kiemTraEmail() returns true if email DOESN'T exist (safe to register)
        // So we check if it returns FALSE (email DOES exist) → reject
        if(!kiemTraEmail(nguoiDung.layThuDienTu())) {
            System.err.println("Email đã tồn tại: " + nguoiDung.layThuDienTu());
            return;
        }
        String maMoi = sinhMaMoi();
        // Gán mã mới vào đối tượng người dùng
        nguoiDung.setMaNguoiDung(maMoi);
        // Dấu ? là placeholder - để lại chỗ trống cho dữ liệu thực tế
        // Dùng placeholder tránh SQL Injection - an toàn hơn ghép string thô
        String sql = "INSERT INTO nguoi_dung " +
                "(ma_nguoi_dung, ho_ten, thu_dien_tu, mat_khau, salt, ngay_sinh, dia_chi, so_dien_thoai, so_du_kha_dung) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // AN TOÀN (DÙNG CÁI NÀY):
        // Dùng ? là "chỗ trống", không ghép string
        // Database sẽ xử lý ? như một tham số dữ liệu, không phải SQL code
        // try-with-resources: tự động đóng PreparedStatement (đóng tài nguyên)
        // Ngay khi ra khỏi try block → PreparedStatement tự gọi close()
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            // Điền dữ liệu vào các vị trí ? (đánh số từ 1, không phải 0)
            // ps.setString(vị_trí, giá_trị);
            ps.setString(1, maMoi);                          // Vị trí 1 = ma_nguoi_dung
            ps.setString(2, nguoiDung.layHoTen());          // Vị trí 2 = ho_ten
            ps.setString(3, nguoiDung.layThuDienTu());      // Vị trí 3 = thu_dien_tu (email)
            ps.setString(4, nguoiDung.layMatKhau());        // Vị trí 4 = mat_khau
            ps.setString(5, nguoiDung.laySalt());           // Vị trí 5 = salt
            ps.setString(6, nguoiDung.layNgaySinh());       // Vị trí 6 = ngay_sinh
            ps.setString(7, nguoiDung.getDiaChi());        // Vị trí 7 = dia_chi
            ps.setString(8, nguoiDung.getSoDienThoai());  // Vị trí 8 = so_dien_thoai
            ps.setDouble(9, nguoiDung.getSoDuKhaDung()); // Vị trí 9 = so_du_kha_dung

            // Thực thi câu lệnh INSERT
            // executeUpdate() = dùng cho INSERT, UPDATE, DELETE (không lấy dữ liệu trả về)
            // Trả về số dòng bị ảnh hưởng (1 = thành công)
            int rowsAffected = ps.executeUpdate();
            System.out.println(" INSERT thành công, số dòng ảnh hưởng: " + rowsAffected);

            // ĐẢM BẢO DATA ĐƯỢC LƯU VÀO DATABASE
            // SQLite mặc định auto-commit = true, nhưng với WAL mode cần explicit commit
            ps.getConnection().commit();
            System.out.println("COMMIT thành công cho user: " + maMoi);

        } catch (SQLException e) {
            // Nếu có lỗi (kết nối thất bại, SQL sai, ...) → in lỗi ra stderr
            System.err.println("Lỗi lưu người dùng: " + e.getMessage());
        }
    }

    @Override
    public void capNhatNguoiDung(NguoiDung nguoiDung) {
        // Kiểm tra người dùng có tồn tại không
        if (nguoiDung == null || nguoiDung.layMaNguoiDung() == null) {
            System.err.println("Không thể cập nhật: Người dùng null hoặc không có mã");
            return;
        }

        String sql = "UPDATE nguoi_dung SET " +
                "ho_ten = ?, thu_dien_tu = ?, mat_khau = ?, salt = ?, ngay_sinh = ?, dia_chi = ?, so_dien_thoai = ?, so_du_kha_dung = ? " +
                "WHERE ma_nguoi_dung = ?";

        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            // Điền dữ liệu vào các vị trí ?
            ps.setString(1, nguoiDung.layHoTen());          // ho_ten
            ps.setString(2, nguoiDung.layThuDienTu());      // thu_dien_tu (email)
            ps.setString(3, nguoiDung.layMatKhau());        // mat_khau
            ps.setString(4, nguoiDung.laySalt());           // salt
            ps.setString(5, nguoiDung.layNgaySinh());       // ngay_sinh
            ps.setString(6, nguoiDung.getDiaChi());         // dia_chi
            ps.setString(7, nguoiDung.getSoDienThoai());    // so_dien_thoai
            ps.setDouble(8, nguoiDung.getSoDuKhaDung());    // so_du_kha_dung
            ps.setString(9, nguoiDung.layMaNguoiDung());    // WHERE ma_nguoi_dung

            int rowsAffected = ps.executeUpdate();
            System.out.println("Cập nhật user thành công, số dòng ảnh hưởng: " + rowsAffected);

            // Commit để đảm bảo data được cập nhật
            ps.getConnection().commit();
            System.out.println("COMMIT cập nhật user: " + nguoiDung.layMaNguoiDung());

        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật người dùng: " + e.getMessage());
        }
    }

    /**
     * METHOD: layTatCa()
     * Mục đích: Lấy toàn bộ danh sách người dùng từ database.
     * Cấu trúc dữ liệu trả về:
     * - Map<String, NguoiDung>
     *   + Key   = email (thu_dien_tu) → tìm kiếm nhanh bằng email
     *   + Value = đối tượng NguoiDung tương ứng
     * Lợi ích của Map:
     *   - Tìm người dùng bằng email: O(1) thay vì O(n)
     *   - Kiểm tra email tồn tại: if (map.containsKey(email))
     *
     * @return Map - Key = email, Value = NguoiDung object
     */
    @Override
    public Map<String, NguoiDung> layTatCa() {
        // HashMap: hiệu suất cao, không đảm bảo thứ tự
        Map<String, NguoiDung> result = new HashMap<>();

        // SELECT * = lấy tất cả cột từ bảng nguoi_dung
        String sql = "SELECT * FROM nguoi_dung";

        // try-with-resources: tự động đóng Statement và ResultSet
        try (Statement stmt   = KetNoiCSDL.layKetNoi().createStatement();
             ResultSet rs     = stmt.executeQuery(sql)) {
            // while (rs.next()): lặp cho đến khi hết dữ liệu
            // rs.next() = di chuyển con trỏ xuống dòng tiếp theo
            //           = trả true nếu còn dòng, false nếu hết
            while (rs.next()) {
                // Lấy dữ liệu từ cột của dòng hiện tại
                // rs.getString("tên_cột") = lấy giá trị cột theo tên
                NguoiDung nd = new NguoiDung(
                        rs.getString("ho_ten"),       // Tên đầy đủ
                        rs.getString("thu_dien_tu"),  // Email
                        rs.getString("mat_khau"),     // Password (đã mã hóa)
                        rs.getString("ngay_sinh")     // Ngày sinh (dạng text)
                );

                // Set mã người dùng từ DB
                nd.setMaNguoiDung(rs.getString("ma_nguoi_dung"));
                // Set thêm các field khác
                nd.setDiaChi(rs.getString("dia_chi"));
                nd.setSoDienThoai(rs.getString("so_dien_thoai"));
                nd.setSoDuKhaDung(rs.getDouble("so_du_kha_dung"));

                // FIX QUAN TRỌNG (App Restart Issue):
                // - Trước: Không retrieve/set salt từ database
                // - Sau: Retrieve và set salt từ database
                // - Lý do: Khi app restart, salt cần được load để password verification hoạt động
                // - Vấn đề: Nếu salt = null, login sẽ fail vì BoMaHoaMatKhau.kiemTraMatKhau() cần salt
                nd.setSalt(rs.getString("salt"));

                // Thêm vào Map: email làm key, NguoiDung làm value
                // Nếu email chưa tồn tại → thêm vào
                // Nếu email đã tồn tại → cập nhật (ghi đè)
                result.put(nd.layThuDienTu(), nd);
            }
        } catch (SQLException e) {
            // Nếu có lỗi → in lỗi, nhưng vẫn trả về Map rỗng
            System.err.println("Lỗi đọc danh sách: " + e.getMessage());
        }

        // Trả về Map (có thể rỗng nếu DB không có người dùng hoặc có lỗi)
        return result;
    }

    public void xoa(NguoiDung nguoiDung){
        // Kiểm tra người dùng có tồn tại không
        if (nguoiDung == null || nguoiDung.layMaNguoiDung() == null) {
            System.err.println("Không thể xóa: Người dùng null hoặc không có mã");
            return;
        }

        String maNguoiDung = nguoiDung.layMaNguoiDung();
        String sql = "DELETE FROM nguoi_dung WHERE ma_nguoi_dung = ?";

        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maNguoiDung);

            int rowsAffected = ps.executeUpdate();
            System.out.println("Xóa user thành công, số dòng ảnh hưởng: " + rowsAffected);

            // Commit để đảm bảo data được xóa
            ps.getConnection().commit();
            System.out.println("COMMIT xóa user: " + maNguoiDung);

        } catch (SQLException e) {
            System.err.println("Lỗi xóa người dùng: " + e.getMessage());
        }
    }
    /**
     * METHOD: kiemTraNguoiDung()
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
    public boolean kiemTraNguoiDung(String email, String password) {
        // THAY ĐỔI: Chỉ lấy cột mat_khau và salt (cần thiết cho kiểm tra)
        // Trước: SELECT * → lấy tất cả cột (chậm hơn)
        // Sau: SELECT mat_khau, salt → chỉ lấy cần thiết (nhanh hơn)
        String sql = "SELECT mat_khau, salt FROM nguoi_dung WHERE thu_dien_tu = ?";
        // try-with-resources: tự động đóng PreparedStatement
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
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

                    System.out.println(" Tìm thấy user với email: " + email);
                    System.out.println(" Password hash trong DB: " + matKhauHashTrongDB);
                    System.out.println(" Salt trong DB: " + saltTrongDB);
                    System.out.println(" Password nhập vào: " + password);

                    // THAY ĐỔI QUAN TRỌNG: Sử dụng BoMaHoaMatKhau để verify password
                    // Trước: matKhauTrongDB.equals(password) → plain text comparison
                    // Sau: BoMaHoaMatKhau.kiemTraMatKhau() → cryptographic verification
                    // matKhauHashTrongDB != null && saltTrongDB != null → đảm bảo không null
                    // BoMaHoaMatKhau.kiemTraMatKhau() → verify password với hash + salt
                    return matKhauHashTrongDB != null && saltTrongDB != null &&
                           com.mycompany.utils.BoMaHoaMatKhau.kiemTraMatKhau(password, matKhauHashTrongDB, saltTrongDB);
                } else {
                    System.out.println(" Không tìm thấy user với email: " + email);
                }
            }
        } catch (SQLException e) {
            // Nếu có lỗi kết nối hoặc SQL → in lỗi
            System.err.println("Lỗi kiểm tra đăng nhập: " + e.getMessage());
        }

        // Email không tồn tại hoặc password sai → đăng nhập thất bại
        return false;
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
    public boolean kiemTraEmail(String email) {
        // ===== BƯỚC 1: Chuẩn bị câu SELECT =====
        // SELECT 1 = chỉ trả về số 1 (là trick để check existence)
        // WHERE thu_dien_tu = ? = tìm email cần kiểm tra
        String sql = "SELECT 1 FROM nguoi_dung WHERE thu_dien_tu = ?";
        // try-with-resources: tự động đóng PreparedStatement
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
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
            System.err.println("Lỗi kiểm tra email: " + e.getMessage());
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
    private String sinhMaMoi() {
        synchronized (ID_GENERATION_LOCK) {
            // COUNT(*) = đếm số dòng trong bảng (số người dùng hiện có)
            String sql = "SELECT COUNT(*) FROM nguoi_dung";
            // try-with-resources: tự động đóng Statement và ResultSet
            try (Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
                 ResultSet rs   = stmt.executeQuery(sql)) {

                // COUNT(*) luôn trả về đúng 1 dòng (không bao giờ rỗng)
                if (rs.next()) {
                    // rs.getInt(1) = lấy giá trị cột đầu tiên (cột 1) dưới dạng int
                    // Cột 1 = kết quả của COUNT(*)
                    int soHienCo = rs.getInt(1);

                    // ===== BƯỚC 4: Tạo mã mới =====
                    // String.format("PPTT%06d", soHienCo + 1)
                    //   PPPT = tiền tố cố định
                    //   %06d = định dạng số nguyên, tối thiểu 6 chữ số
                    //          nếu không đủ → thêm số 0 ở đầu
                    //   soHienCo + 1 = con số cần format
                    // Ví dụ: soHienCo=5 → "PPPT000006"
                    return String.format("PPPT%06d", soHienCo + 1);
                }
            } catch (SQLException e) {
                // Nếu có lỗi kết nối hoặc SQL → in lỗi
                System.err.println("Lỗi sinh mã: " + e.getMessage());
            }

            // Fallback: Nếu có lỗi bất ngờ → return mã mặc định
            // Dùng mã này cho người dùng đầu tiên (khi DB rỗng hoặc lỗi)
            return "PPPT000001";
        }
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
    public void migratePlainTextPasswords() {
        System.out.println("🔄 Bắt đầu migrate mật khẩu plain text...");

        // THAY ĐỔI: Query để tìm users cần migrate
        // salt IS NULL OR salt = '' → users từ JSON migration
        String sql = "SELECT ma_nguoi_dung, mat_khau FROM nguoi_dung WHERE salt IS NULL OR salt = ''";
        try (Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int migratedCount = 0;
            while (rs.next()) {
                String maNguoiDung = rs.getString("ma_nguoi_dung");
                String plainPassword = rs.getString("mat_khau");

                // THAY ĐỔI: Tạo salt và hash mới cho migration
                // BoMaHoaMatKhau.taoSalt() → 16 bytes random Base64
                // BoMaHoaMatKhau.maHoaMatKhau() → SHA-256 hash
                String newSalt = com.mycompany.utils.BoMaHoaMatKhau.taoSalt();
                String hashedPassword = com.mycompany.utils.BoMaHoaMatKhau.maHoaMatKhau(plainPassword, newSalt);

                // THAY ĐỔI: Cập nhật DB với password đã hash
                // Sử dụng PreparedStatement để tránh SQL injection
                String updateSql = "UPDATE nguoi_dung SET mat_khau = ?, salt = ? WHERE ma_nguoi_dung = ?";
                try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(updateSql)) {
                    ps.setString(1, hashedPassword);  // Password đã hash
                    ps.setString(2, newSalt);          // Salt mới
                    ps.setString(3, maNguoiDung);     // WHERE condition
                    ps.executeUpdate();
                    ps.getConnection().commit();      // THAY ĐỔI: Explicit commit cho WAL mode
                    migratedCount++;
                    System.out.println("✅ Migrated user: " + maNguoiDung);
                }
            }

            System.out.println("🎉 Hoàn thành migrate " + migratedCount + " users");

        } catch (SQLException e) {
            System.err.println("❌ Lỗi migrate passwords: " + e.getMessage());
        }
    }
}
