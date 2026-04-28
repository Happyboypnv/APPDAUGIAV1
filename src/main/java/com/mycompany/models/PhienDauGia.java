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
    public PhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this.maPhien = maPhien;
        this.tenPhien = tenPhien;
        this.sanPhamDauGia = sanPhanDauGia;
        this.giaHienTai = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        buocGia = 0.0;
        this.trangThai = TrangThaiPhien.DANG_CHO;
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
    public void setMaPhien(String maPhien) {
        this.maPhien = maPhien;
    }

    public void setTenPhien(String tenPhien) {
        this.tenPhien = tenPhien;
    }

    public void setGiaHienTai(double giaHienTai) {
        this.giaHienTai = giaHienTai;
    }

    public void setBuocGia(double buocGia) {
        this.buocGia = buocGia;
    }

    public void setNguoiThangCuoc(NguoiDung nguoiThangCuoc) {
        this.nguoiThangCuoc = nguoiThangCuoc;
    }

    public void setSanPham(SanPham sanPham) {
        this.sanPhamDauGia = sanPham;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    // Getters
    public TrangThaiPhien getTrangThai() { return this.trangThai; }
    public double getGiaHienTai() { return this.giaHienTai; }
    public double getBuocGia() { return this.buocGia; }
    public SanPham getSanPham() { return this.sanPhamDauGia; }
//    public LocalDateTime getThoiGianDauGia() { return thoiGianKetThuc-thoiGianBatDau; }
    public NguoiDung getNguoiThangCuoc() { return this.nguoiThangCuoc; }
    public NguoiDung getNguoiBan() { return this.nguoiBan; }
   public String getTenPhienDauGia() { return this.tenPhien; }
    public String getMaPhien() { return this.maPhien; }
//    public double getGiaKhoiDiem() { return this.giaKhoiDiem; }
    public boolean getdaCoGia() {
        return daCoGia;
    }
    public String getTenSanPham() {
        return this.sanPhamDauGia.layTenSanPham();
    }
    public String getMaSanPham() {
        return this.sanPhamDauGia.layMaSanPham();
    }

    public String getTenPhien() {
        return tenPhien;
    }

    public LocalDateTime getThoiGianBatDau() {
        return thoiGianBatDau;
    }

    public LocalDateTime getThoiGianKetThuc() {
        return thoiGianKetThuc;
    }

    public boolean isClosed() {
        return isClosed;
    }
    public String getMaPhienDauGia() {
        return maPhien;
    }
}