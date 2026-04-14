package com.mycompany.models;

public interface HanhDongNguoiDung {
    void vaoPhong(PhienDauGia phienDauGia);
    PhienDauGia taoPhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem);

}
