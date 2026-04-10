package com.mycompany.models;

public class Admin extends ConNguoi {
    @Override
    protected String timKiemNguoiDung(ConNguoi other) {
        return other.toString();
    }

    //void banAccount

    Admin (String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen,  thuDienTu, matKhau, ngaySinh);
    }
}
