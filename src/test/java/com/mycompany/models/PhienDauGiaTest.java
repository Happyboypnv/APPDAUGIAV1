package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PhienDauGia (Auction Session) Test Suite")
public class PhienDauGiaTest {
    private PhienDauGia phienDauGia;
    private NguoiDung nguoiBan;
    private NguoiDung nguoiBan2;
    private NguoiDung nguoiMua1;
    private NguoiDung nguoiMua2;
    private SanPham sanPham;

    @BeforeEach
    public void setUp() {
        // Set up users
        nguoiBan = new NguoiDung("MaiQuan", "seller@gmail.com", "123456", "01/01/1990");
        nguoiBan2 = new NguoiDung("NguyenTan", "seller2@gmail.com", "123456", "05/05/1992");
        nguoiMua1 = new NguoiDung("LeVan", "buyer1@gmail.com", "123456", "10/10/1995");
        nguoiMua2 = new NguoiDung("PhamHoa", "buyer2@gmail.com", "123456", "15/03/1998");

        // Setup product
        sanPham = new SanPham("iPhone 15 Pro", "IPHONE15");

        // Set up auction session
        phienDauGia = new PhienDauGia("P001", "iPhone 15 Pro Auction", sanPham, 1000.0, nguoiBan, 3600);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("Should create auction session with all parameters")
        void testConstructorWithAllParameters() {
            PhienDauGia auction = new PhienDauGia("P002", "Samsung Galaxy", sanPham, 500.0, nguoiBan, 1800);
            assertEquals("P002", auction.getMaPhien());
            assertEquals("Samsung Galaxy", auction.getTenPhienDauGia());
            assertEquals(500.0, auction.getGiaHienTai());
            assertEquals(nguoiBan, auction.getNguoiBan());
            assertEquals(1800, auction.getThoiGian());
            assertEquals(TrangThaiPhien.DANG_CHO, auction.getTrangThai());
        }

        @Test
        @DisplayName("Should create auction session without duration (database constructor)")
        void testConstructorWithoutDuration() {
            PhienDauGia auction = new PhienDauGia("P003", "Galaxy Watch", sanPham, 200.0, nguoiBan);
            assertEquals("P003", auction.getMaPhien());
            assertEquals(200.0, auction.getGiaHienTai());
            assertEquals(0, auction.getThoiGian());
        }

        @Test
        @DisplayName("Initial state should be DANG_CHO and not closed")
        void testInitialState() {
            assertEquals(TrangThaiPhien.DANG_CHO, phienDauGia.getTrangThai());
            assertFalse(phienDauGia.isClosed());
            assertFalse(phienDauGia.getdaCoGia());
            assertNull(phienDauGia.getNguoiThangCuoc());
        }
    }

    @Nested
    @DisplayName("Price and Bid Step Tests")
    class PriceAndBidTests {
        @Test
        @DisplayName("Should get current auction price")
        void testGetGiaHienTai() {
            assertEquals(1000.0, phienDauGia.getGiaHienTai());
        }

        @Test
        @DisplayName("Should set and update current price")
        void testSetGiaHienTai() {
            phienDauGia.setGiaHienTai(1500.0);
            assertEquals(1500.0, phienDauGia.getGiaHienTai());
        }

        @Test
        @DisplayName("Should handle bid step (buocGia)")
        void testBuocGia() {
            phienDauGia.setBuocGia(100.0);
            assertEquals(100.0, phienDauGia.getBuocGia());
        }

        @Test
        @DisplayName("Should return minimum price difference (doLechGiaMin)")
        void testDoLechGiaMin() {
            assertEquals(0.06, phienDauGia.getDoLechGiaMin());
        }

        @Test
        @DisplayName("Should track if auction has received any bids")
        void testDaCoGia() {
            assertFalse(phienDauGia.getdaCoGia());
            phienDauGia.setDaCoGia(true);
            assertTrue(phienDauGia.getdaCoGia());
        }

        @Test
        @DisplayName("Should update price with multiple increments")
        void testMultiplePriceUpdates() {
            phienDauGia.setGiaHienTai(1000.0);
            assertEquals(1000.0, phienDauGia.getGiaHienTai());

            phienDauGia.setGiaHienTai(1100.0);
            assertEquals(1100.0, phienDauGia.getGiaHienTai());

            phienDauGia.setGiaHienTai(1300.0);
            assertEquals(1300.0, phienDauGia.getGiaHienTai());
        }
    }

    @Nested
    @DisplayName("Time Management Tests")
    class TimeManagementTests {
        @Test
        @DisplayName("Should set and get start time")
        void testThoiGianBatDau() {
            LocalDateTime startTime = LocalDateTime.now();
            phienDauGia.setThoiGianBatDau(startTime);
            assertEquals(startTime, phienDauGia.getThoiGianBatDau());
        }

