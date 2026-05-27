package com.mycompany.models;

public abstract class ItemFactory {
    public abstract Items createItem(String productName, String productCode, String description);

    public static ItemFactory getFactory(String category) {
        if (category == null) {
            return null;
        }
        return switch (category.trim().toLowerCase()) {
            case "art", "nghe thuat", "nghệ thuật","Nghệ thuật" -> new ArtFactory();
            case "vehicle", "vehicles", "xe", "Xe cộ" -> new VehicleFactory();
            case "electronics", "electronic", "dien tu", "điện tử", "Điện tử" -> new ElectronicsFactory();
            case "gemstone", "gemstones", "da quy", "đá quý", "Đá quý" -> new GemstoneFactory();
            default -> null;
        };
    }
}
