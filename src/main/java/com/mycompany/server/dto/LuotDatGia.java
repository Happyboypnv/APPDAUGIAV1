package com.mycompany.server.dto;

public class LuotDatGia {
    protected int    stt;
    protected String tenNguoiDat;
    protected String maNguoiDat;
    protected String thoiGian; // THÊM MỚI

    public LuotDatGia(int stt, String tenNguoiDat, String maNguoiDat, String thoiGian) {
        this.stt         = stt;
        this.tenNguoiDat = tenNguoiDat;
        this.maNguoiDat  = maNguoiDat;
        this.thoiGian    = thoiGian; // THÊM MỚI
    }
    public int getStt() { return stt; }
    public String getTenNguoiDat() { return tenNguoiDat; }
    public String getMaNguoiDat() { return maNguoiDat; }
    public String getThoiGian() { return thoiGian; } // THÊM MỚI
}