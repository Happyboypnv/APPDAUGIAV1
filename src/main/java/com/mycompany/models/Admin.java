package com.mycompany.models;

public class Admin extends ConNguoi {
    @Override
    protected String timKiemNguoiDung(ConNguoi other) {
        return other.toString();
    }

    //void banAccount

    Admin (String id, String fullName, String email, String birth) {
        super(id, fullName, email, birth);
    }
}
