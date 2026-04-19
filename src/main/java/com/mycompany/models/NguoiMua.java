package com.mycompany.models;
// Ap dung decorator, cho nguoi dung cac chuc nang dac biet cua rieng no o cac lop wrapper

import com.mycompany.action.HanhDongNguoiMua;

public class NguoiMua implements HanhDongNguoiMua {
    private NguoiDung nguoiDung;
    private PhienDauGia phienDauGia;

    public NguoiMua(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public void setPhienDauGia(PhienDauGia phienDauGia) {
        this.phienDauGia = phienDauGia;
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    @Override
    public void datGiaMoi(double giaMoi) {
        phienDauGia.capNhatThongTin(this, giaMoi);
    }

    @Override
    public void roiPhong() {
        phienDauGia.xoaKhoiPhong(this);
    }
}