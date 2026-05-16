package com.mycompany.models;

abstract class Items extends Product {

    protected String description;
    protected String category;

    public Items(String productName, String productCode, String description, String category) {
        super(productName, productCode);
        this.description = description;
        this.category = category;
    }

    public abstract String getDisplayInfo();
}
