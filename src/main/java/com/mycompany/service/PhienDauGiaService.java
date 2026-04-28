package com.mycompany.service;

import com.mycompany.models.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class PhienDauGiaService {
    private QuanLyCacPhienService dieuKhienPhien = QuanLyCacPhienService.getInstance();// Singleton
    private final Map<String, ScheduledFuture<?>> activeTimes = new ConcurrentHashMap<>();
    public void batDauPhien(PhienDauGia phien) {
        LocalDateTime now = LocalDateTime.now();
        phien.setThoiGianBatDau(now);
        phien.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
    }

    public void dongPhien(PhienDauGia phien) {
        LocalDateTime now = LocalDateTime.now();
        phien.setthoiGianKetThuc(now);
        phien.setTrangThai(TrangThaiPhien.KET_THUC);
        if (phien.getdaCoGia()) {
            phien.setNguoiThangCuoc();
        }
        dieuKhienPhien.xoa(phien); // k nen xoa luon sau khi ket thuc vi con luu lich su phien nua
    }

    public void datGia(PhienDauGia phien, NguoiDung nguoiMua, double gia) {
        if (phien.getTrangThai() != TrangThaiPhien.KET_THUC && !nguoiMua.equals(phien.getNguoiBan()) && gia >= phien.getGiaHienTai() + phien.getBuocGia()) {
            synchronized (phien) {
                if (phien.getTrangThai() != TrangThaiPhien.KET_THUC && !nguoiMua.equals(phien.getNguoiBan()) && gia >= phien.getGiaHienTai() + phien.getBuocGia()) { // bi lap???
                    phien.capNhatThongTin(nguoiMua, gia);
                }
            }
        }
    }
}
