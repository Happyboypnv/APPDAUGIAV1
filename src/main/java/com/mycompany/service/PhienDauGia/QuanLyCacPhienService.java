package com.mycompany.service.PhienDauGia;

import com.mycompany.models.*;

import java.util.*;
import java.util.concurrent.*;

public class QuanLyCacPhienService {
    // Singleton
    // volatile đảm bảo các thread đọc giá trị mới nhất
    private static volatile QuanLyCacPhienService instance;

    public static QuanLyCacPhienService getInstance() {
        if (instance == null) {                          // kiểm tra lần 1 (không lock)
            synchronized (QuanLyCacPhienService.class) {
                if (instance == null) {                  // kiểm tra lần 2 (có lock)
                    instance = new QuanLyCacPhienService();
                }
            }
        }
        return instance;
    }

    private QuanLyCacPhienService() {
    }

    private final Map<String, PhienDauGia> danhSachPhien = new ConcurrentHashMap<>();

    public void them(PhienDauGia phien) {
        danhSachPhien.putIfAbsent(phien.getMaPhien(), phien);
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