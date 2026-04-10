package com.mycompany.models;

import java.io.Serializable;

public class SanPham implements Serializable {
    private String tenSanPham;
    private String maSanPham;
    public SanPham(String tenSanPham, String maSanPham) {
        this.tenSanPham = tenSanPham;
        this.maSanPham = maSanPham;
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
}
