package com.mycompany.models;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class PhienDauGia {
    private String maPhienDauGia;
    private String tenPhienDauGia;
    private double giaKhoiDiem;
    private double giaDanDau;
    private double buocGia;
    private final double doLechGiaMin = 0.06;

    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private String thoiGianDauGia;

    private List<NguoiDung> danhSachNguoiTraGia = new ArrayList<>();
    private NguoiDung nguoiBan;
    private NguoiDung nguoiThangCuoc;
    private SanPham sanPhanDauGia;

    private boolean ok = false;

    // THAY ĐỔI 1: Sử dụng Enum thay vì String
    private TrangThaiPhien trangThai = TrangThaiPhien.DANG_CHO;

    // THAY ĐỔI 2: Thêm ổ khóa đa luồng
    private final transient ReentrantLock lock = new ReentrantLock();

    public PhienDauGia(String maPhienDauGia, String tenPhienDauGia, SanPham sanPhanDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this.maPhienDauGia = maPhienDauGia;
        this.tenPhienDauGia = tenPhienDauGia;
        this.giaKhoiDiem = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        this.buocGia = giaKhoiDiem * this.doLechGiaMin;
        this.giaDanDau = giaKhoiDiem;
        this.sanPhanDauGia = sanPhanDauGia;
    }

    public void batDauPhien() {
        this.trangThai = TrangThaiPhien.DANG_DIEN_RA;
        this.thoiGianBatDau = LocalDateTime.now();
    }

    // THAY ĐỔI 3: Áp dụng Lock để chống Race Condition khi nhiều user đặt giá
    public boolean nguoiChoiTraGia(NguoiDung bidder, double giaMoi) {
        // Chặn ngay từ cửa nếu là người bán hoặc phiên chưa diễn ra
        if (bidder.equals(this.nguoiBan) || this.trangThai != TrangThaiPhien.DANG_DIEN_RA) {
            return false;
        }
        lock.lock(); // BẮT ĐẦU KHÓA
        try {
            if (!this.ok) {
                if (giaMoi >= this.giaKhoiDiem) {
                    capNhatThongTinGia(bidder, giaMoi);
                    return true;
                }
            } else {
                if (giaMoi >= this.giaDanDau + this.buocGia) {
                    capNhatThongTinGia(bidder, giaMoi);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock(); // LUÔN LUÔN MỞ KHÓA
        }
    }

    // Tách riêng logic cập nhật để tái sử dụng
    private void capNhatThongTinGia(NguoiDung bidder, double giaMoi) {
        this.giaDanDau = giaMoi;
        this.danhSachNguoiTraGia.add(bidder);
        this.ok = true;
        // Việc đếm lại 6 giây sẽ do bộ đếm bên ngoài (Controller/Server) đảm nhận
    }

    public void ketThucPhien() {
        this.trangThai = TrangThaiPhien.KET_THUC;
        this.thoiGianKetThuc = LocalDateTime.now();

        long tongSoGiay = ChronoUnit.SECONDS.between(this.thoiGianBatDau, this.thoiGianKetThuc);
        long gio = tongSoGiay / 3600L;
        long phut = (tongSoGiay % 3600L) / 60L;
        long giay = tongSoGiay % 60L;
        this.thoiGianDauGia = String.format("%02d:%02d:%02d", gio, phut, giay);

        if (!this.danhSachNguoiTraGia.isEmpty()) {
            this.nguoiThangCuoc = this.danhSachNguoiTraGia.get(this.danhSachNguoiTraGia.size() - 1);
        }
    }

    // Getters
    public TrangThaiPhien layTrangThai() { return this.trangThai; }
    public double layGiaChot() { return this.giaDanDau; }
    public SanPham laySanPham() { return this.sanPhanDauGia; }
    public String layThoiGianDauGia() { return this.thoiGianDauGia; }
    public NguoiDung layNguoiThangCuoc() { return this.nguoiThangCuoc; }
    public NguoiDung layNguoiBan() { return this.nguoiBan; }
    public String layTenPhienDauGia() { return this.tenPhienDauGia; }
    public String layMaPhienDauGia() { return this.maPhienDauGia; }
    public double layGiaKhoiDiem() { return this.giaKhoiDiem; }
}