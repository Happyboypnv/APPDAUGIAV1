// PhienDauGiaService.java — nhận interface qua constructor
package com.mycompany.service;

import com.mycompany.models.*;
import java.time.LocalDateTime;

public class PhienDauGiaService {
    private final IQuanLyCacPhienService dieuKhienPhien; // Phụ thuộc vào abstraction

    public PhienDauGiaService(IQuanLyCacPhienService dieuKhienPhien) {
        this.dieuKhienPhien = dieuKhienPhien;
    }

    public void batDauPhien(PhienDauGia phien) {
        phien.setThoiGianBatDau(LocalDateTime.now());
        phien.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
    }

    public void dongPhien(PhienDauGia phien) {
        phien.setthoiGianKetThuc(LocalDateTime.now());
        phien.setTrangThai(TrangThaiPhien.KET_THUC);
        if (phien.getdaCoGia()) {
            phien.setNguoiThangCuoc();
        }
        dieuKhienPhien.xoa(phien);
    }

    public void datGia(PhienDauGia phien, NguoiDung nguoiMua, double gia) {
        if (phien.getTrangThai() != TrangThaiPhien.KET_THUC
                && !nguoiMua.equals(phien.getNguoiBan())
                && gia >= phien.getGiaHienTai() + phien.getBuocGia()) {
            synchronized (phien) {
                if (phien.getTrangThai() != TrangThaiPhien.KET_THUC
                        && !nguoiMua.equals(phien.getNguoiBan())
                        && gia >= phien.getGiaHienTai() + phien.getBuocGia()) {
                    phien.capNhatThongTin(nguoiMua, gia);
                }
            }
        }
    }
}