//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

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
    private String id;
    private String name;
    private double giaKhoiDiem;
    private double giaDanDau;
    private double buocGia;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private List<User> listBidders = new ArrayList();
    private User seller;
    private final double doLechGiaMin = 0.06;
    private User winner;
    private String thoiGianDauGia;
    private final long donViGio = 3600L;
    private final long donViPhut = 60L;
    private Product sanPhanDauGia;
    private boolean ok = false;
    private Timeline countDownTimeLine;
    private int giayConLai = 6;
    private Runnable endGame;

    public PhienDauGia(String id, String name, Product sanPhanDauGia, double giaKhoiDiem, User seller) {
        this.id = id;
        this.name = name;
        this.giaKhoiDiem = giaKhoiDiem;
        this.seller = seller;
        this.buocGia = giaKhoiDiem * 0.06;
        this.giaDanDau = giaKhoiDiem;
        this.sanPhanDauGia = sanPhanDauGia;
        this.countDownTimeLine = new Timeline(new KeyFrame[]{new KeyFrame(Duration.seconds((double)1.0F), (event) -> {
            --this.giayConLai;
            if (this.giayConLai > 0) {
                System.out.println("Hết giá sau: " + this.giayConLai + "s...");
            }

        }, new KeyValue[0])});
        this.countDownTimeLine.setCycleCount(5);
        this.countDownTimeLine.setOnFinished((event) -> this.ketThucPhien());
    }

    public void batDauPhien() {
        this.thoiGianBatDau = LocalDateTime.now();
        this.giayConLai = 6;
        this.countDownTimeLine.playFromStart();
    }

    public boolean nguoiChoiTraGia(User bidder, double giaMoi) {
        if (bidder.equals(this.seller)) {
            return false;
        } else if (!this.ok) {
            if (giaMoi >= this.giaDanDau) {
                this.giaDanDau = giaMoi;
                this.listBidders.add(bidder);
                this.ok = true;
                this.giayConLai = 6;
                this.countDownTimeLine.playFromStart();
                return true;
            } else {
                return false;
            }
        } else if (giaMoi >= this.giaDanDau + this.buocGia) {
            this.giaDanDau = giaMoi;
            this.listBidders.add(bidder);
            this.giayConLai = 6;
            this.countDownTimeLine.playFromStart();
            return true;
        } else {
            return false;
        }
    }

    public void ketThucPhien() {
        this.thoiGianKetThuc = LocalDateTime.now();
        long tongSoGiay = ChronoUnit.SECONDS.between(this.thoiGianBatDau, this.thoiGianKetThuc);
        long gio = tongSoGiay / 3600L;
        long phut = tongSoGiay % 3600L / 60L;
        long giay = tongSoGiay % 60L;
        this.thoiGianDauGia = String.format("%02d:%02d:%02d", gio, phut, giay);
        if (!this.listBidders.isEmpty()) {
            this.winner = (User)this.listBidders.get(this.listBidders.size() - 1);
        } else {
            this.winner = null;
        }

        if (this.endGame != null) {
            this.endGame.run();
        }

    }

    public void setOnAutionFinished(Runnable endGame) {
        this.endGame = endGame;
    }

    public double getGiaChot() {
        return this.giaDanDau;
    }

    public Product getSanPham() {
        return this.sanPhanDauGia;
    }

    public String getThoiGianDauGia() {
        return this.thoiGianDauGia;
    }

    public User getWinner() {
        return this.winner;
    }

    public User getSeller() {
        return this.seller;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public double getGiaKhoiDiem() {
        return this.giaKhoiDiem;
    }
}
