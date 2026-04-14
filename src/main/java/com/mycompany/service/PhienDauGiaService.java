package com.mycompany.service;

import com.mycompany.models.*;
import java.time.LocalDateTime;

public class PhienDauGiaService {
    private QuanLyCacPhienService dieuKhienPhien = new QuanLyCacPhienService();

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

    public void datGia(PhienDauGia phien, NguoiMua nguoiMua, double gia) {
        if (phien.getTrangThai() != TrangThaiPhien.KET_THUC && !nguoiMua.equals(phien.getNguoiBan()) && gia >= phien.getGiaHienTai() + phien.getBuocGia()) {
            synchronized (phien) {
                if (phien.getTrangThai() != TrangThaiPhien.KET_THUC && !nguoiMua.equals(phien.getNguoiBan()) && gia >= phien.getGiaHienTai() + phien.getBuocGia()) { // bi lap???
                    phien.capNhatThongTin(nguoiMua, gia);
                }
            }
        }
    }
}
