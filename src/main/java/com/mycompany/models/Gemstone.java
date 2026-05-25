package com.mycompany.models;

class Gemstone extends Items {

    public Gemstone(String name, String productCode, String description) {
        super(name, productCode, description, "Đá quý");
    }

    @Override
    public String getDisplayInfo() {
        return "[Gemstone] " + this.getProductName() + " - " + description;
    }
}
