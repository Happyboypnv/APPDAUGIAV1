package com.mycompany.models;

class Vehicle extends Items {

    public Vehicle(String name, String productCode, String description) {
        super(name, productCode, description, "Xe cộ");
    }

    @Override
    public String getDisplayInfo() {
        return "[Vehicle] " + this.getProductName() + " - " + description;
    }
}
