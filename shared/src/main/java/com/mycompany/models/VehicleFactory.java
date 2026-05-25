package com.mycompany.models;

public class VehicleFactory extends ItemFactory {

    public Items createItem(String productName, String productCode, String description) {
        return new Vehicle(productName, productCode, description);
    }
}
