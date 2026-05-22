package com.mycompany.models;

public class Admin extends Person{

    public Admin(String fullName, String email, String password, String dateOfBirth, int role) {
        super(fullName, email, password, dateOfBirth, 1);
    }


}