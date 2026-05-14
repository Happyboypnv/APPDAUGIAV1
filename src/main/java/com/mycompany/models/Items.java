package com.mycompany.models;

import com.mycompany.models.SanPham;

abstract class Items extends SanPham {
    protected String moTa;
    protected String danhMuc;

    public Items(String tenSanPham, String maSanPhan, String moTa, String danhMuc) {
        super(tenSanPham, maSanPhan);
        this.moTa = moTa;
        this.danhMuc = danhMuc;
    }

    public abstract String getDisplayInfo();
}