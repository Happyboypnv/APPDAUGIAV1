package com.mycompany.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Transaction model.
 *
 * Covers: initial status, status change, createdAt, toString correctness.
 */
@DisplayName("Transaction model tests")
class TransactionTest {

    private AuctionSession session;
    private User seller;
    private User winner;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Electronics("Laptop Dell", "PROD-001", "Laptop gaming");

        seller = new User("Nguyen Van Seller", "seller@test.com", "pass", "2000-01-01");
        seller.setUserId("seller-01");

        winner = new User("Tran Thi Winner", "winner@test.com", "pass", "2000-01-01");
        winner.setUserId("winner-01");

        session = new AuctionSession(
                "session-001", "Phien test", product,
                1_000_000.0, seller, 3600
        );
        session.setStatus(SessionStatus.FINISHED);
        session.addBidder(winner);
        session.setWinner();
        session.setCurrentPrice(2_500_000.0);
    }

    @Test
    @DisplayName("Transaction mới tạo có status PAID mặc định")
    void transaction_defaultStatusIsPaid() {
        Transaction tx = new Transaction("TX-001", session);
        assertEquals(TransactionStatus.PAID, tx.getStatus());
    }

    @Test
    @DisplayName("Transaction lưu đúng id")
    void transaction_correctId() {
        Transaction tx = new Transaction("TX-001", session);
        assertEquals("TX-001", tx.getId());
    }

    @Test
    @DisplayName("Transaction liên kết đúng auctionSession")
    void transaction_correctAuctionSession() {
        Transaction tx = new Transaction("TX-001", session);
        assertEquals(session, tx.getAuctionSession());
    }

    @Test
    @DisplayName("setStatus(REFUNDED) cập nhật đúng")
    void transaction_setStatusRefunded() {
        Transaction tx = new Transaction("TX-001", session);
        tx.setStatus(TransactionStatus.REFUNDED);
        assertEquals(TransactionStatus.REFUNDED, tx.getStatus());
    }

    @Test
    @DisplayName("createdAt gần với thời điểm tạo (trong vòng 5 giây)")
    void transaction_createdAtIsRecent() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Transaction tx = new Transaction("TX-001", session);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(tx.getCreatedAt().isAfter(before) && tx.getCreatedAt().isBefore(after));
    }

    @Test
    @DisplayName("Constructor với createdAt tùy chỉnh lưu đúng timestamp")
    void transaction_customCreatedAt() {
        LocalDateTime custom = LocalDateTime.of(2026, 1, 15, 9, 30, 0);
        Transaction tx = new Transaction("TX-002", session, custom);
        assertEquals(custom, tx.getCreatedAt());
    }

    @Test
    @DisplayName("toString() chứa ID giao dịch")
    void transaction_toString_containsId() {
        Transaction tx = new Transaction("TX-SPECIAL", session);
        assertTrue(tx.toString().contains("TX-SPECIAL"));
    }

    @Test
    @DisplayName("toString() chứa tên sản phẩm")
    void transaction_toString_containsProductName() {
        Transaction tx = new Transaction("TX-001", session);
        assertTrue(tx.toString().contains("Laptop Dell"));
    }

    @Test
    @DisplayName("toString() chứa tên người thắng")
    void transaction_toString_containsWinnerName() {
        Transaction tx = new Transaction("TX-001", session);
        assertTrue(tx.toString().contains("Tran Thi Winner"));
    }

    @Test
    @DisplayName("Transaction với null createdAt vẫn không throw NullPointerException")
    void transaction_nullCreatedAt_fallsBackToNow() {
        assertDoesNotThrow(() -> {
            Transaction tx = new Transaction("TX-003", session, null);
            assertNotNull(tx.getCreatedAt());
        });
    }
}
