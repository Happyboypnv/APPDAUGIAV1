package com.mycompany.models;

import java.io.Serializable;

public class SanPham implements Serializable {
    private String tenSanPham;
    private String maSanPham;
    private double giaKhoiDiem;
    public SanPham(String tenSanPham, String maSanPham, double giaKhoiDiem) {
        this.tenSanPham = tenSanPham;
        this.maSanPham = maSanPham;
        this.giaKhoiDiem = giaKhoiDiem;
    }
    public String layTenSanPham() {
        return this.tenSanPham;
    }
    public void suaTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }
    public String layMaSanPham() {
        return this.maSanPham;
    }

    public double layGiaKhoiDiem() {
        return giaKhoiDiem;
    }
}
