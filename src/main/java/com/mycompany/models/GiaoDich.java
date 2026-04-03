
package com.mycompany.models;

public class GiaoDich {
    private final String id;
    private PhienDauGia phienDauGia;
    private String thoiGianDauGia;
    private double giaChot;
    private User winner;
    private User seller;
    public GiaoDich(String id, PhienDauGia phienDauGia) {
        this.id = id;
        this.phienDauGia = phienDauGia;
        this.thoiGianDauGia = phienDauGia.getThoiGianDauGia();
        this.giaChot = phienDauGia.getGiaChot();
        this.winner = phienDauGia.getWinner();
        this.seller = phienDauGia.getSeller();
    }

    public String toString() {
        return String.format("--- THÔNG TIN PHIÊN ĐẤU GIÁ [%s] ---\n| Sản phẩm:    %-20s |\n| Người bán:   %-20s |\n| Người mua:   %-20s |\n| Thời gian:   %-20s |\n| Giá chốt:    %,17.0f VNĐ |\n------------------------------------", this.id, this.phienDauGia.getSanPham().getName(), this.seller.getName(), this.winner != null ? this.winner.getName() : "Không có", this.thoiGianDauGia, this.giaChot);
    }
}
