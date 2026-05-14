package com.mycompany.models;

class Gemstone extends Items {
    public Gemstone(String ten, String maSanPhan, String moTa) {
        super(ten, maSanPhan, moTa, "Gemstone");
    }
    @Override
    public String getDisplayInfo() {
        return "[Đá quý] " + this.layTenSanPham() + " - " + moTa;
    }
}