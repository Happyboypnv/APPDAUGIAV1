package com.mycompany.utils;

import com.mycompany.models.*;
import org.slf4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * KhoLuuTruGiaoDichSQLite - Implementation của IKhoLuuTruGiaoDich sử dụng SQLite.
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
public class KhoLuuTruGiaoDichSQLite implements ITransactionRepository {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(KhoLuuTruGiaoDichSQLite.class);
    // Lock để đồng bộ hóa việc sinh mã giao dịch trong môi trường đa luồng
    private static final Object TRANSACTION_ID_GENERATION_LOCK = new Object();

    // Reference đến storage phiên đấu giá để lồng dữ liệu
    private final IKhoLuuTruPhienDauGia khoLuuTruPhienDauGia = new KhoLuuTruPhienDauGiaSQLite();

    /**
     * Sinh mã giao dịch mới theo mẫu: GD000001, GD000002, ...
     */
    private String sinhMaGiaoDich() {
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
    public void luuGiaoDich(GiaoDich giaoDich) {
        if (kiemTraGiaoDichTonTai(giaoDich.getId())) {
           logger.info("[WARN] Giao dịch đã tồn tại: " + giaoDich.getId());
            return;
        }

        String sql = "INSERT INTO giao_dich " +
                "(ma_giao_dich, ma_phien, trang_thai, thoi_gian_tao) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, giaoDich.getId());
            ps.setString(2, giaoDich.getPhienDauGia().getMaPhienDauGia());
            ps.setString(3, giaoDich.getTrangThai().name());
            ps.setString(4, giaoDich.getThoiGianTao().toString());

            int rowsAffected = ps.executeUpdate();
            ps.getConnection().commit();
           logger.info("[SUCCESS] Lưu giao dịch thành công: " + giaoDich.getId());
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lưu giao dịch: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả giao dịch từ database
     */
    @Override
    public Map<String, GiaoDich> layTatCaGiaoDich() {
        Map<String, GiaoDich> result = new HashMap<>();
        String sql = "SELECT gd.*, pd.* FROM giao_dich gd " +
                "LEFT JOIN phien_dau_gia pd ON gd.ma_phien = pd.ma_phien";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String maGiaoDich = rs.getString("ma_giao_dich");
                String maPhien = rs.getString("ma_phien");

                // Lấy phiên đấu giá đầy đủ thông tin
                PhienDauGia phien = khoLuuTruPhienDauGia.layPhienDauGia(maPhien);

                if (phien != null) {
                    GiaoDich giaoDich = new GiaoDich(maGiaoDich, phien);
                    giaoDich.setTrangThai(TrangThaiGiaoDich.valueOf(rs.getString("trang_thai")));
                    result.put(maGiaoDich, giaoDich);
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
    public GiaoDich timGiaoDichTheoMa(String maGiaoDich) {
        String sql = "SELECT * FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String maPhien = rs.getString("ma_phien");
                    PhienDauGia phien = khoLuuTruPhienDauGia.layPhienDauGia(maPhien);
                    if (phien != null) {
                        GiaoDich giaoDich = new GiaoDich(maGiaoDich, phien);
                        giaoDich.setTrangThai(TrangThaiGiaoDich.valueOf(rs.getString("trang_thai")));
                        return giaoDich;
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
    public boolean xoaGiaoDich(String maGiaoDich) {
        String sql = "DELETE FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
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
    public boolean kiemTraGiaoDichTonTai(String maGiaoDich) {
        String sql = "SELECT COUNT(*) FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
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
    public boolean capNhatGiaoDich(GiaoDich giaoDich) {
        String sql = "UPDATE giao_dich SET trang_thai = ? WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, giaoDich.getTrangThai().name());
            ps.setString(2, giaoDich.getId());
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
    public List<GiaoDich> layGiaoDichTheoNguoiDung(String maNguoiDung) {
        List<GiaoDich> result = new ArrayList<>();
        // Lấy giao dịch nơi người dùng là người bán hoặc người mua
        String sql = "SELECT DISTINCT gd.* FROM giao_dich gd " +
                "LEFT JOIN phien_dau_gia pd ON gd.ma_phien = pd.ma_phien " +
                "WHERE pd.ma_nguoi_ban = ? OR pd.ma_nguoi_thang_cuoc = ?";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, maNguoiDung);
            ps.setString(2, maNguoiDung);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String maGiaoDich = rs.getString("ma_giao_dich");
                    GiaoDich giaoDich = timGiaoDichTheoMa(maGiaoDich);
                    if (giaoDich != null) {
                        result.add(giaoDich);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lấy giao dịch theo người dùng: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy danh sách giao dịch theo trạng thái
     */
    @Override
    public List<GiaoDich> layGiaoDichTheoTrangThai(TrangThaiGiaoDich trangThai) {
        List<GiaoDich> result = new ArrayList<>();
        String sql = "SELECT * FROM giao_dich WHERE trang_thai = ?";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, trangThai.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String maGiaoDich = rs.getString("ma_giao_dich");
                    GiaoDich giaoDich = timGiaoDichTheoMa(maGiaoDich);
                    if (giaoDich != null) {
                        result.add(giaoDich);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("[ERROR] Lỗi lấy giao dịch theo trạng thái: " + e.getMessage());
        }
        return result;
    }
}
