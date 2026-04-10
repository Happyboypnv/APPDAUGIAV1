package com.mycompany.models;

import java.io.Serializable;

public class Product implements Serializable {
    private String name;
    private final String id;
    public Product(String name, String id) {
        this.name = name;
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return this.id;
    }
}
