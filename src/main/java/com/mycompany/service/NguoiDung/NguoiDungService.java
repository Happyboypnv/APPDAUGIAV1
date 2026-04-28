package com.mycompany.service.NguoiDung;

import com.mycompany.models.NguoiDung;
import com.mycompany.utils.IKhoLuuTruNguoiDung;

public class NguoiDungService {
    private  final IKhoLuuTruNguoiDung kho;
    public NguoiDungService(IKhoLuuTruNguoiDung kho) {
        this.kho = kho;
    }
    public String timKiemNguoiDung(String email) {
        NguoiDung nguoiDung = kho.layTatCa().get(email);
        if(nguoiDung == null) return null;
        return nguoiDung.toString();
    }
}
