package com.mycompany.utils;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
/**
 * Implementation của IKhoLuuTruAuctionSession sử dụng SQLite.
 * Đảm bảo thread-safe với ThreadLocal connections và synchronized ID generation.
 */
public class AuctionRepositorySQLite implements IAuctionRepository {

    // Lock để đồng bộ hóa việc sinh mã phiên đấu giá trong môi trường đa luồng
    private static final Object AUCTION_ID_GENERATION_LOCK = new Object();
    private static final Logger logger = LoggerFactory.getLogger(AuctionRepositorySQLite.class);
    /**
     * Method(private): sinhMaAuctionSession()
     * 1. dem so luong auction da co
     * 2. tang dem len 1
     * 3. Format thanh "PH" + 6 chu so (vd: PH000001)
     * dung synchronized de tranh truong ID trong duong thuc daugia
     */
    private String RandNewAuctionId(){
        synchronized (AUCTION_ID_GENERATION_LOCK) {
            String sql = "SELECT MAX(CAST(SUBSTR(ma_phien, 3) AS INTEGER)) " +
                    "FROM phien_dau_gia";
            try (Statement stmt = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs   = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int maxVal = rs.getInt(1); // getInt trả 0 nếu MAX = NULL (bảng rỗng)
                    return String.format("PH%06d", maxVal + 1);
                }
            }
            catch (SQLException e) {
                logger.error("[ERROR] Lỗi khi sinh mã phiên đấu giá: " + e.getMessage());
            }
            return "PH000001";
        }
    }
    /**
     * Method : LuuAuctionSession()
     * 1. sinh ma auction
     * 2. setMaAuctionSession
     * 3. Thuc thi cau insert
     */
    @Override
    public void save(AuctionSession AuctionSession) {
        /// kiem tra auction ton tai
        if(isAuctionAvailable(AuctionSession.getAuctionSessionId())){
            logger.info("[WARN] Phiên đấu giá đã tồn tại: " + AuctionSession.getAuctionSessionId());
            return;
        }
        // FIX: kiemTraauctionTonTai() thực thi SELECT, mở transaction ngầm nhưng không commit.
        // Nếu không commit ngay, SQLite giữ read-lock → block writer thread khác → SQLITE_BUSY.
        // Phải commit (hoặc rollback) trước khi bắt đầu INSERT để giải phóng lock.
        try {
            DatabaseConnection.getConnection().commit();
        } catch (SQLException e) {
            logger.error("[WARN] Không thể commit sau kiemTra: " + e.getMessage());
        }
        /// sinh ma auction
        String auctionId = RandNewAuctionId();
        AuctionSession.setSessionId(auctionId);

        // FIX: Bảng san_pham phải có bản ghi trước khi phien_dau_gia tham chiếu đến nó (FOREIGN KEY).
        // Trước đây không có chỗ nào INSERT vào san_pham → SQLITE_CONSTRAINT_FOREIGNKEY.
        // Dùng INSERT OR IGNORE để an toàn: nếu maProduct đã tồn tại thì bỏ qua, không lỗi.
        String sqlProduct = "INSERT OR IGNORE INTO san_pham (ma_san_pham, ten_san_pham) VALUES (?, ?)";
        try (PreparedStatement psSp = DatabaseConnection.getConnection().prepareStatement(sqlProduct)) {
            psSp.setString(1, AuctionSession.getProduct().getProductCode());
            psSp.setString(2, AuctionSession.getProduct().getProductName());
            psSp.executeUpdate();
            psSp.getConnection().commit();
            logger.info("✅ Đã upsert san_pham: " + AuctionSession.getProduct().getProductCode());
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi lưu sản phẩm: " + e.getMessage());
            return; // Không thể tiếp tục nếu san_pham chưa được lưu
        }

        /// insert vao database
        String sql = "INSERT INTO phien_dau_gia " + "(ma_phien, ten_phien, gia_hien_tai, buoc_gia, thoi_gian_bat_dau, thoi_gian_ket_thuc, trang_thai, ma_nguoi_ban, ma_san_pham, ma_nguoi_thang_cuoc, is_closed) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)){
            ps.setString(1, AuctionSession.getAuctionSessionId());
            ps.setString(2, AuctionSession.getSessionName());
            ps.setDouble(3, AuctionSession.getCurrentPrice());
            ps.setDouble(4, AuctionSession.getPriceStep());
            ps.setString(5, AuctionSession.getStartTime() != null ? AuctionSession.getStartTime().toString() : null);
            ps.setString(6, AuctionSession.getEndTime() != null ? AuctionSession.getEndTime().toString() : null);
            ps.setString(7, AuctionSession.getStatus().name());
            ps.setString(8, AuctionSession.getSeller().getUserId());
            ps.setString(9, AuctionSession.getProduct().getProductCode());
            ps.setString(10, AuctionSession.getWinner() != null ? AuctionSession.getWinner().getUserId() : null);
            ps.setBoolean(11, AuctionSession.isClosed());
            // Thực thi câu lệnh INSERT
            // executeUpdate() = dùng cho INSERT, UPDATE, DELETE (không lấy dữ liệu trả về)
            // Trả về số dòng bị ảnh hưởng (1 = thành công)
            int rowsAffected = ps.executeUpdate();
            logger.info(" INSERT thành công, số dòng ảnh hưởng: " + rowsAffected);

            // ĐẢM BẢO DATA ĐƯỢC LƯU VÀO DATABASE
            // SQLite mặc định auto-commit = true, nhưng với WAL mode cần explicit commit
            ps.getConnection().commit();
            logger.info("COMMIT thành công cho auction: " + auctionId);

        }
        catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi lưu phiên đấu giá: " + e.getMessage());
        }
    }
    /**
     * method : layAuctionSession() -> lay ra du lieu cac auction daugia
     * -Map<String, AuctionSession> : key = auctionId, value = AuctionSession
     */
    @Override
    public Map<String, AuctionSession> findAll()throws SQLException {
        Map<String, AuctionSession> result = new HashMap<>();
        String sql = "SELECT p.*, " +
            "nb.ma_nguoi_dung as ma_nguoi_ban, nb.ho_ten as ten_nguoi_ban, nb.thu_dien_tu as email_nguoi_ban, nb.mat_khau as mat_khau_nguoi_ban, nb.ngay_sinh as ngay_sinh_nguoi_ban, nb.dia_chi as dia_chi_nguoi_ban, nb.so_dien_thoai as so_dien_thoai_nguoi_ban, nb.so_du_kha_dung as so_du_kha_dung_nguoi_ban, " +
            "sp.ma_san_pham as ma_san_pham, sp.ten_san_pham as ten_san_pham, " +
            "ntc.ma_nguoi_dung as ma_nguoi_thang_cuoc, ntc.ho_ten as ten_nguoi_thang_cuoc, ntc.thu_dien_tu as email_nguoi_thang_cuoc, ntc.mat_khau as mat_khau_nguoi_thang_cuoc, ntc.ngay_sinh as ngay_sinh_nguoi_thang_cuoc, ntc.dia_chi as dia_chi_nguoi_thang_cuoc, ntc.so_dien_thoai as so_dien_thoai_nguoi_thang_cuoc, ntc.so_du_kha_dung as so_du_kha_dung_nguoi_thang_cuoc " +
            "FROM phien_dau_gia p " +
            "LEFT JOIN nguoi_dung nb ON p.ma_nguoi_ban = nb.ma_nguoi_dung " +
            "LEFT JOIN san_pham sp ON p.ma_san_pham = sp.ma_san_pham " +
            "LEFT JOIN nguoi_dung ntc ON p.ma_nguoi_thang_cuoc = ntc.ma_nguoi_dung";
        /**
         * SELECT p.*   /// lay toan bo cot trong bang phien_dau_gia
         * chu y : SELECT * la lay tat ca cot tu moi bang
         * vd : neu lay nhu logic o cau lenh tren thi se lay ra 2 lan bang nguoi_dung -> khi goi rs.getString("ho_ten") java se khong doan duoc la lay gia tri o bang nao -> bug
         * con SELECT p.* lay tu du tự do bằng * voiws bảng phien_dau_gia còn với các bảng JOIN khác thì chỉ lấy những cột cần thiết và đặt alias để tránh trùng tên cột giữa các bảng
         * vì phiên có thể chưa co người thang cuoc nen dung LEFT JOIN de lay du lieu tu bang nguoi_dung (ntc) neu co, neu khong co thi se tra ve null thay vi bi loai bo auction do
         */
        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Create seller
                User seller = null;
                if (rs.getString("ma_nguoi_ban") != null) {
                    seller = new User(rs.getString("ten_nguoi_ban"), rs.getString("email_nguoi_ban"), rs.getString("mat_khau_nguoi_ban"), rs.getString("ngay_sinh_nguoi_ban"), rs.getString("dia_chi_nguoi_ban"), rs.getString("so_dien_thoai_nguoi_ban"));
                    seller.setUserId(rs.getString("ma_nguoi_ban"));
                    seller.setAvailableBalance(rs.getDouble("so_du_kha_dung_nguoi_ban"));
                }
                // Create Product
                Product Product = null;
                if (rs.getString("ma_san_pham") != null) {
                    Product = new Product(rs.getString("ten_san_pham"), rs.getString("ma_san_pham"));
                }
                // Create winner
                User winner = null;
                if (rs.getString("ma_nguoi_thang_cuoc") != null) {
                    winner = new User(rs.getString("ten_nguoi_thang_cuoc"), rs.getString("email_nguoi_thang_cuoc"), rs.getString("mat_khau_nguoi_thang_cuoc"), rs.getString("ngay_sinh_nguoi_thang_cuoc"), rs.getString("dia_chi_nguoi_thang_cuoc"), rs.getString("so_dien_thoai_nguoi_thang_cuoc"));
                    winner.setUserId(rs.getString("ma_nguoi_thang_cuoc"));
                    winner.setAvailableBalance(rs.getDouble("so_du_kha_dung_nguoi_thang_cuoc"));
                }
                // Create AuctionSession
                AuctionSession AuctionSession = new AuctionSession(
                    rs.getString("ma_phien"),
                    rs.getString("ten_phien"),
                    Product,
                    rs.getDouble("gia_hien_tai"),
                    seller
                );
                AuctionSession.setPriceStep(rs.getDouble("buoc_gia"));
                AuctionSession.setStartTime(rs.getString("thoi_gian_bat_dau") != null ? LocalDateTime.parse(rs.getString("thoi_gian_bat_dau")) : null);
                AuctionSession.setEndTime(rs.getString("thoi_gian_ket_thuc") != null ? LocalDateTime.parse(rs.getString("thoi_gian_ket_thuc")) : null);
                AuctionSession.setStatus(SessionStatus.valueOf(rs.getString("trang_thai")));
                AuctionSession.setClosed(rs.getBoolean("is_closed"));
                AuctionSession.setWinner(winner);
                result.put(AuctionSession.getAuctionSessionId(), AuctionSession);
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi lấy tất cả phiên đấu giá: " + e.getMessage());
        }
        finally {
            try {
                DatabaseConnection.getConnection().commit(); // commit empty transaction
            } catch (SQLException ex) {
                logger.warn("Could not release read lock: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public AuctionSession findById(String auctionId) {
        String sql = "SELECT p.*, " +
            "nb.ma_nguoi_dung as ma_nguoi_ban, nb.ho_ten as ten_nguoi_ban, nb.thu_dien_tu as email_nguoi_ban, nb.mat_khau as mat_khau_nguoi_ban, nb.ngay_sinh as ngay_sinh_nguoi_ban, nb.dia_chi as dia_chi_nguoi_ban, nb.so_dien_thoai as so_dien_thoai_nguoi_ban, nb.so_du_kha_dung as so_du_kha_dung_nguoi_ban, " +
            "sp.ma_san_pham as ma_san_pham, sp.ten_san_pham as ten_san_pham, " +
            "ntc.ma_nguoi_dung as ma_nguoi_thang_cuoc, ntc.ho_ten as ten_nguoi_thang_cuoc, ntc.thu_dien_tu as email_nguoi_thang_cuoc, ntc.mat_khau as mat_khau_nguoi_thang_cuoc, ntc.ngay_sinh as ngay_sinh_nguoi_thang_cuoc, ntc.dia_chi as dia_chi_nguoi_thang_cuoc, ntc.so_dien_thoai as so_dien_thoai_nguoi_thang_cuoc, ntc.so_du_kha_dung as so_du_kha_dung_nguoi_thang_cuoc " +
            "FROM phien_dau_gia p " +
            "LEFT JOIN nguoi_dung nb ON p.ma_nguoi_ban = nb.ma_nguoi_dung " +
            "LEFT JOIN san_pham sp ON p.ma_san_pham = sp.ma_san_pham " +
            "LEFT JOIN nguoi_dung ntc ON p.ma_nguoi_thang_cuoc = ntc.ma_nguoi_dung " +
            "WHERE p.ma_phien = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Create seller
                    User seller = null;
                    if (rs.getString("ma_nguoi_ban") != null) {
                        seller = new User(rs.getString("ten_nguoi_ban"), rs.getString("email_nguoi_ban"), rs.getString("mat_khau_nguoi_ban"), rs.getString("ngay_sinh_nguoi_ban"), rs.getString("dia_chi_nguoi_ban"), rs.getString("so_dien_thoai_nguoi_ban"));
                        seller.setUserId(rs.getString("ma_nguoi_ban"));
                        seller.setAvailableBalance(rs.getDouble("so_du_kha_dung_nguoi_ban"));
                    }
                    // Create Product
                    Product product = null;
                    if (rs.getString("ma_san_pham") != null) {
                        product = new Product(rs.getString("ten_san_pham"), rs.getString("ma_san_pham"));
                    }
                    // Create winner
                    User winner = null;
                    if (rs.getString("ma_nguoi_thang_cuoc") != null) {
                        winner = new User(rs.getString("ten_nguoi_thang_cuoc"), rs.getString("email_nguoi_thang_cuoc"), rs.getString("mat_khau_nguoi_thang_cuoc"), rs.getString("ngay_sinh_nguoi_thang_cuoc"), rs.getString("dia_chi_nguoi_thang_cuoc"), rs.getString("so_dien_thoai_nguoi_thang_cuoc"));
                        winner.setUserId(rs.getString("ma_nguoi_thang_cuoc"));
                        winner.setAvailableBalance(rs.getDouble("so_du_kha_dung_nguoi_thang_cuoc"));
                    }
                    // Create AuctionSession
                    AuctionSession auction = new AuctionSession(
                        rs.getString("ma_phien"),
                        rs.getString("ten_phien"),
                        product,
                        rs.getDouble("gia_hien_tai"),
                        seller
                    );
                    auction.setPriceStep(rs.getDouble("buoc_gia"));
                    auction.setStartTime(rs.getString("thoi_gian_bat_dau") != null ? LocalDateTime.parse(rs.getString("thoi_gian_bat_dau")) : null);
                    auction.setEndTime(rs.getString("thoi_gian_ket_thuc") != null ? LocalDateTime.parse(rs.getString("thoi_gian_ket_thuc")) : null);
                    auction.setStatus(SessionStatus.valueOf(rs.getString("trang_thai")));
                    auction.setClosed(rs.getBoolean("is_closed"));
                    auction.setWinner(winner);
                    return auction;
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi lấy phiên đấu giá: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean update(AuctionSession AuctionSession) {
        String sql = "UPDATE phien_dau_gia SET ten_phien = ?, gia_hien_tai = ?, buoc_gia = ?, thoi_gian_bat_dau = ?, thoi_gian_ket_thuc = ?, trang_thai = ?, ma_nguoi_ban = ?, ma_san_pham = ?, ma_nguoi_thang_cuoc = ?, is_closed = ? WHERE ma_phien = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, AuctionSession.getSessionName());
            ps.setDouble(2, AuctionSession.getCurrentPrice());
            ps.setDouble(3, AuctionSession.getPriceStep()); // FIX: buocGia bị thiếu trước đây
            ps.setString(4, AuctionSession.getStartTime() != null ? AuctionSession.getStartTime().toString() : null);
            ps.setString(5, AuctionSession.getEndTime() != null ? AuctionSession.getEndTime().toString() : null); // FIX: bỏ dòng setString(5,...) trùng lặp bên dưới
            ps.setString(6, AuctionSession.getStatus().name());
            ps.setString(7, AuctionSession.getSeller().getUserId());
            ps.setString(8, AuctionSession.getProduct().getProductCode());
            ps.setString(9, AuctionSession.getWinner() != null ? AuctionSession.getWinner().getUserId() : null);
            ps.setBoolean(10, AuctionSession.isClosed());
            ps.setString(11, AuctionSession.getAuctionSessionId());
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi cập nhật phiên đấu giá: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(String auctionId) {
        String sql = "DELETE FROM phien_dau_gia WHERE ma_phien = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi xóa phiên đấu giá: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean isAuctionAvailable(String auctionId) {
        String sql = "SELECT COUNT(*) FROM phien_dau_gia WHERE ma_phien = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi khi kiểm tra phiên đấu giá tồn tại: " + e.getMessage());
        }
        return false;
    }
}