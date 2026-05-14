package com.mycompany.models;

public class ArtFactory extends ItemFactory{
    public Items createItem(String tenSanPham, String maSanPham, String moTa){
        return new Art(tenSanPham, maSanPham, moTa);
    }
}
