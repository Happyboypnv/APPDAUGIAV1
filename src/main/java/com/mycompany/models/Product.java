package com.mycompany.models;

import java.io.Serializable;

public class Product implements Serializable {

    private String productName;
    private String productCode;

    public Product(String productName, String productCode) {
        this.productName = productName;
        this.productCode = productCode;
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
}
