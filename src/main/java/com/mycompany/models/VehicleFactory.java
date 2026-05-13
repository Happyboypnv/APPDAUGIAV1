package com.mycompany.models;

public class VehicleFactory extends ItemFactory{
    public Items createItem(String tenSanPham, String maSanPham, String moTa){
        return new Vehicle(tenSanPham, maSanPham, moTa);
    }
}
