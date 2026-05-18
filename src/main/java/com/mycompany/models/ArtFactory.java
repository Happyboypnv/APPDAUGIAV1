package com.mycompany.models;

public class ArtFactory extends ItemFactory {

    public Items createItem(String productName, String productCode, String description) {
        return new Art(productName, productCode, description);
    }
}
