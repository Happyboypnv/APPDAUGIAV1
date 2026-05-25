package com.mycompany.models;

public abstract class ItemFactory {
    public static ItemFactory getFactory(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "Nghệ thuật" -> new ArtFactory();
            case "Đá quý" -> new GemstoneFactory();
            case "Xe cộ", "Xe Cộ" -> new VehicleFactory();
            case "Điện tử" -> new ElectronicsFactory();
            default -> null;
        };
    }

    public abstract Items createItem(String productName, String productCode, String description);
}
