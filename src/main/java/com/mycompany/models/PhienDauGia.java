package com.mycompany.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class PhienDauGia {
    private String maPhien;
    private String tenPhien;
    private double giaHienTai;
    private double buocGia;
    private final double doLechGiaMin = 0.06;

    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;

    private List<NguoiMua> danhSachNguoiTraGia = new ArrayList<>();
    private NguoiBan nguoiBan;
    private NguoiMua nguoiThangCuoc; // Sua cac cho nguoi dung thanh dung role cua no
    private SanPham sanPhamDauGia;

    private boolean daCoGia = false;

    // THAY ĐỔI 1: Sử dụng Enum thay vì String
    private TrangThaiPhien trangThai;

    public PhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this.maPhien = maPhien;
        this.tenPhien = tenPhien;
        this.sanPhamDauGia = sanPhanDauGia;
        this.giaHienTai = giaKhoiDiem;
        NguoiBan nguoiban = new NguoiBan(nguoiBan);
        this.nguoiBan = nguoiban; // set nguoi tao phien la nguoi ban
        buocGia = 0.0;
        this.trangThai = TrangThaiPhien.DANG_CHO_DUYET;
        nguoiban.setPhienDauGia(this); // tuong tu nhu voi nguoi mua, cho nguoi ban co 1 phien dau gia de goi phuong thuc k phai truyen lai tham so phien dau gia nua
    }

    public void themVaoPhong(NguoiDung nguoiMua) {
        if (this.trangThai==TrangThaiPhien.DANG_MO || this.trangThai==TrangThaiPhien.DANG_DIEN_RA) {
            NguoiMua nguoimua = new NguoiMua(nguoiMua);
            danhSachNguoiTraGia.add(0, nguoimua);
            nguoimua.setPhienDauGia(this); // y nghia la nguoi mua dang tham gia cai phien nay, ti nua goi method roi phong hay dat bid kh can truyen vao tham so PhienDauGia nua
            return;
        } // them vao dau danh sach cho do bi lan voi set nguoi tra gia cao nhat
        System.out.println("Phòng chưa mở hoặc đã kết thúc!"); // thay bang throw loi sau
    }

    public void xoaKhoiPhong(NguoiMua nguoiMua) {
        if (this.trangThai==TrangThaiPhien.DANG_DIEN_RA) {
            nguoiMua.setPhienDauGia(null); // nguoi mua k con tham gia phien dau gia nay nua
            return;
        }
        System.out.println("Phiên đã kết thúc!");
    }

    public void capNhatThongTin(NguoiMua nguoiMua, double giaMoi) {
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
    public NguoiDung getNguoiThangCuoc() { return this.nguoiThangCuoc.getNguoiDung(); }
    public NguoiDung getNguoiBan() { return this.nguoiBan.getNguoiDung(); }
//    public String getTenPhienDauGia() { return this.tenPhienDauGia; }
    public String getMaPhien() { return this.maPhien; }
//    public double getGiaKhoiDiem() { return this.giaKhoiDiem; }
    public boolean getdaCoGia() {
        return daCoGia;
    }
}