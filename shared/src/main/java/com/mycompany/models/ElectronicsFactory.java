package com.mycompany.models;

public class ElectronicsFactory extends ItemFactory {

    public Items createItem(String productName, String productCode, String description) {
        return new Electronics(productName, productCode, description);
    }
}
