// QuanLyCacPhienService.java — implement interface
package com.mycompany.service;

import com.mycompany.models.PhienDauGia;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuanLyCacPhienService implements IQuanLyCacPhienService {
    private static final Map<String, PhienDauGia> danhSachPhien = new ConcurrentHashMap<>();

    @Override public void them(PhienDauGia phien)   { danhSachPhien.put(phien.getMaPhien(), phien); }
    @Override public void xoa(PhienDauGia phien)    { danhSachPhien.remove(phien.getMaPhien()); }
    @Override public void xoa(String maPhien)        { danhSachPhien.remove(maPhien); }
    @Override public PhienDauGia tim(String maPhien) { return danhSachPhien.get(maPhien); }
}