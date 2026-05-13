package com.mycompany.server.dto;

import java.util.List;

public class LichSuDatGiaResponse {
    protected String maPhien;
    protected double          giaHienTai;
    protected String          trangThai;
    protected int             soLuotDatGia;
    protected String          nguoiDangThang;
    protected List<LuotDatGia> lichSu;

    public LichSuDatGiaResponse(String maPhien, double giaHienTai, String trangThai,
                         int soLuotDatGia, String nguoiDangThang, List<LuotDatGia> lichSu) {
        this.maPhien        = maPhien;
        this.giaHienTai     = giaHienTai;
        this.trangThai      = trangThai;
        this.soLuotDatGia   = soLuotDatGia;
        this.nguoiDangThang = nguoiDangThang;
        this.lichSu         = lichSu;
    }
    public String getMaPhien() {
        return maPhien;
    }
    public double getGiaHienTai() {
        return giaHienTai;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public int getSoLuotDatGia() {
        return soLuotDatGia;
    }
    public String getNguoiDangThang() {
        return nguoiDangThang;
    }
    public List<LuotDatGia> getLichSu() {
        return lichSu;
    }
}
