package com.mycompany.models;

public abstract class ItemFactory {
    public abstract Items createItem(String productName, String productCode, String description);
}
