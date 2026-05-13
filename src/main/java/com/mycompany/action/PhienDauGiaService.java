package com.mycompany.action;

import com.mycompany.models.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhienDauGiaService {
    private static volatile PhienDauGiaService instance;

    // Lock theo từng mã phiên để đảm bảo thread-safe
    private static final Map<String, Object> locks = new ConcurrentHashMap<>();

    private static final QuanLyCacPhienService dieuKhienPhien = QuanLyCacPhienService.getInstance();
    private static final PhienDauGiaScheduler lichDongPhien = PhienDauGiaScheduler.getInstance();

    private PhienDauGiaService() {}

    public static PhienDauGiaService getInstance() {
        if (instance == null) {
            synchronized (PhienDauGiaService.class) {
                if (instance == null) {
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
        synchronized (getLock(phien.getMaPhien())) {
            if (phien.getTrangThai() != TrangThaiPhien.DANG_CHO) return;

            LocalDateTime now = LocalDateTime.now();
            phien.setThoiGianBatDau(now);
            phien.setThoiGianKetThuc(now.plusSeconds(phien.getThoiGian()));
            phien.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);

            lichDongPhien.lenLichDongPhien(phien);
        }
    }

    public void dongPhien(PhienDauGia phien, TrangThaiPhien lyDo) {
        synchronized (getLock(phien.getMaPhien())) {
            if (phien.getTrangThai() == TrangThaiPhien.DA_THANH_TOAN ||
                    phien.getTrangThai() == TrangThaiPhien.DA_HUY) {
                return;
            }

            if (!phien.getdaCoGia()) {
                lyDo = TrangThaiPhien.DA_HUY;
            }

            if (lyDo == TrangThaiPhien.DA_THANH_TOAN) {
                phien.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
                phien.setNguoiThangCuoc();
                // TODO: Thực hiện trừ tiền người thắng và cộng tiền người bán ở đây
            } else {
                phien.setTrangThai(TrangThaiPhien.DA_HUY);
                lichDongPhien.huyPhien(phien);
            }

            dieuKhienPhien.xoa(phien);
            // Lưu ý: Không xóa lock khỏi Map locks để tránh lỗi đồng bộ
        }
    }

    public boolean datGia(PhienDauGia phien, NguoiDung nguoiMua, double gia) {
        synchronized (getLock(phien.getMaPhien())) {
            // 1. Kiểm tra trạng thái phiên
            if (phien.getTrangThai() != TrangThaiPhien.DANG_DIEN_RA) {
                return false;
            }

            // 2. Chủ phòng không được tự đấu giá
            if (nguoiMua.equals(phien.getNguoiBan())) {
                return false;
            }

            // 3. Tính toán giá tối thiểu cần đặt
            // Nếu là người đầu tiên: giaToiThieu = gia khoi diem
            // Nếu đã có người đặt: giaToiThieu = gia hien tai + buoc gia
            double giaToiThieu = phien.getdaCoGia()
                    ? phien.getGiaHienTai() + phien.getBuocGia()
                    : phien.getGiaHienTai();

            if (gia < giaToiThieu) {
                return false;
            }

            // 4. Kiểm tra số dư người mua (giả định có method getSoDu)
            if (nguoiMua.getSoDuKhaDung() < gia) {
                return false;
            }

            // 5. Logic gia hạn thời gian (Anti-sniping)
            LocalDateTime now = LocalDateTime.now();
            long thoiGianConLai = Duration.between(now, phien.getThoiGianKetThuc()).getSeconds();

            if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
                phien.setThoiGianKetThuc(phien.getThoiGianKetThuc().plusSeconds(30));
                lichDongPhien.lenLichDongPhien(phien); // Cập nhật lại lịch đóng phiên
            }

            // 6. Cập nhật thông tin đấu giá
            if (!phien.getdaCoGia()) {
                phien.setDaCoGia(true);
            }

            // Hoàn lại tiền cho người trả giá cao nhất trước đó (nếu có)
            // TODO: Logic refund cho phien.getNguoiTraGiaCaoNhat()

            phien.setGiaHienTai(gia);

            // CẬP NHẬT BƯỚC GIÁ 6% - Fix lỗi Unit Test
            double buocGiaMoi = gia * phien.getDoLechGiaMin();
            phien.setBuocGia(buocGiaMoi);

            phien.addNguoiTraGia(nguoiMua);

            return true;
        }
    }
}