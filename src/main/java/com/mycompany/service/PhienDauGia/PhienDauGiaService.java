package com.mycompany.service.PhienDauGia;

import com.mycompany.models.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhienDauGiaService {
    private static volatile PhienDauGiaService instance;
    private static final Map<String, Object> locks = new ConcurrentHashMap<>();
    private static final QuanLyCacPhienService dieuKhienPhien = QuanLyCacPhienService.getInstance();
    private static final PhienDauGiaScheduler lichDongPhien = PhienDauGiaScheduler.getInstance();

    private PhienDauGiaService() {};

    public static PhienDauGiaService getInstance() {
        if (instance == null) {                          // kiểm tra lần 1 (không lock)
            synchronized (QuanLyCacPhienService.class) {
                if (instance == null) {                  // kiểm tra lần 2 (có lock)
                    instance = new PhienDauGiaService();
                }
            }
        }
        return instance;
    }

    private Object getLock(String maPhien) {
        return locks.computeIfAbsent(maPhien, k -> new Object());
    }

    public void batDauPhien(PhienDauGia phien) {
        Object lock = getLock(phien.getMaPhien());

        synchronized (lock) {
            if (phien.getTrangThai() != TrangThaiPhien.DANG_CHO) return;

            LocalDateTime now = LocalDateTime.now();

            phien.setThoiGianBatDau(now);
            phien.setThoiGianKetThuc(now.plusSeconds(phien.getThoiGian()));
            phien.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            lichDongPhien.lenLichDongPhien(phien);
        }
    }

    public void dongPhien(PhienDauGia phien, TrangThaiPhien lyDo) {
        Object lock = getLock(phien.getMaPhien());

        synchronized (lock) {
            if (phien.getTrangThai() == TrangThaiPhien.DA_THANH_TOAN) return;
            if (phien.getTrangThai() == TrangThaiPhien.DA_HUY) return;

            LocalDateTime now = LocalDateTime.now();

            if (!phien.getdaCoGia()) {
                lyDo = TrangThaiPhien.DA_HUY;
            }

            if (lyDo == TrangThaiPhien.DA_THANH_TOAN) {
                phien.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
                phien.setNguoiThangCuoc();
                //-> them viec tru tien nguoi choi o dau dayo
            } else if (lyDo == TrangThaiPhien.DA_HUY) {
                phien.setTrangThai(TrangThaiPhien.DA_HUY);
                lichDongPhien.huyPhien(phien);
            }

            dieuKhienPhien.xoa(phien);
            locks.remove(phien.getMaPhien()); // cleanup lock
        }
    }

    public void datGia(PhienDauGia phien, NguoiDung nguoiMua, double gia) {
        Object lock = getLock(phien.getMaPhien());

        synchronized (lock) {
            if (phien.getTrangThai() == TrangThaiPhien.DA_THANH_TOAN) {
                return;
            }

            if (phien.getTrangThai() == TrangThaiPhien.DA_HUY) {
                return;
            }

            if (nguoiMua.equals(phien.getNguoiBan())) {
                return;
            }

            //Check tiền người mua có đủ so với giá không ở đây

            double giaToiThieu = phien.getGiaHienTai() + phien.getBuocGia();

            if (gia < giaToiThieu) {
                return;
            }

            if (!phien.getdaCoGia()) {
                phien.setBuocGia(phien.getGiaHienTai() * phien.getDoLechGiaMin());
                phien.setDaCoGia(true);
            }

            // update state
            LocalDateTime now = LocalDateTime.now();

            long thoiGianConLai = java.time.Duration.between(now, phien.getThoiGianKetThuc()).getSeconds();
            int nguongChongSanPhutChot = 60;
            int thoiGianThem = 30;

            if(thoiGianConLai <= nguongChongSanPhutChot) {
                LocalDateTime thoiGianKetThucMoi = phien.getThoiGianKetThuc().plusSeconds(thoiGianThem);
                phien.setThoiGianKetThuc(thoiGianKetThucMoi);

                lichDongPhien.lenLichDongPhien(phien);
            }
            phien.setGiaHienTai(gia);
            phien.getDanhSachNguoiTraGia().add(nguoiMua);
        }
    }
}
