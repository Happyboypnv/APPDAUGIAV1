package com.mycompany.models;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
public class PhienDauGia {
    private String maPhienDauGia;
    private String tenPhienDauGia;
    private double giaKhoiDiem;
    private double giaDanDau;
    private double buocGia;
    // tinh thoi gian phien dau gia
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    // danh sach nguoi dat bid sao cho ai dat bid thi no duoc nhet vao list -> LIFO
    // -> nguoi thang cuoc se la nguoi vao cuoi cung
    // co the thay list bang stack
    private List<NguoiDung> danhSachNguoiTraGia = new ArrayList();
    private NguoiDung nguoiBan;
    private final double doLechGiaMin = 0.06;
    private NguoiDung nguoiThangCuoc;
    private String thoiGianDauGia;
    // doi thoi gian
    private final long donViGio = 3600L;
    private final long donViPhut = 60L;
    private SanPham sanPhanDauGia;
    // khi vao phien dau gia thi gia thap nhat nguoi choi co the dat la gia khoi diem
    // nguoi choi dat tien dat gia co the la gia khoi diem
    // bien ok hoat dong de check co ai dat gia hay chua
    private boolean ok = false;
    private Timeline thoiGianDemNguoc;
    private int giayConLai = 6;
    private Runnable endGame;
    private String trangThaiPhien = "DANG CHO";
    public PhienDauGia(String maPhienDauGia, String tenPhienDauGia, SanPham sanPhanDauGia, double giaKhoiDiem, NguoiDung nguoiBan) {
        this.maPhienDauGia = maPhienDauGia;
        this.tenPhienDauGia = tenPhienDauGia;
        this.giaKhoiDiem = giaKhoiDiem;
        this.nguoiBan = nguoiBan;
        this.buocGia = giaKhoiDiem * this.doLechGiaMin;
        this.giaDanDau = giaKhoiDiem;
        this.sanPhanDauGia = sanPhanDauGia;
        this.thoiGianDemNguoc = new Timeline(new KeyFrame[]{new KeyFrame(Duration.seconds((double)1.0F), (event) -> {
            --this.giayConLai;
            if (this.giayConLai > 0) {
                System.out.println("Hết giá sau: " + this.giayConLai + "s...");
            }
        }, new KeyValue[0])});
        // tao 5 giay dem nguoc dau tien
        this.thoiGianDemNguoc.setCycleCount(5);
        // khi thoi gian dem nguoc ket thuc thi no goi ra ham ketthucphien
        this.thoiGianDemNguoc.setOnFinished((event) -> this.ketThucPhien());
    }
    public void batDauPhien() {
        this.suaTrangThaiPhien("DANG DIEN RA");
        this.thoiGianBatDau = LocalDateTime.now();
        this.giayConLai = 6;
        this.thoiGianDemNguoc.playFromStart(); // bat dau dem nguoc
    }
    public boolean nguoiChoiTraGia(NguoiDung bidder, double giaMoi) {
        if (bidder.equals(this.nguoiBan)) {
            return false;
        } else if (!this.ok) {
            if (giaMoi >= this.giaDanDau) {
                this.giaDanDau = giaMoi;
                this.danhSachNguoiTraGia.add(bidder);
                this.ok = true;
                this.giayConLai = 6;
                this.thoiGianDemNguoc.playFromStart();
                return true;
            } else {
                return false;
            }
        } else if (giaMoi >= this.giaDanDau + this.buocGia) {
            this.giaDanDau = giaMoi;
            this.danhSachNguoiTraGia.add(bidder);
            this.giayConLai = 6;
            this.thoiGianDemNguoc.playFromStart();
            return true;
        } else {
            return false;
        }
    }
    public void ketThucPhien() {
        suaTrangThaiPhien("KET THUC");
        // tinh thoi gian dau gia
        this.thoiGianKetThuc = LocalDateTime.now();
        long tongSoGiay = ChronoUnit.SECONDS.between(this.thoiGianBatDau, this.thoiGianKetThuc);
        long gio = tongSoGiay / 3600L;
        long phut = tongSoGiay % 3600L / 60L;
        long giay = tongSoGiay % 60L;
        // 00:00:05
        this.thoiGianDauGia = String.format("%02d:%02d:%02d", gio, phut, giay);
        // tim ra nguoi chien thang
        if (!this.danhSachNguoiTraGia.isEmpty()) {
            this.nguoiThangCuoc = (NguoiDung)this.danhSachNguoiTraGia.get(this.danhSachNguoiTraGia.size() - 1);
        } else {
            this.nguoiThangCuoc = null;
        }
        if (this.endGame != null) {
            this.endGame.run();
        }
    }
    public void setOnAutionFinished(Runnable endGame) {
        this.endGame = endGame;
    }
    public double layGiaChot() {
        return this.giaDanDau;
    }
    public SanPham laySanPham() {
        return this.sanPhanDauGia;
    }
    public String layThoiGianDauGia() {
        return this.thoiGianDauGia;
    }
    public NguoiDung layNguoiThangCuoc() {
        return this.nguoiThangCuoc;
    }
    public NguoiDung layNguoiBan() {
        return this.nguoiBan;
    }
    public String layTenPhienDauGia() {
        return this.tenPhienDauGia;
    }
    public String layMaPhienDauGia() {
        return this.maPhienDauGia;
    }
    public double layGiaKhoiDiem() {
        return this.giaKhoiDiem;
    }
    public void suaTrangThaiPhien(String trangThaiPhien) {this.trangThaiPhien = trangThaiPhien;}
}
