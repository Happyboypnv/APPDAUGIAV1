package com.mycompany.server.dto;

public class DatGiaResponse {
    protected String thongBao;
    protected double giaHienTai;
    protected String thoiGianKetThuc;

    public DatGiaResponse(String thongBao, double giaHienTai, String thoiGianKetThuc) {
        this.thongBao       = thongBao;
        this.giaHienTai     = giaHienTai;
        this.thoiGianKetThuc = thoiGianKetThuc;
    }
}

