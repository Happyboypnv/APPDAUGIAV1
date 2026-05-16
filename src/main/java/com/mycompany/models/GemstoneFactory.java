package com.mycompany.models;

public class GemstoneFactory extends ItemFactory {

    public Items createItem(String productName, String productCode, String description) {
        return new Gemstone(productName, productCode, description);
    }
}
