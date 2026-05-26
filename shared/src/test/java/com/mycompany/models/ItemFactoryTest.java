package com.mycompany.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Factory Method pattern and Item class hierarchy.
 *
 * Covers: ItemFactory, ArtFactory, ElectronicsFactory, VehicleFactory, GemstoneFactory.
 */
@DisplayName("Factory pattern and Item hierarchy tests")
class ItemFactoryTest {

    @Test
    @DisplayName("ElectronicsFactory tạo đúng instance Electronics")
    void electronicsFactory_createsElectronics() {
        ItemFactory factory = new ElectronicsFactory();
        Items item = factory.createItem("Laptop Dell", "E-001", "Laptop gaming");
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    @DisplayName("ArtFactory tạo đúng instance Art")
    void artFactory_createsArt() {
        ItemFactory factory = new ArtFactory();
        Items item = factory.createItem("Tranh Son Dau", "A-001", "Tranh phong canh");
        assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("VehicleFactory tạo đúng instance Vehicle")
    void vehicleFactory_createsVehicle() {
        ItemFactory factory = new VehicleFactory();
        Items item = factory.createItem("Honda Wave", "V-001", "Xe may cu");
        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    @DisplayName("GemstoneFactory tạo đúng instance Gemstone")
    void gemstoneFactory_createsGemstone() {
        ItemFactory factory = new GemstoneFactory();
        Items item = factory.createItem("Kim Cuong", "G-001", "Kim cuong 2 cara");
        assertInstanceOf(Gemstone.class, item);
    }

    @Test
    @DisplayName("Item được tạo bởi factory không được null")
    void factory_doesNotReturnNull() {
        ItemFactory factory = new ElectronicsFactory();
        assertNotNull(factory.createItem("TV Samsung", "E-002", "Smart TV 4K"));
    }

    @Test
    @DisplayName("Items kế thừa Product — là subtype của Product")
    void items_isSubtypeOfProduct() {
        ItemFactory factory = new ElectronicsFactory();
        Items item = factory.createItem("Phone", "E-003", "Smartphone");
        assertInstanceOf(Product.class, item);
    }

    @Test
    @DisplayName("Electronics có getDisplayInfo không null")
    void electronics_displayInfo_notNull() {
        ItemFactory factory = new ElectronicsFactory();
        Items item = factory.createItem("MacBook", "E-004", "Laptop Apple");
        assertNotNull(item.getDisplayInfo());
    }
}
