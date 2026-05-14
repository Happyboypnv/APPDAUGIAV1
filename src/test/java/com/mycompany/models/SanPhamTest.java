package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
public class SanPhamTest {
    private static SanPham sanPham;
    @BeforeEach
    public void setUp() {
        sanPham = new SanPham("Laptop", "SP001");
    }
    @Test
    @DisplayName("Test Getter SanPham")
    public void testGetterSanPham() {
        assertEquals(sanPham.layTenSanPham(), "Laptop");
        assertEquals(sanPham.layMaSanPham(), "SP001");
    }
    @Test
    @DisplayName("Test Setter SanPham")
    public void testSetterSanPham() {
        sanPham.suaTenSanPham("Smartphone");
        assertEquals(sanPham.layTenSanPham(), "Smartphone");
    }

}