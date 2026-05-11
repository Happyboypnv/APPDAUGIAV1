package com.mycompany.server.dto;

public class DatGiaRequest {
    protected String maPhien;
    protected double gia;
    public DatGiaRequest(String maPhien, double gia) {
        this.maPhien = maPhien;
        this.gia      = gia;
    }
    public String getMaPhien() {
        return maPhien;
    }
    public double getGia() {
        return gia;
    }
}
