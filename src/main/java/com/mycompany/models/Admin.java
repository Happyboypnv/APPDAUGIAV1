// Admin.java — gọn lại, không cần override vô nghĩa
package com.mycompany.models;

public class Admin extends ConNguoi {

    //void banAccount

    Admin(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
    }
    // Nếu Admin cần tìm kiếm người dùng, inject service qua constructor (DIP)
}
