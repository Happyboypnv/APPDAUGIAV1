package com.mycompany.utils;

import com.mycompany.models.PhienDauGia;
import com.mycompany.models.TrangThaiPhien;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.SanPham;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
/**
 * Implementation của IKhoLuuTruPhienDauGia sử dụng SQLite.
 * Đảm bảo thread-safe với ThreadLocal connections và synchronized ID generation.
 */
public class KhoLuuTruPhienDauGiaSQLite implements IKhoLuuTruPhienDauGia {

    // Lock để đồng bộ hóa việc sinh mã phiên đấu giá trong môi trường đa luồng
    private static final Object AUCTION_ID_GENERATION_LOCK = new Object();
    /**
     * Method(private): sinhMaPhienDauGia()
     * 1. dem so luong phien da co
     * 2. tang dem len 1
     * 3. Format thanh "PH" + 6 chu so (vd: PH000001)
     * dung synchronized de tranh truong ID trong duong thuc daugia
     */
    private String sinhMaPhienDauGia(){
        synchronized (AUCTION_ID_GENERATION_LOCK) {
            /// 1. dem so luong phien da co
            String sql = "SELECT COUNT(*) FROM phien_dau_gia";
            /// try_with_resources : tu dong dong
            try(Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
                ResultSet rs = stmt.executeQuery(sql)){
                /// COUNT(*) tra ve 1 dung mot dong (khong rong)
                /// vd: 3
                if(rs.next()){
                    int soHienCo = rs.getInt(1);
                    return String.format("PH%06d", soHienCo + 1);
                }
            }
            catch (SQLException e) {
                System.err.println("[ERROR] Lỗi khi sinh mã phiên đấu giá: " + e.getMessage());
            }
            return String.format("PH%06d", 1); // Trường hợp không có phiên nào, bắt đầu từ PH000001
        }
    }
    /**
     * Method : LuuPhienDauGia()
     * 1. sinh ma phien
     * 2. setMaPhienDauGia
     * 3. Thuc thi cau insert
     */
    @Override
    public void luuPhienDauGia(PhienDauGia phienDauGia) {
        /// kiem tra phien ton tai
        if(kiemTraPhienTonTai(phienDauGia.getMaPhienDauGia())){
            System.out.println("[WARN] Phiên đấu giá đã tồn tại: " + phienDauGia.getMaPhienDauGia());
            return;
        }
        /// sinh ma phien
        String maPhien = sinhMaPhienDauGia();
        phienDauGia.setMaPhien(maPhien);
        /// insert vao database
        String sql = "INSERT INTO phien_dau_gia " + "(ma_phien, ten_phien, gia_hien_tai, buoc_gia, thoi_gian_bat_dau, thoi_gian_ket_thuc, trang_thai, ma_nguoi_ban, ma_san_pham, ma_nguoi_thang_cuoc, is_closed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)){
            ps.setString(1, phienDauGia.getMaPhienDauGia());
            ps.setString(2, phienDauGia.getTenPhienDauGia());
            ps.setDouble(3, phienDauGia.getGiaHienTai());
            ps.setDouble(4, phienDauGia.getBuocGia());
            ps.setString(5, phienDauGia.getThoiGianBatDau().toString());
            ps.setString(6, phienDauGia.getThoiGianKetThuc().toString());
            ps.setString(7, phienDauGia.getTrangThai().name());
            ps.setString(8, phienDauGia.getNguoiBan().layMaNguoiDung());
            ps.setString(9, phienDauGia.getSanPham().layMaSanPham());
            ps.setString(10, phienDauGia.getNguoiThangCuoc() != null ? phienDauGia.getNguoiThangCuoc().layMaNguoiDung() : null);
            ps.setBoolean(11, phienDauGia.isClosed());
            // Thực thi câu lệnh INSERT
            // executeUpdate() = dùng cho INSERT, UPDATE, DELETE (không lấy dữ liệu trả về)
            // Trả về số dòng bị ảnh hưởng (1 = thành công)
            int rowsAffected = ps.executeUpdate();
            System.out.println(" INSERT thành công, số dòng ảnh hưởng: " + rowsAffected);

            // ĐẢM BẢO DATA ĐƯỢC LƯU VÀO DATABASE
            // SQLite mặc định auto-commit = true, nhưng với WAL mode cần explicit commit
            ps.getConnection().commit();
            System.out.println("COMMIT thành công cho Phien: " + maPhien);

        }
        catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi lưu phiên đấu giá: " + e.getMessage());
        }
    }
    /**
     * method : layPhienDauGia() -> lay ra du lieu cac phien daugia
     * -Map<String, PhienDauGia> : key = maPhien, value = PhienDauGia
     */
    @Override
    public Map<String, PhienDauGia> layTatCaPhienDauGia() {
        Map<String, PhienDauGia> result = new HashMap<>();
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
         * vì phiên có thể chưa co người thang cuoc nen dung LEFT JOIN de lay du lieu tu bang nguoi_dung (ntc) neu co, neu khong co thi se tra ve null thay vi bi loai bo phien do
         */
        try (Statement stmt = KetNoiCSDL.layKetNoi().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Create NguoiBan
                NguoiDung nguoiBan = null;
                if (rs.getString("ma_nguoi_ban") != null) {
                    nguoiBan = new NguoiDung(rs.getString("ten_nguoi_ban"), rs.getString("email_nguoi_ban"), rs.getString("mat_khau_nguoi_ban"), rs.getString("ngay_sinh_nguoi_ban"), rs.getString("dia_chi_nguoi_ban"), rs.getString("so_dien_thoai_nguoi_ban"));
                    nguoiBan.setMaNguoiDung(rs.getString("ma_nguoi_ban"));
                    nguoiBan.setSoDuKhaDung(rs.getDouble("so_du_kha_dung_nguoi_ban"));
                }
                // Create SanPham
                SanPham sanPham = null;
                if (rs.getString("ma_san_pham") != null) {
                    sanPham = new SanPham(rs.getString("ten_san_pham"), rs.getString("ma_san_pham"));
                }
                // Create NguoiThangCuoc
                NguoiDung nguoiThangCuoc = null;
                if (rs.getString("ma_nguoi_thang_cuoc") != null) {
                    nguoiThangCuoc = new NguoiDung(rs.getString("ten_nguoi_thang_cuoc"), rs.getString("email_nguoi_thang_cuoc"), rs.getString("mat_khau_nguoi_thang_cuoc"), rs.getString("ngay_sinh_nguoi_thang_cuoc"), rs.getString("dia_chi_nguoi_thang_cuoc"), rs.getString("so_dien_thoai_nguoi_thang_cuoc"));
                    nguoiThangCuoc.setMaNguoiDung(rs.getString("ma_nguoi_thang_cuoc"));
                    nguoiThangCuoc.setSoDuKhaDung(rs.getDouble("so_du_kha_dung_nguoi_thang_cuoc"));
                }
                // Create PhienDauGia
                PhienDauGia phienDauGia = new PhienDauGia(
                    rs.getString("ma_phien"),
                    rs.getString("ten_phien"),
                    sanPham,
                    rs.getDouble("gia_hien_tai"),
                    nguoiBan
                );
                phienDauGia.setBuocGia(rs.getDouble("buoc_gia"));
                phienDauGia.setThoiGianBatDau(LocalDateTime.parse(rs.getString("thoi_gian_bat_dau")));
                phienDauGia.setthoiGianKetThuc(LocalDateTime.parse(rs.getString("thoi_gian_ket_thuc")));
                phienDauGia.setTrangThai(TrangThaiPhien.valueOf(rs.getString("trang_thai")));
                phienDauGia.setClosed(rs.getBoolean("is_closed"));
                phienDauGia.setNguoiThangCuoc(nguoiThangCuoc);
                result.put(phienDauGia.getMaPhienDauGia(), phienDauGia);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi lấy tất cả phiên đấu giá: " + e.getMessage());
        }
        return result;
    }

    @Override
    public PhienDauGia layPhienDauGia(String maPhien) {
        String sql = "SELECT p.*, " +
                "nb.ma_nguoi_dung as ma_nguoi_ban, nb.ho_ten as ten_nguoi_ban, nb.thu_dien_tu as email_nguoi_ban, nb.mat_khau as mat_khau_nguoi_ban, nb.ngay_sinh as ngay_sinh_nguoi_ban, nb.dia_chi as dia_chi_nguoi_ban, nb.so_dien_thoai as so_dien_thoai_nguoi_ban, nb.so_du_kha_dung as so_du_kha_dung_nguoi_ban, " +
                "sp.ma_san_pham as ma_san_pham, sp.ten_san_pham as ten_san_pham, " +
                "ntc.ma_nguoi_dung as ma_nguoi_thang_cuoc, ntc.ho_ten as ten_nguoi_thang_cuoc, ntc.thu_dien_tu as email_nguoi_thang_cuoc, ntc.mat_khau as mat_khau_nguoi_thang_cuoc, ntc.ngay_sinh as ngay_sinh_nguoi_thang_cuoc, ntc.dia_chi as dia_chi_nguoi_thang_cuoc, ntc.so_dien_thoai as so_dien_thoai_nguoi_thang_cuoc, ntc.so_du_kha_dung as so_du_kha_dung_nguoi_thang_cuoc " +
                "FROM phien_dau_gia p " +
                "LEFT JOIN nguoi_dung nb ON p.ma_nguoi_ban = nb.ma_nguoi_dung " +
                "LEFT JOIN san_pham sp ON p.ma_san_pham = sp.ma_san_pham " +
                "LEFT JOIN nguoi_dung ntc ON p.ma_nguoi_thang_cuoc = ntc.ma_nguoi_dung " +
                "WHERE p.ma_phien = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maPhien);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Create NguoiBan
                    NguoiDung nguoiBan = null;
                    if (rs.getString("ma_nguoi_ban") != null) {
                        nguoiBan = new NguoiDung(rs.getString("ten_nguoi_ban"), rs.getString("email_nguoi_ban"), rs.getString("mat_khau_nguoi_ban"), rs.getString("ngay_sinh_nguoi_ban"), rs.getString("dia_chi_nguoi_ban"), rs.getString("so_dien_thoai_nguoi_ban"));
                        nguoiBan.setMaNguoiDung(rs.getString("ma_nguoi_ban"));
                        nguoiBan.setSoDuKhaDung(rs.getDouble("so_du_kha_dung_nguoi_ban"));
                    }
                    // Create SanPham
                    SanPham sanPham = null;
                    if (rs.getString("ma_san_pham") != null) {
                        sanPham = new SanPham(rs.getString("ten_san_pham"), rs.getString("ma_san_pham"));
                    }
                    // Create NguoiThangCuoc
                    NguoiDung nguoiThangCuoc = null;
                    if (rs.getString("ma_nguoi_thang_cuoc") != null) {
                        nguoiThangCuoc = new NguoiDung(rs.getString("ten_nguoi_thang_cuoc"), rs.getString("email_nguoi_thang_cuoc"), rs.getString("mat_khau_nguoi_thang_cuoc"), rs.getString("ngay_sinh_nguoi_thang_cuoc"), rs.getString("dia_chi_nguoi_thang_cuoc"), rs.getString("so_dien_thoai_nguoi_thang_cuoc"));
                        nguoiThangCuoc.setMaNguoiDung(rs.getString("ma_nguoi_thang_cuoc"));
                        nguoiThangCuoc.setSoDuKhaDung(rs.getDouble("so_du_kha_dung_nguoi_thang_cuoc"));
                    }
                    // Create PhienDauGia
                    PhienDauGia phien = new PhienDauGia(
                        rs.getString("ma_phien"),
                        rs.getString("ten_phien"),
                        sanPham,
                        rs.getDouble("gia_hien_tai"),
                        nguoiBan
                    );
                    phien.setBuocGia(rs.getDouble("buoc_gia"));
                    phien.setThoiGianBatDau(LocalDateTime.parse(rs.getString("thoi_gian_bat_dau")));
                    phien.setthoiGianKetThuc(LocalDateTime.parse(rs.getString("thoi_gian_ket_thuc")));
                    phien.setTrangThai(TrangThaiPhien.valueOf(rs.getString("trang_thai")));
                    phien.setClosed(rs.getBoolean("is_closed"));
                    phien.setNguoiThangCuoc(nguoiThangCuoc);
                    return phien;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi lấy phiên đấu giá: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean capNhatPhienDauGia(PhienDauGia phienDauGia) {
        String sql = "UPDATE phien_dau_gia SET ten_phien = ?, gia_hien_tai = ?, buoc_gia = ?, thoi_gian_bat_dau = ?, thoi_gian_ket_thuc = ?, trang_thai = ?, ma_nguoi_ban = ?, ma_san_pham = ?, ma_nguoi_thang_cuoc = ?, is_closed = ? WHERE ma_phien = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, phienDauGia.getTenPhienDauGia());
            ps.setDouble(2, phienDauGia.getGiaHienTai());
            ps.setDouble(3, phienDauGia.getBuocGia());
            ps.setString(4, phienDauGia.getThoiGianBatDau().toString());
            ps.setString(5, phienDauGia.getThoiGianKetThuc().toString());
            ps.setString(6, phienDauGia.getTrangThai().name());
            ps.setString(7, phienDauGia.getNguoiBan().layMaNguoiDung());
            ps.setString(8, phienDauGia.getSanPham().layMaSanPham());
            ps.setString(9, phienDauGia.getNguoiThangCuoc() != null ? phienDauGia.getNguoiThangCuoc().layMaNguoiDung() : null);
            ps.setBoolean(10, phienDauGia.isClosed());
            ps.setString(11, phienDauGia.getMaPhienDauGia());
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi cập nhật phiên đấu giá: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean xoaPhienDauGia(String maPhien) {
        String sql = "DELETE FROM phien_dau_gia WHERE ma_phien = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maPhien);
            int rows = ps.executeUpdate();
            ps.getConnection().commit();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi xóa phiên đấu giá: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean kiemTraPhienTonTai(String maPhien) {
        String sql = "SELECT COUNT(*) FROM phien_dau_gia WHERE ma_phien = ?";
        try (PreparedStatement ps = KetNoiCSDL.layKetNoi().prepareStatement(sql)) {
            ps.setString(1, maPhien);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi khi kiểm tra phiên đấu giá tồn tại: " + e.getMessage());
        }
        return false;
    }
}
