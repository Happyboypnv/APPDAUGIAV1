package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;
public interface IKhoLuuTruNguoiDung {
    void luu(NguoiDung nguoiDung);
    Map<String, NguoiDung> layTatCa();
    boolean kiemTraNguoiDung(String email, String password);
    boolean kiemTraEmail(String email);
}