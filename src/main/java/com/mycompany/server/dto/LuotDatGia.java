package com.mycompany.server.dto;

public class LuotDatGia {
    protected int    stt;
    protected String tenNguoiDat;
    protected String maNguoiDat;

    public LuotDatGia(int stt, String tenNguoiDat, String maNguoiDat) {
        this.stt          = stt;
        this.tenNguoiDat  = tenNguoiDat;
        this.maNguoiDat   = maNguoiDat;
    }
    public int getStt() {
        return stt;
    }
    public String getTenNguoiDat() {
        return tenNguoiDat;
    }
    public String getMaNguoiDat() {
        return maNguoiDat;
    }
}
