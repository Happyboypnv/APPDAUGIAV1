package com.mycompany.models;

public abstract class Items extends Product {

    protected String description;
    protected String category;

    public Items(String productName, String productCode, String description, String category) {
        super(productName, productCode);
        this.description = description;
        this.category = category;
    }

    public abstract String getDisplayInfo();

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}