        @Test
        @DisplayName("Should set and get end time")
        void testThoiGianKetThuc() {
            LocalDateTime endTime = LocalDateTime.now().plusHours(1);
            phienDauGia.setThoiGianKetThuc(endTime);
            assertEquals(endTime, phienDauGia.getThoiGianKetThuc());
        }

        @Test
        @DisplayName("Should set and get duration (thoiGian)")
        void testThoiGian() {
            phienDauGia.setThoiGian(7200);
            assertEquals(7200, phienDauGia.getThoiGian());
        }

        @Test
        @DisplayName("Should handle time sequence (start -> end)")
        void testTimeSequence() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusHours(2);

            phienDauGia.setThoiGianBatDau(start);
            phienDauGia.setThoiGianKetThuc(end);
            phienDauGia.setThoiGian(7200);

            assertEquals(start, phienDauGia.getThoiGianBatDau());
            assertEquals(end, phienDauGia.getThoiGianKetThuc());
            assertEquals(7200, phienDauGia.getThoiGian());
        }
    }

    @Nested
    @DisplayName("Status and State Tests")
    class StatusTests {
        @Test
        @DisplayName("Should set and get auction status")
        void testTrangThai() {
            assertEquals(TrangThaiPhien.DANG_CHO, phienDauGia.getTrangThai());

            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            assertEquals(TrangThaiPhien.DANG_DIEN_RA, phienDauGia.getTrangThai());

            phienDauGia.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
            assertEquals(TrangThaiPhien.DA_THANH_TOAN, phienDauGia.getTrangThai());
        }

        @Test
        @DisplayName("Should handle all auction status states")
        void testAllTrangThaiStates() {
            phienDauGia.setTrangThai(TrangThaiPhien.DANG_CHO);
            assertSame(TrangThaiPhien.DANG_CHO, phienDauGia.getTrangThai());

            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            assertSame(TrangThaiPhien.DANG_DIEN_RA, phienDauGia.getTrangThai());

            phienDauGia.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
            assertSame(TrangThaiPhien.DA_THANH_TOAN, phienDauGia.getTrangThai());

            phienDauGia.setTrangThai(TrangThaiPhien.DA_HUY);
            assertSame(TrangThaiPhien.DA_HUY, phienDauGia.getTrangThai());
        }

        @Test
        @DisplayName("Should set and check closed status")
        void testClosedStatus() {
            assertFalse(phienDauGia.isClosed());
            phienDauGia.setClosed(true);
            assertTrue(phienDauGia.isClosed());
        }
    }

    @Nested
    @DisplayName("Winner Determination Tests")
    class WinnerTests {
        @Test
        @DisplayName("Should return null winner if no bidders")
        void testNoWinnerWhenNoBidders() {
            assertNull(phienDauGia.getNguoiThangCuoc());
        }

        @Test
        @DisplayName("Should set winner manually")
        void testSetWinnerManually() {
            phienDauGia.setNguoiThangCuoc(nguoiMua1);
            assertEquals(nguoiMua1, phienDauGia.getNguoiThangCuoc());
        }

        @Test
        @DisplayName("Should set winner from bidder list (last bidder)")
        void testSetWinnerFromBidderList() {
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.addNguoiTraGia(nguoiMua2);

            phienDauGia.setNguoiThangCuoc();

            assertEquals(nguoiMua2, phienDauGia.getNguoiThangCuoc());
        }

        @Test
        @DisplayName("Should update winner when new bidder added")
        void testUpdateWinnerWithNewBidder() {
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.setNguoiThangCuoc();
            assertEquals(nguoiMua1, phienDauGia.getNguoiThangCuoc());

            phienDauGia.addNguoiTraGia(nguoiMua2);
            phienDauGia.setNguoiThangCuoc();
            assertEquals(nguoiMua2, phienDauGia.getNguoiThangCuoc());
        }

        @Test
        @DisplayName("Should handle winner change scenario")
        void testWinnerChange() {
            NguoiDung newBidder = new NguoiDung("TranHung", "buyer3@gmail.com", "123456", "20/12/1997");

            phienDauGia.setNguoiThangCuoc(nguoiMua1);
            assertEquals(nguoiMua1, phienDauGia.getNguoiThangCuoc());

            phienDauGia.setNguoiThangCuoc(newBidder);
            assertEquals(newBidder, phienDauGia.getNguoiThangCuoc());
        }
    }

    @Nested
    @DisplayName("Bidder List Management Tests")
    class BidderListTests {
        @Test
        @DisplayName("Should initialize empty bidder list")
        void testEmptyBidderList() {
            assertTrue(phienDauGia.getDanhSachNguoiTraGia().isEmpty());
        }

        @Test
        @DisplayName("Should add single bidder")
        void testAddSingleBidder() {
            phienDauGia.addNguoiTraGia(nguoiMua1);
            assertEquals(1, phienDauGia.getDanhSachNguoiTraGia().size());
            assertTrue(phienDauGia.getDanhSachNguoiTraGia().contains(nguoiMua1));
        }

        @Test
        @DisplayName("Should add multiple bidders")
        void testAddMultipleBidders() {
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.addNguoiTraGia(nguoiMua2);

            assertEquals(2, phienDauGia.getDanhSachNguoiTraGia().size());
            assertTrue(phienDauGia.getDanhSachNguoiTraGia().contains(nguoiMua1));
            assertTrue(phienDauGia.getDanhSachNguoiTraGia().contains(nguoiMua2));
        }

        @Test
        @DisplayName("Should get bidder list")
        void testGetBidderList() {
            List<NguoiDung> bidders = phienDauGia.getDanhSachNguoiTraGia();
            assertNotNull(bidders);

            bidders.add(nguoiMua1);
            bidders.add(nguoiMua2);

            assertEquals(2, bidders.size());
        }

        @Test
        @DisplayName("Should handle duplicate bidders")
        void testDuplicateBidders() {
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.addNguoiTraGia(nguoiMua1);

            assertEquals(2, phienDauGia.getDanhSachNguoiTraGia().size());
        }
    }

    @Nested
    @DisplayName("Auction Identification Tests")
    class IdentificationTests {
        @Test
        @DisplayName("Should get and set auction ID (maPhien)")
        void testMaPhien() {
            assertEquals("P001", phienDauGia.getMaPhien());

            phienDauGia.setMaPhien("P999");
            assertEquals("P999", phienDauGia.getMaPhien());
        }

        @Test
        @DisplayName("Should get auction ID with alternative getter")
        void testMaPhienDauGia() {
            String maPhien = phienDauGia.getMaPhienDauGia();
            assertEquals("P001", maPhien);
            assertEquals(phienDauGia.getMaPhien(), maPhien);
        }

        @Test
        @DisplayName("Should get auction name (tenPhien)")
        void testTenPhienDauGia() {
            assertEquals("iPhone 15 Pro Auction", phienDauGia.getTenPhienDauGia());
        }
    }

    @Nested
    @DisplayName("Product and Seller Tests")
    class ProductAndSellerTests {
        @Test
        @DisplayName("Should return auctioned product")
        void testGetProduct() {
            SanPham product = phienDauGia.getSanPham();
            assertNotNull(product);
            assertEquals("iPhone 15 Pro", product.layTenSanPham());
            assertEquals("IPHONE15", product.layMaSanPham());
        }

        @Test
        @DisplayName("Should return seller")
        void testGetSeller() {
            NguoiDung seller = phienDauGia.getNguoiBan();
            assertNotNull(seller);
            assertEquals(nguoiBan, seller);
        }

        @Test
        @DisplayName("Should handle different sellers")
        void testDifferentSellers() {
            PhienDauGia auction2 = new PhienDauGia("P100", "New Auction", sanPham, 2000.0, nguoiBan2, 3600);

            assertEquals(nguoiBan, phienDauGia.getNguoiBan());
            assertEquals(nguoiBan2, auction2.getNguoiBan());
            assertNotEquals(phienDauGia.getNguoiBan(), auction2.getNguoiBan());
        }
    }

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {
        @Test
        @DisplayName("Complete auction session lifecycle")
        void testAuctionSessionLifecycle() {
            // Initial state: DANG_CHO
            assertEquals(TrangThaiPhien.DANG_CHO, phienDauGia.getTrangThai());
            assertEquals(1000.0, phienDauGia.getGiaHienTai());

            // Start auction: DANG_DIEN_RA
            LocalDateTime startTime = LocalDateTime.now();
            phienDauGia.setThoiGianBatDau(startTime);
            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            assertEquals(TrangThaiPhien.DANG_DIEN_RA, phienDauGia.getTrangThai());

            // Add bidders
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.setDaCoGia(true);
            phienDauGia.setGiaHienTai(1500.0);
            assertEquals(1500.0, phienDauGia.getGiaHienTai());
            assertTrue(phienDauGia.getdaCoGia());

            // More bidding
            phienDauGia.addNguoiTraGia(nguoiMua2);
            phienDauGia.setGiaHienTai(2000.0);
            assertEquals(2, phienDauGia.getDanhSachNguoiTraGia().size());

            // End auction: DA_THANH_TOAN
            LocalDateTime endTime = startTime.plusHours(1);
            phienDauGia.setThoiGianKetThuc(endTime);
            phienDauGia.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
            phienDauGia.setNguoiThangCuoc();

            assertEquals(TrangThaiPhien.DA_THANH_TOAN, phienDauGia.getTrangThai());
            assertEquals(nguoiMua2, phienDauGia.getNguoiThangCuoc());
            assertEquals(2000.0, phienDauGia.getGiaHienTai());
        }

        @Test
        @DisplayName("Auction cancellation scenario")
        void testAuctionCancellation() {
            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            phienDauGia.addNguoiTraGia(nguoiMua1);

            // Cancel auction
            phienDauGia.setTrangThai(TrangThaiPhien.DA_HUY);
            phienDauGia.setClosed(true);

            assertEquals(TrangThaiPhien.DA_HUY, phienDauGia.getTrangThai());
            assertTrue(phienDauGia.isClosed());
        }

        @Test
        @DisplayName("Price escalation with multiple bids")
        void testPriceEscalation() {
            double basePrice = phienDauGia.getGiaHienTai();
            int bidCount = 5;

            for (int i = 0; i < bidCount; i++) {
                double newPrice = basePrice + (100.0 * (i + 1));
                phienDauGia.setGiaHienTai(newPrice);
                phienDauGia.addNguoiTraGia(new NguoiDung("User" + i, "email" + i + "@gmail.com", "123456", "01/01/2000"));
            }

            assertEquals(basePrice + (100.0 * bidCount), phienDauGia.getGiaHienTai());
            assertEquals(bidCount, phienDauGia.getDanhSachNguoiTraGia().size());
        }

        @Test
        @DisplayName("Complete bidding history with winner determination")
        void testCompleteBiddingHistory() {
            // Round 1
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.setGiaHienTai(1100.0);
            phienDauGia.setDaCoGia(true);

            // Round 2
            phienDauGia.addNguoiTraGia(nguoiMua2);
            phienDauGia.setGiaHienTai(1250.0);

            // Round 3
            NguoiDung nguoiMua3 = new NguoiDung("AnhDung", "buyer4@gmail.com", "123456", "25/07/1999");
            phienDauGia.addNguoiTraGia(nguoiMua3);
            phienDauGia.setGiaHienTai(1500.0);

            // Determine winner
            phienDauGia.setNguoiThangCuoc();

            assertEquals(3, phienDauGia.getDanhSachNguoiTraGia().size());
            assertEquals(nguoiMua3, phienDauGia.getNguoiThangCuoc());
            assertEquals(1500.0, phienDauGia.getGiaHienTai());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle zero initial price")
        void testZeroInitialPrice() {
            PhienDauGia freeAuction = new PhienDauGia("P000", "Free Item", sanPham, 0.0, nguoiBan, 3600);
            assertEquals(0.0, freeAuction.getGiaHienTai());
        }

        @Test
        @DisplayName("Should handle very high prices")
        void testHighPrice() {
            phienDauGia.setGiaHienTai(999999.99);
            assertEquals(999999.99, phienDauGia.getGiaHienTai());
        }

        @Test
        @DisplayName("Should handle zero duration")
        void testZeroDuration() {
            PhienDauGia auction = new PhienDauGia("P002", "Test", sanPham, 100.0, nguoiBan, 0);
            assertEquals(0, auction.getThoiGian());
        }

        @Test
        @DisplayName("Should handle large bidder lists")
        void testLargeBidderList() {
            for (int i = 0; i < 100; i++) {
                phienDauGia.addNguoiTraGia(
                    new NguoiDung("Buyer" + i, "buyer" + i + "@gmail.com", "123456", "01/01/2000")
                );
            }

            assertEquals(100, phienDauGia.getDanhSachNguoiTraGia().size());
            phienDauGia.setNguoiThangCuoc();
            assertNotNull(phienDauGia.getNguoiThangCuoc());
        }

        @Test
        @DisplayName("Should preserve data integrity after multiple operations")
        void testDataIntegrity() {
            String originalId = phienDauGia.getMaPhien();
            double originalPrice = phienDauGia.getGiaHienTai();
            NguoiDung originalSeller = phienDauGia.getNguoiBan();

            // Perform multiple operations
            phienDauGia.setGiaHienTai(5000.0);
            phienDauGia.addNguoiTraGia(nguoiMua1);
            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);

            // Verify core data hasn't changed
            assertEquals(originalId, phienDauGia.getMaPhien());
            assertEquals(originalSeller, phienDauGia.getNguoiBan());
            // But price should change
            assertEquals(5000.0, phienDauGia.getGiaHienTai());
            assertNotEquals(originalPrice, phienDauGia.getGiaHienTai());
        }
    }
}