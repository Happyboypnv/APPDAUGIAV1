package com.mycompany.models;

import java.util.List;
import java.util.ArrayList;

public class NguoiDung extends ConNguoi implements CoTheBan, CoTheRoiPhong, CoTheTraGia {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;
    private List<GiaoDich> cacGiaoDich; // ✅ khai báo field

    public NguoiDung(String maNguoiDung, String hoTen, String thuDienTu,
                     String matKhau, String ngaySinh,
                     String diaChi, String soDienThoai) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        this.cacGiaoDich = new ArrayList<>();
    }

    public List<GiaoDich> layCacGiaoDich() {
        return cacGiaoDich;
    }

    public void themGiaoDich(GiaoDich gd) {
        this.cacGiaoDich.add(gd);
    }
    @Override
    public void mua(SanPham p) {
        // Lấy tên NguoiDung từ thuộc tính hoặc hàm toString để in ra cho trực quan
        System.out.println("[MUA] Người dùng " + this.toString().split("\n")[1] + " đang đặt giá mua: " + p.layTenSanPham());
    }
    @Override
    public void ban(SanPham p) {
        System.out.println("[BÁN] Người dùng " + this.toString().split("\n")[1] + " đang đăng bán: " + p.layTenSanPham());
    }
    @Override
    public void roiKhoiPhong(){
        System.out.println("Người dùng đã rời phòng đấu giá.");
    }
}
