package com.mycompany.utils;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.Transaction;
import com.mycompany.models.TransactionStatus;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * KhoLuuTrutransactionSQLite - Implementation của IKhoLuuTrutransaction sử dụng SQLite.
 *
 * MỤC ĐÍCH:
 * - Quản lý lưu trữ tất cả giao dịch (transactions) trong database
 * - Hỗ trợ CRUD operations đầy đủ
 * - Tìm kiếm theo người dùng, trạng thái, etc.
 *
 * THREAD-SAFE:
 * - Sử dụng synchronized cho ID generation
 * - Sử dụng ThreadLocal connections từ KetNoiCSDL
 */
public class TransactionRepositorySQLite implements ITransactionRepository {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TransactionRepositorySQLite.class);
    // Lock để đồng bộ hóa việc sinh mã giao dịch trong môi trường đa luồng
    private static final Object TRANSACTION_ID_GENERATION_LOCK = new Object();

    // Reference đến storage phiên đấu giá để lồng dữ liệu
    private final IAuctionRepository auctionRepository = new AuctionRepositorySQLite();

    /**
     * Sinh mã giao dịch mới theo mẫu: GD000001, GD000002, ...
     */
    private String RandNewTransactionId() {
        synchronized (TRANSACTION_ID_GENERATION_LOCK) {
            String sql = "SELECT MAX(CAST(SUBSTR(ma_giao_dich, 3) AS INTEGER)) " +
                "FROM giao_dich";
            try (Statement stmt = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs   = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int maxVal = rs.getInt(1); // getInt trả 0 nếu MAX = NULL (bảng rỗng)
                    return String.format("GD%06d", maxVal + 1);
                }
            }
            catch (SQLException e) {
                logger.error("[ERROR] Lỗi sinh mã giao dịch: " + e.getMessage());
            }
            return "GD000001";
        }
    }

    /**
     * Lưu một giao dịch mới vào database
     */
    @Override
    public void save(Transaction transaction) {
        String transactionId = (transaction.getId() == null
            || transaction.getId().isBlank()
            || "TEMP".equals(transaction.getId()))
            ? RandNewTransactionId()
            : transaction.getId();

        if (isTransactionAvailable(transactionId)) {
            logger.info("[WARN] Giao dịch đã tồn tại: " + transactionId);
            return;
        }

        String sql = "INSERT INTO giao_dich " +
            "(ma_giao_dich, ma_phien, trang_thai, thoi_gian_tao) " +
            "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, transactionId);
            ps.setString(2, transaction.getAuctionSession().getSessionId());
            ps.setString(3, transaction.getStatus().name());
            ps.setString(4, transaction.getCreatedAt().toString());

            int rowsAffected = ps.executeUpdate();
            ps.getConnection().commit();
            logger.info("[SUCCESS] Lưu giao dịch thành công: " + transactionId);
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lưu giao dịch: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả giao dịch từ database
     */
    @Override
    public Map<String, Transaction> findAll() {
        Map<String, Transaction> result = new HashMap<>();
        String sql = "SELECT gd.*, pd.* FROM giao_dich gd " +
            "LEFT JOIN phien_dau_gia pd ON gd.ma_phien = pd.ma_phien";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String transactionID = rs.getString("ma_giao_dich");
                String auctionID = rs.getString("ma_phien");

                // Lấy phiên đấu giá đầy đủ thông tin
                AuctionSession phien = auctionRepository.findById(auctionID);

                if (phien != null) {
                    Transaction transaction = new Transaction(transactionID, phien,
                        LocalDateTime.parse(rs.getString("thoi_gian_tao")));
                    transaction.setStatus(TransactionStatus.valueOf(rs.getString("trang_thai")));
                    result.put(transactionID, transaction);
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lấy giao dịch: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy giao dịch theo mã
     */
    @Override
    public Transaction findById(String transactionID) {
        String sql = "SELECT * FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, transactionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String auctionID = rs.getString("ma_phien");
                    AuctionSession phien = auctionRepository.findById(auctionID);
                    if (phien != null) {
                        Transaction transaction = new Transaction(transactionID, phien,
                            LocalDateTime.parse(rs.getString("thoi_gian_tao")));
                        transaction.setStatus(TransactionStatus.valueOf(rs.getString("trang_thai")));
                        return transaction;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi tìm giao dịch: " + e.getMessage());
        }
        return null;
    }

    /**
     * Xóa giao dịch theo mã
     */
    @Override
    public boolean delete(String transactionID) {
        String sql = "DELETE FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, transactionID);
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi xóa giao dịch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Kiểm tra giao dịch có tồn tại hay không
     */
    @Override
    public boolean isTransactionAvailable(String transactionID) {
        String sql = "SELECT COUNT(*) FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, transactionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi kiểm tra giao dịch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật giao dịch (chủ yếu cập nhật trạng thái)
     */
    @Override
    public boolean update(Transaction transaction) {
        String sql = "UPDATE giao_dich SET trang_thai = ? WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, transaction.getStatus().name());
            ps.setString(2, transaction.getId());
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi cập nhật giao dịch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách giao dịch của một người dùng (bán hoặc mua)
     */
    @Override
    public List<Transaction> findByUserId(String maNguoiDung) {
        List<Transaction> result = new ArrayList<>();
        // Lấy giao dịch nơi người dùng là người bán hoặc người mua
        String sql = "SELECT DISTINCT gd.* FROM giao_dich gd " +
            "LEFT JOIN phien_dau_gia pd ON gd.ma_phien = pd.ma_phien " +
            "WHERE pd.ma_nguoi_ban = ? OR pd.ma_nguoi_thang_cuoc = ?";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maNguoiDung);
            ps.setString(2, maNguoiDung);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String transactionID = rs.getString("ma_giao_dich");
                    Transaction transaction = findById(transactionID);
                    if (transaction != null) {
                        result.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lấy giao dịch theo người dùng: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Transaction findByAuctionId(AuctionSession session) {
        Transaction transaction = null;
        String sql = "SELECT * FROM giao_dich WHERE ma_phien = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1,session.getSessionId());
            try (ResultSet rs = ps.executeQuery()) {
                String transactionID = rs.getString("ma_giao_dich");
                transaction = findById(transactionID);
            }

        } catch (SQLException e) {
            logger.error("[TRANSACTION REPO] Lỗi lấy giao dịch theo mã phiên" + e.getMessage());
        }
        if (transaction==null) {
            logger.error("[TRANSACTION REPO] Lỗi lấy giao dịch theo mã phiên");
        }
        return transaction;
    }

    /**
     * Lấy danh sách giao dịch theo trạng thái
     */
    @Override
    public List<Transaction> findByStatus(TransactionStatus trangThai) {
        List<Transaction> result = new ArrayList<>();
        String sql = "SELECT * FROM giao_dich WHERE trang_thai = ?";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, trangThai.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String transactionID = rs.getString("ma_giao_dich");
                    Transaction transaction = findById(transactionID);
                    if (transaction != null) {
                        result.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lấy giao dịch theo trạng thái: " + e.getMessage());
        }
        return result;
    }

    @Override
    public int countAllTransactions() {
        String sql = "SELECT COUNT(*) FROM giao_dich";
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
}
