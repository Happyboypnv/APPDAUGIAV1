package com.mycompanytest;

import com.mycompanytest.models.*;
import com.mycompanytest.service.*;

public class Main {
    // Hàm main chuẩn của Java, không dính dáng gì đến JavaFX
    public static void main(String[] args) {
        // Gọi hàm main của file App.java từ đây
        QuanLyCacPhienService quanLyCacPhien = new QuanLyCacPhienService();
        PhienDauGiaService quanLyPhien = new PhienDauGiaService();

        NguoiDung seller_top_1_vn = new NguoiDung("Nguyen Thanh Tuan");
        quanLyCacPhien.them(new PhienDauGia("phien1", "phien 1", new com.mycompanytest.models.SanPham("bao tay", "sanpham1"), 10, seller_top_1_vn));

        for (String key : quanLyCacPhien.danhSachPhien.keySet()) {
            System.out.println(quanLyCacPhien.tim(key));
        }

        quanLyPhien.batDauPhien(quanLyCacPhien.tim("phien1"));


        for (String key : quanLyCacPhien.danhSachPhien.keySet()) {
            System.out.println(quanLyCacPhien.tim(key));
        }

        quanLyPhien.dongPhien(quanLyCacPhien.tim("phien1"));

        for (String key : quanLyCacPhien.danhSachPhien.keySet()) {
            System.out.println(quanLyCacPhien.tim(key));
        }
        App.main(args);
    }
}