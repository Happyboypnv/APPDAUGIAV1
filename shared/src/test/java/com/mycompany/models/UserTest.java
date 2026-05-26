package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User model.
 *
 * Covers: balance logic, inheritance, interface contracts.
 */
@DisplayName("User model tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Nguyen Van A", "a@test.com", "password123", "2000-05-15",
                "Ha Noi", "0901234567");
        user.setUserId("user-001");
    }

    @Test
    @DisplayName("User kế thừa Person — getFullName trả về đúng")
    void inheritsFromPerson_fullName() {
        assertEquals("Nguyen Van A", user.getFullName());
    }

    @Test
    @DisplayName("User kế thừa Person — getEmail trả về đúng")
    void inheritsFromPerson_email() {
        assertEquals("a@test.com", user.getEmail());
    }

    @Test
    @DisplayName("getAvailableBalance = actualBalance - frozenBalance")
    void availableBalance_formula() {
        user.setActualBalance(10_000_000);
        user.setFrozenBalance(3_000_000);
        assertEquals(7_000_000.0, user.getAvailableBalance(), 0.001);
    }

    @Test
    @DisplayName("getAvailableBalance = actualBalance khi frozenBalance = 0")
    void availableBalance_noFrozen() {
        user.setActualBalance(5_000_000);
        user.setFrozenBalance(0);
        assertEquals(5_000_000.0, user.getAvailableBalance(), 0.001);
    }

    @Test
    @DisplayName("Sau khi nạp tiền, actualBalance tăng đúng")
    void deposit_increasesActualBalance() {
        user.setActualBalance(0);
        double deposit = 2_000_000;
        user.setActualBalance(user.getActualBalance() + deposit);
        assertEquals(2_000_000.0, user.getActualBalance(), 0.001);
    }

    @Test
    @DisplayName("Admin là subtype của Person")
    void admin_isInstanceOfPerson() {
        Admin admin = new Admin("Admin User", "admin@test.com", "adminpass", "1990-01-01");
        assertInstanceOf(Person.class, admin);
    }

    @Test
    @DisplayName("User implement CanBid")
    void user_implementsCanBid() {
        assertInstanceOf(CanBid.class, user);
    }

    @Test
    @DisplayName("User implement CanBeSold")
    void user_implementsCanBeSold() {
        assertInstanceOf(CanBeSold.class, user);
    }

    @Test
    @DisplayName("User implement CanLeaveRoom")
    void user_implementsCanLeaveRoom() {
        assertInstanceOf(CanLeaveRoom.class, user);
    }

    @Test
    @DisplayName("getAddress trả về đúng địa chỉ")
    void getAddress_correct() {
        assertEquals("Ha Noi", user.getAddress());
    }

    @Test
    @DisplayName("getPhoneNumber trả về đúng")
    void getPhoneNumber_correct() {
        assertEquals("0901234567", user.getPhoneNumber());
    }

    @Test
    @DisplayName("setAvailableBalance cập nhật actualBalance giữ nguyên frozen")
    void setAvailableBalance_updatesActual() {
        user.setFrozenBalance(1_000_000);
        user.setAvailableBalance(4_000_000);
        // actualBalance = newBalance + frozenBalance = 4M + 1M = 5M
        assertEquals(5_000_000.0, user.getActualBalance(), 0.001);
    }
}
