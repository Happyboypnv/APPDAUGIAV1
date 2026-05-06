package com.mycompany.models;

import java.io.Serializable;

/**
 * ConNguoi - Class trừu tượng đại diện cho một người dùng trong hệ thống
 *
 * THAY ĐỔI QUAN TRỌNG (SQLite Migration):
 * - Thêm field 'salt' để hỗ trợ password hashing
 * - Constructor tự động hash password với salt
 * - Thêm getter/setter cho salt
 *
 * MỤC ĐÍCH:
 * - Lưu trữ thông tin cơ bản của người dùng (tên, email, ngày sinh, mật khẩu)
 * - Base class cho NguoiDung và Admin
 * - Cung cấp các phương thức getter/setter chung
 * - Mã hóa mật khẩu với salt khi tạo mới
 */
public class ConNguoi implements Serializable {
    private String maNguoiDung;
    private String hoTen;
    private String thuDienTu;
    private String ngaySinh;
    private String matKhau;

    // THAY ĐỔI QUAN TRỌNG: Thêm field salt cho password hashing
    // Lý do: Cần thiết cho bảo mật password
    // Cách hoạt động: Mỗi user có salt riêng → rainbow table attack không hiệu quả
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
     *
     * THAY ĐỔI QUAN TRỌNG (FIX Double Hashing):
     * - Trước: Constructor tự động hash password → gây double-hashing
     * - Sau: Constructor lưu password như-là (đã được hash bởi caller)
     *
     * Quy trình mới:
     * 1. Nhận password (có thể đã được hash bởi caller)
     * 2. Lưu trực tiếp mà không hash lại
     * 3. Salt được set riêng bởi caller (thông qua setSalt())
     *
     * Lý do thay đổi:
     * - Tách rời trách nhiệm: ConNguoi là model class, không nên quyết định bảo mật
     * - LoginAction.dangKy() tự tin hash password rồi → không được hash lại
     * - Tránh double-hashing: hash(hash(originalPassword, salt1), salt2)
     * - Linh hoạt hơn: Có thể tái sử dụng cho migration, backup, v.v.
     *
     * @param hoTen Họ tên người dùng
     * @param thuDienTu Email/Thu điện tử
     * @param matKhau Mật khẩu (đã được hash bởi caller, sẽ lưu như-là)
     * @param ngaySinh Ngày sinh
     */
    protected ConNguoi(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        this.hoTen = hoTen;
        this.thuDienTu = thuDienTu;
        this.matKhau = matKhau;  // FIX: Lưu trực tiếp, không hash lại
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

    // THAY ĐỔI: Thêm getter cho salt
    // Cần thiết cho password verification trong login
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
    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    // THAY ĐỔI: Thêm setter cho salt
    // Cần thiết cho migration users cũ từ JSON
    public void setSalt(String salt) {
        this.salt = salt;
    }
}
