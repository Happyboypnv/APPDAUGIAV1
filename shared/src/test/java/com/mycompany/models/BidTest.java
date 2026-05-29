package com.mycompany.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Bid model.
 *
 * Covers: creation, field correctness, UUID generation, custom constructor.
 */
@DisplayName("Bid model tests")
class BidTest {

    @Test
    @DisplayName("Bid tự sinh bidId (UUID) khi dùng constructor 3 tham số")
    void bid_autoGeneratesBidId() {
        Bid bid = new Bid("session-01", "user-01", 1_500_000.0);
        assertNotNull(bid.getBidId());
        assertFalse(bid.getBidId().isEmpty());
    }

    @Test
    @DisplayName("Hai Bid khác nhau phải có bidId khác nhau")
    void bid_uniqueBidIds() {
        Bid bid1 = new Bid("session-01", "user-01", 1_000_000.0);
        Bid bid2 = new Bid("session-01", "user-02", 1_500_000.0);
        assertNotEquals(bid1.getBidId(), bid2.getBidId());
    }

    @Test
    @DisplayName("Bid lưu đúng sessionId")
    void bid_correctSessionId() {
        Bid bid = new Bid("s-XYZ", "u-001", 2_000_000.0);
        assertEquals("s-XYZ", bid.getSessionId());
    }

    @Test
    @DisplayName("Bid lưu đúng userId")
    void bid_correctUserId() {
        Bid bid = new Bid("session-01", "user-ABC", 2_000_000.0);
        assertEquals("user-ABC", bid.getUserId());
    }

    @Test
    @DisplayName("Bid lưu đúng amount")
    void bid_correctAmount() {
        Bid bid = new Bid("session-01", "user-01", 3_500_000.0);
        assertEquals(3_500_000.0, bid.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Bid tự set bidTime gần với thời điểm hiện tại (trong vòng 5 giây)")
    void bid_bidTimeIsRecent() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Bid bid = new Bid("session-01", "user-01", 1_000_000.0);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(bid.getBidTime().isAfter(before) && bid.getBidTime().isBefore(after));
    }

    @Test
    @DisplayName("Constructor đầy đủ 5 tham số lưu đúng tất cả trường")
    void bid_fullConstructor_correctFields() {
        LocalDateTime ts = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        Bid bid = new Bid("bid-001", "session-01", "user-01", 4_000_000.0, ts);
        assertAll(
            () -> assertEquals("bid-001", bid.getBidId()),
            () -> assertEquals("session-01", bid.getSessionId()),
            () -> assertEquals("user-01", bid.getUserId()),
            () -> assertEquals(4_000_000.0, bid.getAmount(), 0.001),
            () -> assertEquals(ts, bid.getBidTime())
        );
    }

    @Test
    @DisplayName("Amount = 0 vẫn được lưu (validation ở tầng service)")
    void bid_zeroAmount_isStored() {
        Bid bid = new Bid("session-01", "user-01", 0.0);
        assertEquals(0.0, bid.getAmount(), 0.001);
    }
}
