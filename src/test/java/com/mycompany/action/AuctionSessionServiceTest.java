package com.mycompany.action;

import com.mycompany.models.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionSessionServiceTest {

    private AuctionSessionService service;
    private AuctionSession auction;
    private User seller;
    private User bidder1;
    private User bidder2;

    @BeforeEach
    void setUp() {
        service = AuctionSessionService.getInstance();

        // Tạo người bán
        seller = new User("Nguoi Ban", "seller@test.com", "pass", "1990-01-01");
        seller.setUserId("SELLER001");
        seller.setAvailableBalance(0);

        // Tạo người đặt giá
        bidder1 = new User("Nguoi Dat 1", "bidder1@test.com", "pass", "1995-01-01");
        bidder1.setUserId("BIDDER001");
        bidder1.setAvailableBalance(10_000_000);

        bidder2 = new User("Nguoi Dat 2", "bidder2@test.com", "pass", "1996-01-01");
        bidder2.setUserId("BIDDER002");
        bidder2.setAvailableBalance(20_000_000);

        // Tạo phiên đấu giá
        Product product = new Product("Laptop", "SP001");
        auction = new AuctionSession("PH001", "Đấu giá Laptop", product, 5_000_000, seller, 300);

        // Chuyển phiên sang IN_PROGRESS thủ công (tránh phụ thuộc scheduler)
        auction.setStatus(SessionStatus.IN_PROGRESS);
        auction.setStartTime(java.time.LocalDateTime.now());
        auction.setEndTime(java.time.LocalDateTime.now().plusSeconds(300));
        auction.setPriceStep(0); // Lần đặt đầu tiên không cần bước giá
    }

    // ===== setPrice() — các trường hợp hợp lệ =====

    @Test
    @DisplayName("setPrice() lần đầu tiên với giá bằng giá khởi điểm phải thành công")
    void setPrice_firstBidAtStartingPriceShouldSucceed() {
        boolean result = service.setPrice(auction, bidder1, 5_000_000);
        assertTrue(result);
        assertEquals(5_000_000, auction.getCurrentPrice());
    }

    @Test
    @DisplayName("setPrice() thành công phải trừ tiền người đặt")
    void setPrice_shouldDeductBidderBalance() {
        double balanceBefore = bidder1.getAvailableBalance();
        service.setPrice(auction, bidder1, 5_000_000);
        assertTrue(bidder1.getAvailableBalance() < balanceBefore,
                "Phải trừ tiền người đặt giá");
    }

    @Test
    @DisplayName("setPrice() thành công phải thêm bidder vào danh sách")
    void setPrice_shouldAddBidderToList() {
        service.setPrice(auction, bidder1, 5_000_000);
        assertTrue(auction.getBidderList().contains(bidder1));
    }

    @Test
    @DisplayName("setPrice() người thứ hai đặt giá cao hơn phải thành công và hoàn tiền người trước")
    void setPrice_secondBidShouldRefundPreviousLeader() {
        // bidder1 đặt trước
        service.setPrice(auction, bidder1, 5_000_000);
        double bidder1BalanceAfterFirstBid = bidder1.getAvailableBalance();

        // bidder2 đặt cao hơn
        double priceStep = auction.getPriceStep();
        boolean result = service.setPrice(auction, bidder2, 5_000_000 + priceStep);

        assertTrue(result, "bidder2 phải đặt thành công");
        // bidder1 phải được hoàn tiền
        assertTrue(bidder1.getAvailableBalance() > bidder1BalanceAfterFirstBid,
                "bidder1 phải được hoàn tiền khi không còn dẫn đầu");
    }

    // ===== setPrice() — các trường hợp thất bại =====

    @Test
    @DisplayName("setPrice() người bán tự đặt giá phải trả về false")
    void setPrice_sellerBiddingOnOwnAuction_shouldReturnFalse() {
        seller.setAvailableBalance(999_999_999);
        boolean result = service.setPrice(auction, seller, 5_000_000);
        assertFalse(result, "Người bán không được tự đặt giá");
    }

    @Test
    @DisplayName("setPrice() phiên chưa bắt đầu (WAITING) phải trả về false")
    void setPrice_auctionNotStarted_shouldReturnFalse() {
        auction.setStatus(SessionStatus.WAITING);
        boolean result = service.setPrice(auction, bidder1, 5_000_000);
        assertFalse(result);
    }

    @Test
    @DisplayName("setPrice() phiên đã kết thúc (PAID) phải trả về false")
    void setPrice_auctionEnded_shouldReturnFalse() {
        auction.setStatus(SessionStatus.PAID);
        boolean result = service.setPrice(auction, bidder1, 5_000_000);
        assertFalse(result);
    }

    @Test
    @DisplayName("setPrice() giá quá thấp (thấp hơn giá hiện tại + bước giá) phải trả về false")
    void setPrice_bidTooLow_shouldReturnFalse() {
        // Đặt giá đầu tiên
        service.setPrice(auction, bidder1, 5_000_000);
        double minNext = auction.getCurrentPrice() + auction.getPriceStep();

        // bidder2 đặt thấp hơn mức tối thiểu
        boolean result = service.setPrice(auction, bidder2, minNext - 1);
        assertFalse(result, "Giá quá thấp phải bị từ chối");
    }

    @Test
    @DisplayName("setPrice() số dư không đủ phải trả về false")
    void setPrice_insufficientBalance_shouldReturnFalse() {
        bidder1.setAvailableBalance(100); // Số dư thấp hơn giá đặt
        boolean result = service.setPrice(auction, bidder1, 5_000_000);
        assertFalse(result);
    }

    // ===== start() =====

    @Test
    @DisplayName("start() chuyển phiên từ WAITING sang IN_PROGRESS")
    void start_shouldChangeStatusToInProgress() {
        Product product = new Product("Item", "SP002");
        AuctionSession newAuction = new AuctionSession(
                "PH002", "Test", product, 1_000_000, seller, 60);
        // newAuction mặc định WAITING
        service.start(newAuction);
        assertEquals(SessionStatus.IN_PROGRESS, newAuction.getStatus());
    }

    @Test
    @DisplayName("start() phải set startTime và endTime")
    void start_shouldSetStartAndEndTime() {
        Product product = new Product("Item", "SP003");
        AuctionSession newAuction = new AuctionSession(
                "PH003", "Test", product, 1_000_000, seller, 120);
        service.start(newAuction);
        assertNotNull(newAuction.getStartTime());
        assertNotNull(newAuction.getEndTime());
    }

    @Test
    @DisplayName("start() phiên đang IN_PROGRESS không được bắt đầu lại")
    void start_alreadyInProgress_shouldNotRestart() {
        java.time.LocalDateTime originalStart = auction.getStartTime();
        service.start(auction); // auction đã IN_PROGRESS
        // startTime không được thay đổi
        assertEquals(originalStart, auction.getStartTime(),
                "Phiên đang chạy không được bắt đầu lại");
    }

    // ===== closeAuction() =====

    @Test
    @DisplayName("closeAuction() không có bid phải chuyển sang CANCELLED")
    void closeAuction_noBids_shouldBeCancelled() {
        service.closeAuction(auction, SessionStatus.PAID);
        assertEquals(SessionStatus.CANCELLED, auction.getStatus(),
                "Không có bid → phải CANCELLED dù yêu cầu PAID");
    }

    @Test
    @DisplayName("closeAuction() có bid phải chuyển sang PAID và set winner")
    void closeAuction_withBids_shouldBePaidWithWinner() {
        service.setPrice(auction, bidder1, 5_000_000);
        service.closeAuction(auction, SessionStatus.PAID);
        assertEquals(SessionStatus.PAID, auction.getStatus());
        assertNotNull(auction.getWinner(), "Phải có người thắng");
    }

    @Test
    @DisplayName("closeAuction() phiên đã PAID không được đóng lại")
    void closeAuction_alreadyClosed_shouldNotChangeStatus() {
        service.setPrice(auction, bidder1, 5_000_000);
        service.closeAuction(auction, SessionStatus.PAID);
        // Gọi lần 2
        service.closeAuction(auction, SessionStatus.CANCELLED);
        assertEquals(SessionStatus.PAID, auction.getStatus(),
                "Phiên đã đóng không được thay đổi trạng thái");
    }
}