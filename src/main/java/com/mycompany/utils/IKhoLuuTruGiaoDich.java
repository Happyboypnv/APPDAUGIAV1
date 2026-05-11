package com.mycompany.utils;

import com.mycompany.models.GiaoDich;
import com.mycompany.models.TrangThaiGiaoDich;

import java.util.Map;
import java.util.List;

/**
 * IKhoLuuTruGiaoDich - Interface định nghĩa hợp đồng lưu trữ giao dịch
 *
 * MỤC ĐÍCH:
 * - Định nghĩa các phương thức cần thiết để lưu/lấy/kiểm tra dữ liệu giao dịch
 * - Sử dụng thiết kế pattern: Strategy Pattern
 * - Cho phép thay thế implementation (SQLite Database)
 *
 * LỢI ÍCH:
 * - Loose coupling: Controller không phụ thuộc vào cách lưu trữ cụ thể
 * - Dễ thay đổi: Nếu muốn đổi database, chỉ cần tạo class mới implement interface này
 * - Dễ test: Có thể tạo mock implementation để test
 */
public interface    IKhoLuuTruGiaoDich {

    /**
     * Lưu một giao dịch mới
     * @param giaoDich giao dịch cần lưu
     */
    void luuGiaoDich(GiaoDich giaoDich);

    /**
     * Lấy tất cả giao dịch
     * @return Map với key = maGiaoDich, value = GiaoDich object
     */
    Map<String, GiaoDich> layTatCaGiaoDich();

    /**
     * Lấy một giao dịch theo mã
     * @param maGiaoDich mã giao dịch cần tìm
     * @return GiaoDich object hoặc null nếu không tìm thấy
     */
    GiaoDich timGiaoDichTheoMa(String maGiaoDich);

    /**
     * Xóa một giao dịch theo mã
     * @param maGiaoDich mã giao dịch cần xóa
     * @return true nếu xóa thành công, false nếu thất bại
     */
    boolean xoaGiaoDich(String maGiaoDich);

    /**
     * Kiểm tra giao dịch có tồn tại hay không
     * @param maGiaoDich mã giao dịch cần kiểm tra
     * @return true nếu tồn tại, false nếu không
     */
    boolean kiemTraGiaoDichTonTai(String maGiaoDich);

    /**
     * Cập nhật giao dịch (thay đổi trạng thái hoặc thông tin)
     * @param giaoDich giao dịch đã được cập nhật
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    boolean capNhatGiaoDich(GiaoDich giaoDich);

    /**
     * Lấy danh sách giao dịch của một người dùng (bán hoặc mua)
     * @param maNguoiDung mã người dùng
     * @return List các giao dịch liên quan đến người dùng
     */
    List<GiaoDich> layGiaoDichTheoNguoiDung(String maNguoiDung);

    /**
     * Lấy danh sách giao dịch theo trạng thái
     * @param trangThai trạng thái giao dịch cần tìm
     * @return List các giao dịch có trạng thái tương ứng
     */
    List<GiaoDich> layGiaoDichTheoTrangThai(TrangThaiGiaoDich trangThai);
}