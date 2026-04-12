package com.mycompany.service;

import com.mycompany.models.*;
import java.util.*;
import java.util.concurrent.*;

public class QuanLyCacPhienService {
    private static final Map<String, PhienDauGia> danhSachPhien = new ConcurrentHashMap<>();

    public void them(PhienDauGia phien) {
        danhSachPhien.put(phien.getMaPhien(),  phien);
    }

    public void xoa(PhienDauGia phien) {
        danhSachPhien.remove(phien.getMaPhien());
    }

    public void xoa(String maPhien) {
        danhSachPhien.remove(maPhien);
    }

    public PhienDauGia tim(String maPhien) {
        return danhSachPhien.get(maPhien);
    }
}
