package com.mycompany.models;

import java.io.Serializable;

/**
 * ConNguoi - Class trừu tượng đại diện cho một người dùng trong hệ thống
 *
 * MỤC ĐÍCH:
 * - Lưu trữ thông tin cơ bản của người dùng (tên, email, ngày sinh, mật khẩu)
 * - Base class cho NguoiDung và Admin
 * - Cung cấp các phương thức getter/setter chung
 * - Mã hóa mật khẩu với BCrypt khi tạo mới
 */
public abstract class ConNguoi implements Serializable {
    private String maNguoiDung;
    private String hoTen;
    private String thuDienTu;
    private String ngaySinh;
    private String matKhau;
    private String salt;

    @Override
    public String toString() {
        return "maNguoiDung: " + maNguoiDung +
                "\nhoTen: " + hoTen +
                "\nthuDienTu: " + thuDienTu +
                "\nngaySinh: " + ngaySinh;
    }

    /**
     * Constructor đầy đủ - Dùng khi tạo người dùng mới
     * Tự động mã hóa mật khẩu với salt
     * @param hoTen Họ tên người dùng
     * @param thuDienTu Email/Thu điện tử
     * @param matKhau Mật khẩu (plain text, sẽ được mã hóa)
     * @param ngaySinh Ngày sinh
     */
    protected ConNguoi(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        this.hoTen = hoTen;
        this.thuDienTu = thuDienTu;
        this.salt = com.mycompany.utils.BoMaHoaMatKhau.taoSalt();
        this.matKhau = com.mycompany.utils.BoMaHoaMatKhau.maHoaMatKhau(matKhau, this.salt);
        this.ngaySinh = ngaySinh;
    }

    /**
     * Constructor đơn giản - Dùng khi chỉ có tên
     * @param hoTen Họ tên người dùng
     */
    protected ConNguoi(String hoTen) {
        this.hoTen = hoTen;
    }

    // ===== GETTERS =====

    public String layHoTen() {
        return hoTen;
    }

    public String layThuDienTu() {
        return this.thuDienTu;
    }

    public String layNgaySinh() {
        return this.ngaySinh;
    }

    public String layMaNguoiDung() {
        return this.maNguoiDung;
    }

    public String layMatKhau() {
        return matKhau;
    }

    public String laySalt() {
        return salt;
    }

    // ===== SETTERS =====

    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public void setThuDienTu(String thuDienTu) {
        this.thuDienTu = thuDienTu;
    }

    public void setNgaySinh(String ngaySinh) {
        this.ngaySinh = ngaySinh;
    }
}
