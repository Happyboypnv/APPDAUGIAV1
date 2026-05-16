package com.mycompany.models;

class Electronics extends Items {

    public Electronics(String name, String productCode, String description) {
        super(name, productCode, description, "Electronics");
    }

    @Override
    public String getDisplayInfo() {
        return "[Electronics] " + this.getProductName() + " - " + description;
    }
}
