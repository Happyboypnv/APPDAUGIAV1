package com.mycompany.models;

public class ElectronicsFactory extends ItemFactory{
    public Items createItem(String tenSanPham, String maSanPham, String moTa){
        return new Electronics(tenSanPham, maSanPham, moTa);
    }
}
