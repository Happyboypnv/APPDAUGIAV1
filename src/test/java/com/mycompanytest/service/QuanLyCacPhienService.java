package com.mycompanytest.service;

import com.mycompanytest.models.PhienDauGia;

import java.util.*;
import java.util.concurrent.*;

public class QuanLyCacPhienService {
    public static final Map<String, com.mycompanytest.models.PhienDauGia> danhSachPhien = new ConcurrentHashMap<>();

    public void them(com.mycompanytest.models.PhienDauGia phien) {
        danhSachPhien.put(phien.getMaPhien(),  phien);
    }

    public void xoa(com.mycompanytest.models.PhienDauGia phien) {
        danhSachPhien.remove(phien.getMaPhien());
    }

    public void xoa(String maPhien) {
        danhSachPhien.remove(maPhien);
    }

    public PhienDauGia tim(String maPhien) {
        return danhSachPhien.get(maPhien);
    }
}
