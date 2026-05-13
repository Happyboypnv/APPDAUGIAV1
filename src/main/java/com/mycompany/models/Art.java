package com.mycompany.models;

class Art extends Items {
    public Art(String ten, String maSanPhan, String moTa) {
        super(ten, maSanPhan, moTa, "Art");
    }
    @Override
    public String getDisplayInfo() {
        return "[Nghệ thuật] " + this.layTenSanPham() + " - " + moTa;
    }
}