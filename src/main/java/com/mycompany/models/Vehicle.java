package com.mycompany.models;

class Vehicle extends Items {
    public Vehicle(String ten, String maSanPhan, String moTa) {
        super(ten, maSanPhan, moTa, "Vehicle");
    }
    @Override
    public String getDisplayInfo() {
        return "[Xe cộ] " + this.layTenSanPham() + " - " + moTa;
    }
}