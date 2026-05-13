package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NguoiDung (Người Dùng) - User Management Test Suite")
public class NguoiDungTest {

    private static NguoiDung nguoiDung;
    private static NguoiDung nguoiDungDayDu;
    private static SanPham sanPham;
    private static PhienDauGia phienDauGia;

    @BeforeEach
    public void setUp() {
        // Constructor đơn giản: hoTen, email, password, ngaySinh
        nguoiDung = new NguoiDung("Nguyễn Văn Phong", "phong@gmail.com", "123456", "06/02/2007");

        // Constructor đầy đủ: hoTen, email, password, ngaySinh, diaChi, soDienThoai
        nguoiDungDayDu = new NguoiDung(
            "Trần Thị Linh",
            "linh@gmail.com",
            "password123",
            "15/05/2005",
            "123 Nguyễn Huệ, HCM",
            "0987654321"
        );

        // Tạo sản phẩm cho kiểm tra
        sanPham = new SanPham("SP001", "Laptop");
        NguoiDung nguoiBan = new NguoiDung("Nguyễn Văn A", "phong2007@gmail.com", "123456", "06/02/2007");
        phienDauGia = new PhienDauGia("P001", "Laptop", sanPham, 15000000, nguoiBan);
    }

    // ==========================================
    // KIỂM TRA CONSTRUCTOR
    // ==========================================
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with simple constructor")
        public void testSimpleConstructor() {
            assertNotNull(nguoiDung);
            assertEquals("Nguyễn Văn Phong", nguoiDung.layHoTen());
            assertEquals("phong@gmail.com", nguoiDung.layThuDienTu());
            assertEquals("123456", nguoiDung.layMatKhau());
            assertEquals("06/02/2007", nguoiDung.layNgaySinh());
        }

        @Test
        @DisplayName("Should create user with full constructor")
        public void testFullConstructor() {
            assertNotNull(nguoiDungDayDu);
            assertEquals("Trần Thị Linh", nguoiDungDayDu.layHoTen());
            assertEquals("linh@gmail.com", nguoiDungDayDu.layThuDienTu());
            assertEquals("password123", nguoiDungDayDu.layMatKhau());
            assertEquals("15/05/2005", nguoiDungDayDu.layNgaySinh());
            assertEquals("123 Nguyễn Huệ, HCM", nguoiDungDayDu.getDiaChi());
            assertEquals("0987654321", nguoiDungDayDu.getSoDienThoai());
        }

        @Test
        @DisplayName("Should initialize balance to 0 after construction")
        public void testInitialBalance() {
            assertEquals(0, nguoiDung.getSoDuKhaDung());
            assertEquals(0, nguoiDungDayDu.getSoDuKhaDung());
        }

