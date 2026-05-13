package com.mycompany.models;

class Electronics extends Items {
    public Electronics(String ten, String maSanPhan, String moTa) {
        super(ten, maSanPhan, moTa, "Electronics");
    }
    @Override
    public String getDisplayInfo() {
        return "[Điện tử] " + this.layTenSanPham() + " - " + moTa;
    }
}