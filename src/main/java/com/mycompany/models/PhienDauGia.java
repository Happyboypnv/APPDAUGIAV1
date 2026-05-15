package com.mycompany.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class PhienDauGia {
    private String maPhien;
    private String tenPhien;
    private double giaHienTai;
    private double buocGia;
    private final double DOLECHGIAMIN = 0.06;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private int thoiGian;
    private final List<NguoiDung> danhSachNguoiTraGia = Collections.synchronizedList(new ArrayList<>());
    private NguoiDung nguoiBan;
    private NguoiDung nguoiThangCuoc;
    private SanPham sanPhamDauGia;
    private boolean daCoGia = false;
    private volatile TrangThaiPhien trangThai;
    private boolean isClosed = false;

    public PhienDauGia(String maPhien, String tenPhien, SanPham sanPhamDauGia, double giaKhoiDiem, NguoiDung nguoiBan, int thoiGian) {
        this.maPhien = maPhien;
        this.tenPhien = tenPhien;
        this.sanPhamDauGia = sanPhamDauGia;
        this.giaHienTai = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        this.thoiGian = thoiGian;
        buocGia = 0.0;
        this.trangThai = TrangThaiPhien.DANG_CHO;
    }
    public PhienDauGia(String tenPhien, SanPham sanPhamDauGia, double giaKhoiDiem, NguoiDung nguoiBan, LocalDateTime thoiGianBatDau, LocalDateTime thoiGianKetThuc) {
        this.tenPhien = tenPhien;
        this.sanPhamDauGia = sanPhamDauGia;
        this.giaHienTai = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        this.thoiGianBatDau = thoiGianBatDau;
        this.thoiGianKetThuc = thoiGianKetThuc;
    }

    // Overloaded constructor for use with database (without thoiGian)
    public PhienDauGia(String maPhien, String tenPhien, SanPham sanPhamDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this(maPhien, tenPhien, sanPhamDauGia, giaKhoiDiem, nguoiBan, 0);
    }
    public void addNguoiTraGia(NguoiDung nguoiTraGia) {
        synchronized (danhSachNguoiTraGia) {
            danhSachNguoiTraGia.add(nguoiTraGia);
        }
    }

    //Setters
    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) {
        this.thoiGianBatDau = thoiGianBatDau;
    }

    public void setTrangThai(TrangThaiPhien trangThai) {
        this.trangThai = trangThai;
    }

    public void setNguoiThangCuoc() {
        if (!danhSachNguoiTraGia.isEmpty()) nguoiThangCuoc = danhSachNguoiTraGia.get(danhSachNguoiTraGia.size() - 1);
    }

    public void setNguoiThangCuoc(NguoiDung nguoiThangCuoc) {
        this.nguoiThangCuoc = nguoiThangCuoc;
    }

    public void setBuocGia(double buocGia) {
        this.buocGia = buocGia;
    }

    public void setDaCoGia(boolean daCoGia) {
        this.daCoGia = daCoGia;
    }

    public void setGiaHienTai(double giaHienTai) {
        this.giaHienTai = giaHienTai;
    }

    public void setThoiGian(int thoiGian) {
        this.thoiGian = thoiGian;
    }

    public void setThoiGianKetThuc(LocalDateTime thoiGianKetThuc) {
        this.thoiGianKetThuc = thoiGianKetThuc;
    }

    public void setMaPhien(String maPhien) {
        this.maPhien = maPhien;
    }

    // Getters
    public TrangThaiPhien getTrangThai() {
        return this.trangThai;
    }

    public double getGiaHienTai() {
        return this.giaHienTai;
    }

    public double getBuocGia() {
        return this.buocGia;
    }

    public SanPham getSanPham() {
        return this.sanPhamDauGia;
    }

    public NguoiDung getNguoiThangCuoc() {
        return this.nguoiThangCuoc;
    }

    public NguoiDung getNguoiBan() {
        return this.nguoiBan;
    }

    public String getMaPhien() {
        return this.maPhien;
    }

    public String getMaPhienDauGia() {
        return this.maPhien;
    }

    public String getTenPhienDauGia() {
        return this.tenPhien;
    }

    public boolean getdaCoGia() {
        return daCoGia;
    }

    public double getDoLechGiaMin() {
        return  DOLECHGIAMIN;
    }

    public List<NguoiDung> getDanhSachNguoiTraGia() {
        synchronized (danhSachNguoiTraGia) {
            return new ArrayList<>(danhSachNguoiTraGia);
        }
    }

    public int getThoiGian() {
        return thoiGian;
    }

    public LocalDateTime getThoiGianKetThuc() {
        return thoiGianKetThuc;
    }

    public LocalDateTime getThoiGianBatDau() {
        return thoiGianBatDau;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }
}