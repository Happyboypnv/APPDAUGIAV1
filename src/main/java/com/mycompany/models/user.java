package com.mycompany.models;
import  java.io.*;
public class user implements Serializable{
    private static final long serialVersionUID = 1L;
    public String name;
    public transient String password;
    public user(String name, String password) {
        this.name = name;
        this.password = password;
    }
    @Override
    public String toString() {
        return name + ","  + password;
    }
}
