package com.mycompany.models;

public class Admin extends Person {
    @Override
    protected String findPerson(Person other) {
        return other.toString();
    }

    //void banAccount

    Admin (String id, String fullName, String email, String birth) {
        super(id, fullName, email, birth);
    }
}
