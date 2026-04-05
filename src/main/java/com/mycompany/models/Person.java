package com.mycompany.models;

import java.io.Serializable;

public abstract class Person implements Serializable {
    private String id;
    private String fullName;
    private String email;
    private String birth;

    protected abstract String findPerson(Person person);

    @Override
    public String toString () {
        return "ID: " + id +
                "\nFullname: " + fullName +
                "\nEmail: " + email +
                "\nBrith: " + birth;
    }

    protected Person (String id, String fullName, String email, String birth) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.birth = birth;
    }
    protected Person(String fullName){this.fullName = fullName;}
    public String getFullName() {
        return fullName;
    }
    public String getEmail() {return this.email;}
    public String getBirth() {return this.birth;}
    public String getId() {return this.id;}
}
