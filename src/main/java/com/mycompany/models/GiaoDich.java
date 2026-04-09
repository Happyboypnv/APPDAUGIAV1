
package com.mycompany.models;

public class GiaoDich {
    private final String id;
    private PhienDauGia phienDauGia;
    private String thoiGianDauGia;
    private double giaChot;
    private NguoiDung nguoiThangCuoc;
    private NguoiDung nguoiBan;
    public GiaoDich(String id, PhienDauGia phienDauGia) {
        this.id = id;
        this.phienDauGia = phienDauGia;
        this.thoiGianDauGia = phienDauGia.layThoiGianDauGia();
        this.giaChot = phienDauGia.layGiaChot();
        this.nguoiThangCuoc = phienDauGia.layNguoiThangCuoc();
        this.nguoiBan = phienDauGia.layNguoiBan();
    }

    public String toString() {
        return String.format("--- THÔNG TIN PHIÊN ĐẤU GIÁ [%s] ---\n| Sản phẩm:    %-20s |\n| Người bán:   %-20s |\n| Người mua:   %-20s |\n| Thời gian:   %-20s |\n| Giá chốt:    %,17.0f VNĐ |\n------------------------------------", this.id, this.phienDauGia.laySanPham().layTenSanPham(), this.nguoiBan.layHoTen(), this.nguoiThangCuoc != null ? this.nguoiThangCuoc.layHoTen() : "Không có", this.thoiGianDauGia, this.giaChot);
    }
}
