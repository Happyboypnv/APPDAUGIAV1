package com.mycompany.models;

import java.time.LocalDateTime;

/**
 * Đại diện cho một giao dịch được tạo ra SAU KHI phiên đấu giá kết thúc.
 *
 * Một GiaoDich bao gồm:
 *  - id         : mã định danh duy nhất của giao dịch
 *  - phienDauGia: phiên đấu giá đã kết thúc, chứa thông tin người bán, người mua, sản phẩm, giá chốt
 *  - trangThai  : trạng thái hiện tại của giao dịch (CHO_THANH_TOAN, DA_THANH_TOAN, DA_HOAN_TIEN)
 *  - thoiGianTao: thời điểm giao dịch được tạo ra
 */
public class GiaoDich {

    // ===== ENUM TRẠNG THÁI GIAO DỊCH =====

    /**
     * Các trạng thái có thể có của một giao dịch:
     *  CHO_THANH_TOAN : phiên vừa kết thúc, người thắng chưa thanh toán
     *  DA_THANH_TOAN  : người thắng đã chuyển tiền thành công
     *  DA_HOAN_TIEN   : giao dịch bị hủy, tiền đã được hoàn lại cho người thắng
     */
    public enum TrangThaiGiaoDich {
        CHO_THANH_TOAN,
        DA_THANH_TOAN,
        DA_HOAN_TIEN
    }

    // ===== CÁC THUỘC TÍNH =====

    /** Mã định danh duy nhất của giao dịch (ví dụ: "GD000001") */
    private final String id;

    /** Phiên đấu giá liên quan — chứa người bán, người mua, sản phẩm, giá chốt */
    private final PhienDauGia phienDauGia;

    /** Trạng thái hiện tại của giao dịch */
    private TrangThaiGiaoDich trangThai;

    /** Thời điểm giao dịch được tạo ra (khi phiên đấu giá kết thúc) */
    private final LocalDateTime thoiGianTao;

    // ===== CONSTRUCTOR =====

    /**
     * Tạo một giao dịch mới từ một phiên đấu giá vừa kết thúc.
     *
     * @param id          mã giao dịch duy nhất
     * @param phienDauGia phiên đấu giá đã kết thúc
     */
    public GiaoDich(String id, PhienDauGia phienDauGia) {
        this.id = id;
        this.phienDauGia = phienDauGia;
        // Giao dịch mới tạo ra luôn ở trạng thái CHỜ THANH TOÁN
        this.trangThai = TrangThaiGiaoDich.CHO_THANH_TOAN;
        // Ghi nhận thời điểm tạo giao dịch
        this.thoiGianTao = LocalDateTime.now();
    }

    // ===== GETTERS =====

    public String getId()                        { return id; }
    public PhienDauGia getPhienDauGia()          { return phienDauGia; }
    public TrangThaiGiaoDich getTrangThai()      { return trangThai; }
    public LocalDateTime getThoiGianTao()        { return thoiGianTao; }

    // ===== SETTER TRẠNG THÁI =====

    /**
     * Cập nhật trạng thái giao dịch.
     * Dùng khi xác nhận thanh toán hoặc khi hoàn tiền.
     *
     * @param trangThai trạng thái mới cần cập nhật
     */
    public void setTrangThai(TrangThaiGiaoDich trangThai) {
        this.trangThai = trangThai;
    }

    // ===== HIỂN THỊ =====

    /**
     * Trả về chuỗi mô tả chi tiết giao dịch để in ra màn hình.
     */
    @Override
    public String toString() {
        String tenNguoiMua = (phienDauGia.getNguoiThangCuoc() != null)
                ? phienDauGia.getNguoiThangCuoc().layHoTen()
                : "Chưa có";

        return String.format(
                "--- THÔNG TIN GIAO DỊCH [%s] ---\n" +
                        "| Sản phẩm      : %-25s |\n" +
                        "| Người bán     : %-25s |\n" +
                        "| Người mua     : %-25s |\n" +
                        "| Giá chốt      : %,22.0f VNĐ |\n" +
                        "| Trạng thái    : %-25s |\n" +
                        "| Thời gian tạo : %-25s |\n" +
                        "------------------------------------------",
                id,
                phienDauGia.getSanPham().layTenSanPham(),
                phienDauGia.getNguoiBan().layHoTen(),
                tenNguoiMua,
                phienDauGia.getGiaHienTai(),
                trangThai.name(),
                thoiGianTao.toString()
        );
    }
}