package com.mycompany.models;
import  java.io.*;
public class User implements Serializable{
    private String name;
    public User(String name){
        this.name = name;
    }
    public String getName(){return this.name;}
    public String toString(){return this.name;}
}
