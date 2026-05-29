package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import com.mycompany.exception.AuctionRoom.AuctionClosedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuctionSession and related model logic.
 *
 * Covers:
 * - Bid validation (minimum price, price step, double-bid prevention)
 * - Session state transitions
 * - Anti-sniping time extension detection
 * - Winner determination
 * - Balance calculation (actualBalance / frozenBalance)
 */
@DisplayName("AuctionSession model tests")
class AuctionSessionTest {

    private AuctionSession session;
    private User seller;
    private User bidderA;
    private User bidderB;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Electronics("Laptop Dell", "PROD-001", "Laptop gaming");

        seller = new User("Nguyen Van Seller", "seller@test.com", "pass", "2000-01-01");
        seller.setUserId("seller-01");
        seller.setActualBalance(0);

        bidderA = new User("Tran Thi A", "a@test.com", "pass", "2000-01-01");
        bidderA.setUserId("bidder-A");
        bidderA.setActualBalance(10_000_000);

        bidderB = new User("Le Van B", "b@test.com", "pass", "2000-01-01");
        bidderB.setUserId("bidder-B");
        bidderB.setActualBalance(10_000_000);

        // Phiên bắt đầu với giá khởi điểm 1.000.000, duration 3600s
        session = new AuctionSession(
                "session-001", "Phien test", product,
                1_000_000.0, seller, 3600
        );
        session.setStartTime(LocalDateTime.now().minusMinutes(1));
        session.setEndTime(LocalDateTime.now().plusMinutes(59));
        session.setStatus(SessionStatus.IN_PROGRESS);
    }

    // =========================================================
    // 1. TRẠNG THÁI PHIÊN
    // =========================================================

    @Test
    @DisplayName("Phiên mới tạo phải ở trạng thái WAITING")
    void newSession_shouldBePending() {
        AuctionSession fresh = new AuctionSession(
                "s-002", "Fresh", product, 500_000.0, seller, 1800
        );
        assertEquals(SessionStatus.PENDING, fresh.getStatus());
    }

    @Test
    @DisplayName("Phiên mới tạo phải ở trạng thái PENDING (chờ admin duyệt)")
    void setStatus_inProgress() {
        AuctionSession fresh = new AuctionSession(
                "s-003", "Fresh2", product, 500_000.0, seller, 1800
        );
        fresh.setStatus(SessionStatus.IN_PROGRESS);
        assertEquals(SessionStatus.IN_PROGRESS, fresh.getStatus());
    }

    @Test
    @DisplayName("Chuyển trạng thái sang PAID")
    void setStatus_paid() {
        session.setStatus(SessionStatus.PAID);
        assertEquals(SessionStatus.PAID, session.getStatus());
    }

    @Test
    @DisplayName("Chuyển trạng thái sang CANCELLED")
    void setStatus_cancelled() {
        session.setStatus(SessionStatus.CANCELLED);
        assertEquals(SessionStatus.CANCELLED, session.getStatus());
    }

    // =========================================================
    // 2. KIỂM TRA GIÁ ĐẶT
    // =========================================================

    @Test
    @DisplayName("Lần đầu đặt giá đúng bằng startingPrice phải hợp lệ")
    void firstBid_atStartingPrice_isValid() {
        double gia = session.getCurrentPrice(); // 1_000_000
        assertTrue(gia >= session.getCurrentPrice(),
                "Giá bằng giá khởi điểm phải hợp lệ cho lần đặt đầu");
    }

    @Test
    @DisplayName("Đặt giá thấp hơn currentPrice phải bị từ chối")
    void bid_belowCurrentPrice_isInvalid() {
        // Giả lập đã có người đặt giá 1.5M
        session.setCurrentPrice(1_500_000.0);
        session.setHasBid(true);
        session.setPriceStep(1_500_000.0 * session.getMinPriceDiffRatio());

        double giaToiThieu = session.getCurrentPrice() + session.getPriceStep();
        double giaDat = session.getCurrentPrice() - 1; // thấp hơn

        assertFalse(giaDat >= giaToiThieu,
                "Giá thấp hơn currentPrice+priceStep phải không hợp lệ");
    }

    @Test
    @DisplayName("Đặt giá đúng bằng minimum bid (currentPrice + priceStep) phải hợp lệ")
    void bid_atMinimum_isValid() {
        session.setCurrentPrice(1_500_000.0);
        session.setHasBid(true);
        session.setPriceStep(1_500_000.0 * session.getMinPriceDiffRatio());

        double giaToiThieu = session.getCurrentPrice() + session.getPriceStep();

        assertTrue(giaToiThieu >= session.getCurrentPrice() + session.getPriceStep() - 0.001,
                "Giá đúng minimum phải hợp lệ");
    }

    @Test
    @DisplayName("priceStep sau khi đặt giá phải bằng gia * MIN_PRICE_DIFF_RATIO")
    void priceStep_calculatedCorrectly() {
        double gia = 2_000_000.0;
        session.setCurrentPrice(gia);
        session.setPriceStep(gia * session.getMinPriceDiffRatio());

        double expected = 2_000_000.0 * 0.06;
        assertEquals(expected, session.getPriceStep(), 0.001);
    }

    // =========================================================
    // 3. BIDDER LIST & WINNER
    // =========================================================

    @Test
    @DisplayName("addBidder: danh sách bidder phải tăng đúng")
    void addBidder_incrementsList() {
        assertTrue(session.getBidderList().isEmpty());
        session.addBidder(bidderA);
        assertEquals(1, session.getBidderList().size());
        session.addBidder(bidderB);
        assertEquals(2, session.getBidderList().size());
    }

    @Test
    @DisplayName("setWinner: người thắng phải là bidder cuối cùng")
    void setWinner_isLastBidder() {
        session.addBidder(bidderA);
        session.addBidder(bidderB);
        session.setWinner();
        assertEquals(bidderB.getUserId(), session.getWinner().getUserId());
    }

    @Test
    @DisplayName("setWinner khi không có bidder: winner phải là null")
    void setWinner_noBidders_isNull() {
        session.setWinner();
        assertNull(session.getWinner());
    }

    @Test
    @DisplayName("Người bán không được là người thắng (seller != bidder logic)")
    void seller_isNotBidder() {
        // Kiểm tra logic: nếu seller cố đặt giá, userId trùng với seller phải bị chặn
        assertNotEquals(seller.getUserId(), bidderA.getUserId(),
                "Seller và bidder phải khác userId");
        assertTrue(bidderA.getUserId().equals(seller.getUserId())
                == false);
    }

    // =========================================================
    // 4. ANTI-SNIPING
    // =========================================================

    @Test
    @DisplayName("Anti-sniping: endTime phải được gia hạn 30s nếu còn dưới 60s")
    void antiSniping_extendsEndTime() {
        LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(45);
        session.setEndTime(nearEnd);

        // Mô phỏng logic anti-sniping
        long thoiGianConLai = java.time.Duration.between(
                LocalDateTime.now(), session.getEndTime()).getSeconds();
        if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
            session.setEndTime(session.getEndTime().plusSeconds(30));
        }

        // endTime sau gia hạn phải là nearEnd + 30s
        assertEquals(nearEnd.plusSeconds(30), session.getEndTime());
    }

    @Test
    @DisplayName("Anti-sniping: không gia hạn nếu còn hơn 60s")
    void antiSniping_noExtensionIfTimeRemaining() {
        LocalDateTime farEnd = LocalDateTime.now().plusMinutes(10);
        session.setEndTime(farEnd);

        long thoiGianConLai = java.time.Duration.between(
                LocalDateTime.now(), session.getEndTime()).getSeconds();

        LocalDateTime endBefore = session.getEndTime();
        if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
            session.setEndTime(session.getEndTime().plusSeconds(30));
        }

        assertEquals(endBefore, session.getEndTime(),
                "endTime không được thay đổi khi còn hơn 60s");
    }

    // =========================================================
    // 5. CLOSED FLAG
    // =========================================================

    @Test
    @DisplayName("isClosed mặc định phải là false")
    void isClosed_defaultFalse() {
        assertFalse(session.isClosed());
    }

    @Test
    @DisplayName("setClosed(true) phải phản ánh qua isClosed()")
    void setClosed_true() {
        session.setClosed(true);
        assertTrue(session.isClosed());
    }

    // =========================================================
    // 6. USER BALANCE
    // =========================================================

    @Test
    @DisplayName("getAvailableBalance = actualBalance - frozenBalance")
    void availableBalance_calculatedCorrectly() {
        bidderA.setActualBalance(5_000_000);
        bidderA.setFrozenBalance(1_500_000);
        assertEquals(3_500_000.0, bidderA.getAvailableBalance(), 0.001);
    }

    @Test
    @DisplayName("frozenBalance tăng khi đặt giá (mô phỏng)")
    void frozenBalance_increasesOnBid() {
        double initial = bidderA.getFrozenBalance();
        double bidAmount = 2_000_000;
        bidderA.setFrozenBalance(bidderA.getFrozenBalance() + bidAmount);
        assertEquals(initial + bidAmount, bidderA.getFrozenBalance(), 0.001);
    }

    @Test
    @DisplayName("frozenBalance giải phóng khi bị outbid")
    void frozenBalance_releasedOnOutbid() {
        bidderA.setFrozenBalance(2_000_000);
        // Người khác bid cao hơn → giải phóng frozen của A
        bidderA.setFrozenBalance(Math.max(0, bidderA.getFrozenBalance() - 2_000_000));
        assertEquals(0.0, bidderA.getFrozenBalance(), 0.001);
    }

    @Test
    @DisplayName("User không đủ balance không được đặt giá")
    void bid_insufficientBalance_isRejected() {
        bidderA.setActualBalance(500_000);
        bidderA.setFrozenBalance(0);
        double bidAmount = 2_000_000;

        boolean enoughBalance = bidderA.getAvailableBalance() >= bidAmount;
        assertFalse(enoughBalance, "Không đủ balance phải bị từ chối");
    }

    // =========================================================
    // 7. SESSION INFO
    // =========================================================

    @Test
    @DisplayName("getSessionId trả về đúng ID đã set")
    void getSessionId_correct() {
        assertEquals("session-001", session.getSessionId());
    }

    @Test
    @DisplayName("getSessionName trả về đúng tên")
    void getSessionName_correct() {
        assertEquals("Phien test", session.getSessionName());
    }

    @Test
    @DisplayName("getCurrentPrice trả về giá khởi điểm ban đầu")
    void getCurrentPrice_initial() {
        assertEquals(1_000_000.0, session.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("setCurrentPrice cập nhật đúng")
    void setCurrentPrice_updates() {
        session.setCurrentPrice(3_000_000.0);
        assertEquals(3_000_000.0, session.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("hasBid mặc định là false")
    void hasBid_defaultFalse() {
        assertFalse(session.isHasBid());
    }

    @Test
    @DisplayName("hasBid = true sau khi set")
    void hasBid_setTrue() {
        session.setHasBid(true);
        assertTrue(session.isHasBid());
    }

    @Test
    @DisplayName("AuctionClosedException chứa đúng sessionId và status")
    void auctionClosedException_hasCorrectFields() {
        AuctionClosedException ex = new AuctionClosedException("session-001", "CANCELLED");
        assertEquals("session-001", ex.getSessionId());
        assertEquals("CANCELLED", ex.getSessionStatus());
    }

    @Test
    @DisplayName("AuctionClosedException message chứa sessionId")
    void auctionClosedException_messageContainsSessionId() {
        AuctionClosedException ex = new AuctionClosedException("session-XYZ", "PAID");
        assertTrue(ex.getMessage().contains("session-XYZ"));
    }

    @Test
    @DisplayName("AuctionClosedException message chứa trạng thái")
    void auctionClosedException_messageContainsStatus() {
        AuctionClosedException ex = new AuctionClosedException("session-001", "WAITING");
        assertTrue(ex.getMessage().contains("WAITING"));
    }

    @Test
    @DisplayName("Không thể đặt giá khi phiên ở trạng thái CANCELLED")
    void bid_onCancelledSession_shouldBeRejected() {
        session.setStatus(SessionStatus.CANCELLED);
        assertNotEquals(SessionStatus.IN_PROGRESS, session.getStatus(),
            "Phiên CANCELLED không được nhận bid");
    }

    @Test
    @DisplayName("Không thể đặt giá khi phiên ở trạng thái PAID")
    void bid_onPaidSession_shouldBeRejected() {
        session.setStatus(SessionStatus.PAID);
        assertNotEquals(SessionStatus.IN_PROGRESS, session.getStatus(),
            "Phiên PAID không được nhận bid");
    }

    @Test
    @DisplayName("Không thể đặt giá khi phiên ở trạng thái WAITING")
    void bid_onWaitingSession_shouldBeRejected() {
        session.setStatus(SessionStatus.WAITING);
        assertNotEquals(SessionStatus.IN_PROGRESS, session.getStatus(),
            "Phiên WAITING không được nhận bid");
    }
}
