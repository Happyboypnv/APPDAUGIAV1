package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConNguoiTest {
    private static ConNguoi conNguoi;
    @BeforeEach
    public void setUp() {
       conNguoi = new ConNguoi("Nguyen Van Phong","phong@gmail.com", "@Phong060207", "06/02/2007");
    }
    @Test
    @DisplayName("Test Getter ConNguoi")
    public void testGetterConNguoi() {
        assertEquals(conNguoi.layHoTen(), "Nguyen Van Phong");
        assertEquals(conNguoi.layThuDienTu(), "phong@gmail.com");
        assertEquals(conNguoi.layMatKhau(), "@Phong060207");
        assertEquals(conNguoi.layNgaySinh(), "06/02/2007");
    }
    @Test
    @DisplayName("Test Setter ConNguoi")
    public void testSetterConNguoi() {
        conNguoi.setMaNguoiDung("ND001");
        conNguoi.setHoTen("Nguyen Van A");
        conNguoi.setThuDienTu("nguyenvanphong@gmail.com");
        conNguoi.setMatKhau("123456");
        conNguoi.setNgaySinh("01/01/2000");
        assertEquals(conNguoi.layMaNguoiDung(), "ND001");
        assertEquals(conNguoi.layHoTen(), "Nguyen Van A");
        assertEquals(conNguoi.layThuDienTu(), "nguyenvanphong@gmail.com");
        assertEquals(conNguoi.layMatKhau(), "123456");
        assertEquals(conNguoi.layNgaySinh(), "01/01/2000");
    }
}