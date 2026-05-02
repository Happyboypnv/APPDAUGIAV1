package com.mycompany.models;

import java.util.List;
import java.util.ArrayList;

public class NguoiDung extends ConNguoi implements CoTheBan, CoTheRoiPhong, CoTheTraGia {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;
    private List<GiaoDich> cacGiaoDich; // ✅ khai báo field
    private String soTaiKhoan;
    private String nganHang;
    public NguoiDung(String hoTen, String thuDienTu,
                     String matKhau, String ngaySinh,
                     String diaChi, String soDienThoai) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        this.cacGiaoDich = new ArrayList<>();
    }
    public NguoiDung(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.cacGiaoDich = new ArrayList<>();
        this.soDuKhaDung = 0;
    }
    /**
     * Lấy số dư khả dụng hiện tại của người dùng.
     * Dùng trong GiaoDichService khi kiểm tra / trừ / cộng tiền.
     */

    /**
     * Cập nhật số dư khả dụng của người dùng.
     * Dùng khi:
     *  - xacNhanThanhToan(): trừ tiền người mua, cộng tiền người bán
     *  - hoanTien()        : cộng lại tiền cho người mua
     *
     * @paramố dư mới sau khi cộng/trừ
     */

    public List<GiaoDich> layCacGiaoDich() {
        if (cacGiaoDich == null) cacGiaoDich = new ArrayList<>();
        return cacGiaoDich;
    }

    public void themGiaoDich(GiaoDich gd) {
        if (cacGiaoDich == null) cacGiaoDich = new ArrayList<>();
        this.cacGiaoDich.add(gd);
    }
    public void setSoDuKhaDung(double soDuKhaDung) {
        this.soDuKhaDung = soDuKhaDung;
    }
    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }
    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }
    public String getSoTaiKhoan() {
        return soTaiKhoan;
    }
    public void setSoTaiKhoan(String soTaiKhoan) {
        this.soTaiKhoan = soTaiKhoan;
    }
    public String getNganHang() {
        return nganHang;
    }
    public void setNganHang(String nganHang) {
        this.nganHang = nganHang;
    }
    public double getSoDuKhaDung() {
        return soDuKhaDung;
    }
    public String getDiaChi() {
        return diaChi;
    }
    public String getSoDienThoai() {
        return soDienThoai;
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