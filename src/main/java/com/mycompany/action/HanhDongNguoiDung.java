package com.mycompany.action;

import com.mycompany.models.PhienDauGia;
import com.mycompany.models.SanPham;

public interface HanhDongNguoiDung {
    void vaoPhong(PhienDauGia phienDauGia);
    PhienDauGia taoPhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem);

}
