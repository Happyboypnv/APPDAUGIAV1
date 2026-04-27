package com.mycompany.models;

public class Admin extends ConNguoi {
    Admin (String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen,  thuDienTu, matKhau, ngaySinh);
    }

    @Override
    public PhienDauGia taoPhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem) {
        return null;
    }
}
