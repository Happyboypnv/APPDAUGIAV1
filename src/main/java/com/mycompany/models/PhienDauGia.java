package com.mycompany.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PhienDauGia {
    private String maPhien;
    private String tenPhien;
    private double giaHienTai;
    private double buocGia;
    private final double doLechGiaMin = 0.06;

    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;

    private List<NguoiDung> danhSachNguoiTraGia = new ArrayList<>();
    private NguoiDung nguoiBan;
    private NguoiDung nguoiThangCuoc;
    private SanPham sanPhamDauGia;

    private boolean daCoGia = false;

    // THAY ĐỔI 1: Sử dụng Enum thay vì String
    private TrangThaiPhien trangThai;

    private boolean isClosed;
    private final transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public PhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this.maPhien = maPhien;
        this.tenPhien = tenPhien;
        this.sanPhamDauGia = sanPhanDauGia;
        this.giaHienTai = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        buocGia = 0.0;
        this.trangThai = TrangThaiPhien.DANG_CHO_DUYET;
    }

    public void capNhatThongTin(NguoiDung nguoiMua, double giaMoi) {
        if (!daCoGia) {
            this.buocGia = giaHienTai * this.doLechGiaMin;
            daCoGia = true;
        }
        this.giaHienTai = giaMoi;
        this.danhSachNguoiTraGia.add(nguoiMua);
        // Việc đếm lại 6 giây sẽ do bộ đếm bên ngoài (Controller/Server) đảm nhận
    }

    //Setters
    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) {
        this.thoiGianBatDau = thoiGianBatDau;
    }
    public void setthoiGianKetThuc(LocalDateTime thoiKetThuc) {
        this.thoiGianKetThuc = thoiKetThuc;
    }
    public void setTrangThai(TrangThaiPhien trangThai) {
        this.trangThai = trangThai;
    }
    public void setNguoiThangCuoc() {
        if (!danhSachNguoiTraGia.isEmpty()) nguoiThangCuoc = danhSachNguoiTraGia.get(danhSachNguoiTraGia.size() - 1);
    }
    // Getters
    public TrangThaiPhien getTrangThai() { return this.trangThai; }
    public double getGiaHienTai() { return this.giaHienTai; }
    public double getBuocGia() { return this.buocGia; }
    public SanPham getSanPham() { return this.sanPhamDauGia; }
//    public LocalDateTime getThoiGianDauGia() { return thoiGianKetThuc-thoiGianBatDau; }
    public NguoiDung getNguoiThangCuoc() { return this.nguoiThangCuoc; }
    public NguoiDung getNguoiBan() { return this.nguoiBan; }
//    public String getTenPhienDauGia() { return this.tenPhienDauGia; }
    public String getMaPhien() { return this.maPhien; }
//    public double getGiaKhoiDiem() { return this.giaKhoiDiem; }
    public boolean getdaCoGia() {
        return daCoGia;
    }
    public ReentrantReadWriteLock getLock() { return lock; }
}