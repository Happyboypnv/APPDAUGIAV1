package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;
public interface IKhoLuuTruNguoiDung {
    void luu(NguoiDung nguoiDung);
    NguoiDung timTheoMa(String maTimKiem);
    Map<String, NguoiDung> layTatCa();

}
