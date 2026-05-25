package com.mycompany.models;

class Art extends Items {

    public Art(String name, String productCode, String description) {
        super(name, productCode, description, "Nghệ thuật");
    }

    @Override
    public String getDisplayInfo() {
        return "[Art] " + this.getProductName() + " - " + description;
    }
}