        @Test
        @DisplayName("Should initialize empty transaction list")
        public void testInitialGiaoDichList() {
            assertNotNull(nguoiDung.layCacGiaoDich());
            assertTrue(nguoiDung.layCacGiaoDich().isEmpty());
        }
    }

    // ==========================================
    // KIỂM TRA ADDRESSS, PHONE & BALANCE
    // ==========================================
    @Nested
    @DisplayName("Address, Phone & Balance Tests")
    class AddressPhoneBalanceTests {

        @Test
        @DisplayName("Should get and set address correctly")
        public void testDiaChiGetterSetter() {
            String newAddress = "456 Lê Lợi, Hà Nội";
            nguoiDung.setDiaChi(newAddress);
            assertEquals(newAddress, nguoiDung.getDiaChi());
        }

        @Test
        @DisplayName("Should get and set phone number correctly")
        public void testSoDienThoaiGetterSetter() {
            String newPhone = "0912345678";
            nguoiDung.setSoDienThoai(newPhone);
            assertEquals(newPhone, nguoiDung.getSoDienThoai());
        }

        @Test
        @DisplayName("Should get and set available balance correctly")
        public void testSoDuKhaDungGetterSetter() {
            double newBalance = 5000000;
            nguoiDung.setSoDuKhaDung(newBalance);
            assertEquals(newBalance, nguoiDung.getSoDuKhaDung());
        }

        @Test
        @DisplayName("Should handle positive balance values")
        public void testPositiveBalance() {
            double[] testBalances = {100, 1000, 1000000, 999999999};
            for (double balance : testBalances) {
                nguoiDung.setSoDuKhaDung(balance);
                assertEquals(balance, nguoiDung.getSoDuKhaDung());
            }
        }

        @Test
        @DisplayName("Should handle zero balance")
        public void testZeroBalance() {
            nguoiDung.setSoDuKhaDung(0);
            assertEquals(0, nguoiDung.getSoDuKhaDung());
        }
    }
    
    @Nested
    @DisplayName("Bank Account & Bank Name Tests")
    class BankAccountTests {

        @Test
        @DisplayName("Should get and set bank account number")
        public void testSoTaiKhoanGetterSetter() {
            String accountNumber = "1234567890";
            nguoiDung.setSoTaiKhoan(accountNumber);
            assertEquals(accountNumber, nguoiDung.getSoTaiKhoan());
        }

        @Test
        @DisplayName("Should get and set bank name")
        public void testNganHangGetterSetter() {
            String bankName = "Vietcombank";
            nguoiDung.setNganHang(bankName);
            assertEquals(bankName, nguoiDung.getNganHang());
        }

        @Test
        @DisplayName("Should initialize bank account and bank name as null")
        public void testInitialBankAccountNull() {
            assertNull(nguoiDung.getSoTaiKhoan());
            assertNull(nguoiDung.getNganHang());
        }

        @Test
        @DisplayName("Should handle multiple bank updates")
        public void testMultipleBankUpdates() {
            nguoiDung.setNganHang("VietcomBank");
            assertEquals("VietcomBank", nguoiDung.getNganHang());

            nguoiDung.setNganHang("Techcombank");
            assertEquals("Techcombank", nguoiDung.getNganHang());

            nguoiDung.setNganHang("Sacombank");
            assertEquals("Sacombank", nguoiDung.getNganHang());
        }
    }
    @Nested
    @DisplayName("Transaction Management Tests")
    class GiaoDichTests {

        @Test
        @DisplayName("Should add transaction successfully")
        public void testThemGiaoDich() {
            GiaoDich giaoDich = new GiaoDich("GD001", phienDauGia);

            nguoiDung.themGiaoDich(giaoDich);
            List<GiaoDich> cacGiaoDich = nguoiDung.layCacGiaoDich();

            assertFalse(cacGiaoDich.isEmpty());
            assertEquals(1, cacGiaoDich.size());
        }

        @Test
        @DisplayName("Should add multiple transactions")
        public void testThemMultipleGiaoDich() {
            GiaoDich gd1 = new GiaoDich("GD001", phienDauGia);
            GiaoDich gd2 = new GiaoDich("GD002", phienDauGia);
            GiaoDich gd3 = new GiaoDich("GD003", phienDauGia);

            nguoiDung.themGiaoDich(gd1);
            nguoiDung.themGiaoDich(gd2);
            nguoiDung.themGiaoDich(gd3);

            List<GiaoDich> cacGiaoDich = nguoiDung.layCacGiaoDich();
            assertEquals(3, cacGiaoDich.size());
        }

        @Test
        @DisplayName("Should return non-null transaction list even if null initially")
        public void testLayCacGiaoDichNullSafety() {
            assertNotNull(nguoiDung.layCacGiaoDich());
            assertNotNull(nguoiDungDayDu.layCacGiaoDich());
        }

        @Test
        @DisplayName("Should handle multiple retrieval calls")
        public void testMultipleGiaoDichRetrieval() {
            GiaoDich giaoDich = new GiaoDich("GD001", phienDauGia);
            nguoiDung.themGiaoDich(giaoDich);

            List<GiaoDich> cacGiaoDich1 = nguoiDung.layCacGiaoDich();
            List<GiaoDich> cacGiaoDich2 = nguoiDung.layCacGiaoDich();

            assertEquals(cacGiaoDich1.size(), cacGiaoDich2.size());
            assertEquals(1, cacGiaoDich2.size());
        }
    }

    // ==========================================
    // KIỂM TRA INTERFACE IMPLEMENTATIONS
    // ==========================================
    @Nested
    @DisplayName("Interface Implementation Tests")
    class InterfaceTests {

        @Test
        @DisplayName("Should execute mua() method (CoTheBan interface)")
        public void testMuaInterface() {
            assertDoesNotThrow(() -> nguoiDung.mua(sanPham));
        }

        @Test
        @DisplayName("Should execute ban() method (CoTheRoiPhong interface)")
        public void testBanInterface() {
            assertDoesNotThrow(() -> nguoiDung.ban(sanPham));
        }

        @Test
        @DisplayName("Should execute roiKhoiPhong() method (CoTheTraGia interface)")
        public void testRoiKhoiPhongInterface() {
            assertDoesNotThrow(() -> nguoiDung.roiKhoiPhong());
        }

        @Test
        @DisplayName("Should execute all interface methods sequentially")
        public void testAllInterfaceMethodsSequentially() {
            assertDoesNotThrow(() -> {
                nguoiDung.mua(sanPham);
                nguoiDung.ban(sanPham);
                nguoiDung.roiKhoiPhong();
            });
        }
    }

    // ==========================================
    // KIỂM TRA INHERITED METHODS FROM ConNguoi
    // ==========================================
    @Nested
    @DisplayName("Inherited Methods from ConNguoi Tests")
    class InheritedMethodsTests {

        @Test
        @DisplayName("Should get user ID from parent class")
        public void testGetMaNguoiDung() {
            String userId = "USER001";
            nguoiDung.setMaNguoiDung(userId);
            assertEquals(userId, nguoiDung.layMaNguoiDung());
        }

        @Test
        @DisplayName("Should set and get user name")
        public void testSetGetHoTen() {
            String newName = "Lê Văn Anh";
            nguoiDung.setHoTen(newName);
            assertEquals(newName, nguoiDung.layHoTen());
        }

        @Test
        @DisplayName("Should set and get email")
        public void testSetGetEmail() {
            String newEmail = "newemail@gmail.com";
            nguoiDung.setThuDienTu(newEmail);
            assertEquals(newEmail, nguoiDung.layThuDienTu());
        }

        @Test
        @DisplayName("Should set and get birth date")
        public void testSetGetNgaySinh() {
            String newBirthDate = "01/01/2000";
            nguoiDung.setNgaySinh(newBirthDate);
            assertEquals(newBirthDate, nguoiDung.layNgaySinh());
        }

        @Test
        @DisplayName("Should set and get password")
        public void testSetGetMatKhau() {
            String newPassword = "newPassword123";
            nguoiDung.setMatKhau(newPassword);
            assertEquals(newPassword, nguoiDung.layMatKhau());
        }
    }

    // ==========================================
    // KIỂM TRA STRING REPRESENTATION
    // ==========================================
    @Nested
    @DisplayName("String Representation Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return valid toString representation")
        public void testToString() {
            String result = nguoiDung.toString();
            assertNotNull(result);
            assertTrue(result.contains("maNguoiDung"));
            assertTrue(result.contains("hoTen"));
            assertTrue(result.contains("thuDienTu"));
            assertTrue(result.contains("ngaySinh"));
        }

        @Test
        @DisplayName("Should contain all user information in toString")
        public void testToStringContent() {
            String result = nguoiDung.toString();
            assertTrue(result.contains("Nguyễn Văn Phong"));
            assertTrue(result.contains("phong@gmail.com"));
            assertTrue(result.contains("06/02/2007"));
        }
    }

    // ==========================================
    // KIỂM TRA COMPLEX SCENARIOS
    // ==========================================
    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle complete user profile setup")
        public void testCompleteProfileSetup() {
            // Setup profile
            nguoiDung.setDiaChi("789 Đinh Bộ Lĩnh, Hà Nội");
            nguoiDung.setSoDienThoai("0963123456");
            nguoiDung.setNganHang("TPBank");
            nguoiDung.setSoTaiKhoan("1234567890");
            nguoiDung.setSoDuKhaDung(10000000);

            // Verify all fields
            assertEquals("789 Đinh Bộ Lĩnh, Hà Nội", nguoiDung.getDiaChi());
            assertEquals("0963123456", nguoiDung.getSoDienThoai());
            assertEquals("TPBank", nguoiDung.getNganHang());
            assertEquals("1234567890", nguoiDung.getSoTaiKhoan());
            assertEquals(10000000, nguoiDung.getSoDuKhaDung());
        }

        @Test
        @DisplayName("Should manage transactions with balance updates")
        public void testTransactionsWithBalance() {
            // Chuẩn bị
            nguoiDung.setSoDuKhaDung(50000000);
            GiaoDich gd = new GiaoDich("GD001", phienDauGia);

            // Đăng ký giao dịch
            nguoiDung.themGiaoDich(gd);

            // Giả lập khoảng trừ tiền
            double newBalance = nguoiDung.getSoDuKhaDung() - 5000000;
            nguoiDung.setSoDuKhaDung(newBalance);

            // Kiểm tra
            assertEquals(45000000, nguoiDung.getSoDuKhaDung());
            assertEquals(1, nguoiDung.layCacGiaoDich().size());
        }

        @Test
        @DisplayName("Should handle multiple users independently")
        public void testMultipleUsersIndependently() {
            NguoiDung user1 = new NguoiDung("User1", "user1@gmail.com", "pass1", "01/01/2000");
            NguoiDung user2 = new NguoiDung("User2", "user2@gmail.com", "pass2", "02/02/2001");

            user1.setSoDuKhaDung(1000000);
            user2.setSoDuKhaDung(2000000);

            assertEquals(1000000, user1.getSoDuKhaDung());
            assertEquals(2000000, user2.getSoDuKhaDung());

            user1.themGiaoDich(new GiaoDich("GD001", phienDauGia));
            assertEquals(0, user2.layCacGiaoDich().size());
        }
    }
}
