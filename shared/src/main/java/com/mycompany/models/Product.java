package com.mycompany.models;

import java.io.Serializable;

public class Product implements Serializable {

    private String productName;
    private String productCode;
    private String description;
    private String category;

    public Product(String productName, String productCode) {
        this.productName = productName;
        this.productCode = productCode;
    }

    public Product(String productName, String productCode, String description, String category) {
        this(productName, productCode);
        this.description = description;
        this.category = category;
    }

    public String getProductName() {
        return this.productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
