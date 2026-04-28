package com.mycompany.models;

import java.util.List;
import java.util.ArrayList;

public class NguoiDung extends ConNguoi implements CoTheBan, CoTheRoiPhong, CoTheTraGia {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;
    private String soTaiKhoan, nganHang;
    private List<GiaoDich> cacGiaoDich; // ✅ khai báo field

    public NguoiDung(String hoTen, String thuDienTu,
                     String matKhau, String ngaySinh,
                     String diaChi, String soDienThoai, String soTaiKhoan, String nganHang) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        this.soTaiKhoan = soTaiKhoan;
        this.nganHang = nganHang;
        this.cacGiaoDich = new ArrayList<>();
    }
    public NguoiDung(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.cacGiaoDich = new ArrayList<>();
        this.soDuKhaDung = 0;
    }
    public List<GiaoDich> layCacGiaoDich() {
        if (cacGiaoDich == null) cacGiaoDich = new ArrayList<>();
        return cacGiaoDich;
    }

    public void themGiaoDich(GiaoDich gd) {
        if (cacGiaoDich == null) cacGiaoDich = new ArrayList<>();
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

    public double laySoDuKhaDung() {
        return soDuKhaDung;
    }


    public String layDiaChi() {
        return diaChi;
    }

    public String laySoDienThoai() {
        return soDienThoai;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String laySoTaiKhoan() {
        return soTaiKhoan;
    }

    public void setSoTaiKhoan(String soTaiKhoan) {
        this.soTaiKhoan = soTaiKhoan;
    }

    public void setSoDuKhaDung(double soDuKhaDung) {
        this.soDuKhaDung = soDuKhaDung;
    }

    public String layNganHang() {
        return nganHang;
    }

    public void setNganHang(String nganHang) {
        this.nganHang = nganHang;
    }
}
