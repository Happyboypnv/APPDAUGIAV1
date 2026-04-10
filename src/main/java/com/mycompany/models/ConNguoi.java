package com.mycompany.models;

import java.io.Serializable;

public abstract class ConNguoi implements Serializable {
    private String maNguoiDung;
    private String hoTen;
    private String thuDienTu;
    private String ngaySinh;
    private String matKhau;
    protected abstract String timKiemNguoiDung(ConNguoi person);
    @Override
    public String toString () {
        return "maNguoiDung: " + maNguoiDung +
                "\nhoTen: " + hoTen +
                "\nthuDienTu: " + thuDienTu +
                "\nBrith: " + ngaySinh;
    }

    protected ConNguoi(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        this.hoTen = hoTen;
        this.thuDienTu = thuDienTu;
        this.matKhau = matKhau;
        this.ngaySinh = ngaySinh;
    }
    protected ConNguoi(String hoTen){this.hoTen = hoTen;}
    public String layHoTen() {
        return hoTen;
    }
    public String layThuDienTu() {return this.thuDienTu;}
    public String layNgaySinh() {return this.ngaySinh;}
    public String layMaNguoiDung() {return this.maNguoiDung;}
    public String layMatKhau() {
        return matKhau;
    }

    public void setMaNguoiDung(String maNguoiDung) {this.maNguoiDung = maNguoiDung;}
}
