package com.mycompany.utils;

import com.mycompany.models.PhienDauGia;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
public interface IKhoLuuTruPhienDauGia {
    void luuPhienDauGia(PhienDauGia phienDauGia);
    PhienDauGia layPhienDauGia(String maPhien);
    Map<String, PhienDauGia> layTatCaPhienDauGia();
    boolean capNhatPhienDauGia(PhienDauGia phienDauGia);
    boolean xoaPhienDauGia(String maPhien);
    boolean kiemTraPhienTonTai(String maPhien);
}