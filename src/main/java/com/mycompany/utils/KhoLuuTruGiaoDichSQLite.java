package com.mycompany.utils;

import com.mycompany.models.GiaoDich;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.SanPham;
import java.sql.*;
import java.time.LocalDateTime;
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
public class KhoLuuTruGiaoDichSQLite implements IKhoLuuTruGiaoDich {

    // Lock để đồng bộ hóa việc sinh mã giao dịch trong môi trường đa luồng
    private static final Object TRANSACTION_ID_GENERATION_LOCK = new Object();

    // Reference đến storage phiên đấu giá để lồng dữ liệu
    private final IKhoLuuTruPhienDauGia khoLuuTruPhienDauGia = new KhoLuuTruPhienDauGiaSQLite();

    /**
     * Sinh mã giao dịch mới theo mẫu: GD000001, GD000002, ...
     */
    private String sinhMaGiaoDich() {
        synchronized (TRANSACTION_ID_GENERATION_LOCK) {
            String sql = "SELECT COUNT(*) FROM giao_dich";
            try (Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int soHienCo = rs.getInt(1);
                    return String.format("GD%06d", soHienCo + 1);
                }
            } catch (SQLException e) {
                System.err.println("[ERROR] Lỗi sinh mã giao dịch: " + e.getMessage());
            }
            return String.format("GD%06d", 1);
        }
    }

    /**
     * Lưu một giao dịch mới vào database
     */
    @Override
    public void luuGiaoDich(GiaoDich giaoDich) {
        if (kiemTraGiaoDichTonTai(giaoDich.getId())) {
            System.out.println("[WARN] Giao dịch đã tồn tại: " + giaoDich.getId());
            return;
        }

        String sql = "INSERT INTO giao_dich " +
                "(ma_giao_dich, ma_phien, trang_thai, thoi_gian_tao) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, giaoDich.getId());
            ps.setString(2, giaoDich.getPhienDauGia().getMaPhienDauGia());
            ps.setString(3, giaoDich.getTrangThai().name());
            ps.setString(4, giaoDich.getThoiGianTao().toString());

            int rowsAffected = ps.executeUpdate();
            ps.getConnection().commit();
            System.out.println("[SUCCESS] Lưu giao dịch thành công: " + giaoDich.getId());
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi lưu giao dịch: " + e.getMessage());
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

        try (Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String maGiaoDich = rs.getString("ma_giao_dich");
                String maPhien = rs.getString("ma_phien");

                // Lấy phiên đấu giá đầy đủ thông tin
                PhienDauGia phien = khoLuuTruPhienDauGia.layPhienDauGia(maPhien);

                if (phien != null) {
                    GiaoDich giaoDich = new GiaoDich(maGiaoDich, phien);
                    giaoDich.setTrangThai(GiaoDich.TrangThaiGiaoDich.valueOf(rs.getString("trang_thai")));
                    result.put(maGiaoDich, giaoDich);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi lấy giao dịch: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy giao dịch theo mã
     */
    @Override
    public GiaoDich timGiaoDichTheoMa(String maGiaoDich) {
        String sql = "SELECT * FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String maPhien = rs.getString("ma_phien");
                    PhienDauGia phien = khoLuuTruPhienDauGia.layPhienDauGia(maPhien);
                    if (phien != null) {
                        GiaoDich giaoDich = new GiaoDich(maGiaoDich, phien);
                        giaoDich.setTrangThai(GiaoDich.TrangThaiGiaoDich.valueOf(rs.getString("trang_thai")));
                        return giaoDich;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi tìm giao dịch: " + e.getMessage());
        }
        return null;
    }

    /**
     * Xóa giao dịch theo mã
     */
    @Override
    public boolean xoaGiaoDich(String maGiaoDich) {
        String sql = "DELETE FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi xóa giao dịch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Kiểm tra giao dịch có tồn tại hay không
     */
    @Override
    public boolean kiemTraGiaoDichTonTai(String maGiaoDich) {
        String sql = "SELECT COUNT(*) FROM giao_dich WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maGiaoDich);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi kiểm tra giao dịch: " + e.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật giao dịch (chủ yếu cập nhật trạng thái)
     */
    @Override
    public boolean capNhatGiaoDich(GiaoDich giaoDich) {
        String sql = "UPDATE giao_dich SET trang_thai = ? WHERE ma_giao_dich = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, giaoDich.getTrangThai().name());
            ps.setString(2, giaoDich.getId());
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi cập nhật giao dịch: " + e.getMessage());
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

        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
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
            System.err.println("[ERROR] Lỗi lấy giao dịch theo người dùng: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy danh sách giao dịch theo trạng thái
     */
    @Override
    public List<GiaoDich> layGiaoDichTheoTrangThai(GiaoDich.TrangThaiGiaoDich trangThai) {
        List<GiaoDich> result = new ArrayList<>();
        String sql = "SELECT * FROM giao_dich WHERE trang_thai = ?";

        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
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
            System.err.println("[ERROR] Lỗi lấy giao dịch theo trạng thái: " + e.getMessage());
        }
        return result;
    }
}
