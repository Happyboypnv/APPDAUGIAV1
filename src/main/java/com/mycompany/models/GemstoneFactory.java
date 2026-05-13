package com.mycompany.models;

public class GemstoneFactory extends ItemFactory{
    public Items createItem(String tenSanPham, String maSanPham, String moTa){
        return new Gemstone(tenSanPham, maSanPham, moTa);
    }
}
