
package com.mycompany.models;

public class GiaoDich {
    private final String id;
    private PhienDauGia phienDauGia;
    public GiaoDich(String id, PhienDauGia phienDauGia) {
        this.id = id;
        this.phienDauGia = phienDauGia;
    }

    public String toString() {
        return String.format(
                "--- THÔNG TIN PHIÊN ĐẤU GIÁ [%s] ---\n" +
                        "| Sản phẩm:    %-20s |\n" +
                        "| Người bán:   %-20s |\n" +
                        "| Người mua:   %-20s |\n" +
                        "| Thời lượng:  %-20s |\n" +
                        "| Giá chốt:    %,17.0f VNĐ |\n" +
                        "------------------------------------",
                id,
                phienDauGia.getSanPham().layTenSanPham(),
                phienDauGia.getNguoiBan().layHoTen(),
                phienDauGia.getNguoiThangCuoc().layHoTen(),
                "Tạm thời không hỗ trợ",
                phienDauGia.getGiaHienTai()
        );
    }
}
