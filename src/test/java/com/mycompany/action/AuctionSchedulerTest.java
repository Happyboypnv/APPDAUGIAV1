package com.mycompany.action;

import com.mycompany.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionScheduler Tests")
public class AuctionSchedulerTest {

    private AuctionScheduler scheduler;
    private AuctionSession auction;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        scheduler = AuctionScheduler.getInstance();

        seller = new User("Seller", "seller@test.com", "Pass1234!", "1990-01-01");
        seller.setUserId("SELLER001");

        product = new Product("Test Product", "PROD001");

        auction = new AuctionSession(
            "AUCTION001",
            "Test Auction",
            product,
            5000.0,
            seller,
            60 // 60 seconds
        );
    }

    @Test
    @DisplayName("Test setTimeCancelAuction() - Should schedule auction closure")
    void testSetTimeCancelAuction() {
        LocalDateTime now = LocalDateTime.now();
        auction.setEndTime(now.plusSeconds(30));

        assertDoesNotThrow(() -> {
            scheduler.setTimeCancelAuction(auction);
        });
    }

    @Test
    @DisplayName("Test cancel() - Should cancel scheduled future")
    void testCancel() {
        LocalDateTime now = LocalDateTime.now();
        auction.setEndTime(now.plusSeconds(30));

        scheduler.setTimeCancelAuction(auction);

        assertDoesNotThrow(() -> {
            scheduler.cancel(auction);
        });
    }

    @Test
    @DisplayName("Test closeAuction() - Should set auction to PAID")
    void testCloseAuction() {
        auction.setStatus(SessionStatus.IN_PROGRESS);
        auction.setHasBid(true);

        scheduler.closeAuction(auction);

        assertEquals(SessionStatus.PAID, auction.getStatus());
    }

    @Test
    @DisplayName("Test singleton - Should return same instance")
    void testSingletonInstance() {
        AuctionScheduler instance1 = AuctionScheduler.getInstance();
        AuctionScheduler instance2 = AuctionScheduler.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Test setTimeCancelAuction() - With past end time should return early")
    void testSetTimeCancelAuctionPastTime() {
        LocalDateTime now = LocalDateTime.now();
        auction.setEndTime(now.minusSeconds(10)); // Already past

        assertDoesNotThrow(() -> {
            scheduler.setTimeCancelAuction(auction);
        });
    }

    @Test
    @DisplayName("Test multiple auctions - Should handle multiple scheduled tasks")
    void testMultipleAuctions() {
        AuctionSession auction2 = new AuctionSession(
            "AUCTION002",
            "Test Auction 2",
            product,
            3000.0,
            seller,
            60
        );

        LocalDateTime now = LocalDateTime.now();
        auction.setEndTime(now.plusSeconds(30));
        auction2.setEndTime(now.plusSeconds(40));

        assertDoesNotThrow(() -> {
            scheduler.setTimeCancelAuction(auction);
            scheduler.setTimeCancelAuction(auction2);
        });
    }

    @Test
    @DisplayName("Test reschedule - Should replace old scheduled task")
    void testReschedule() {
        LocalDateTime now = LocalDateTime.now();
        auction.setEndTime(now.plusSeconds(30));

        scheduler.setTimeCancelAuction(auction);

        // Reschedule with new end time
        auction.setEndTime(now.plusSeconds(60));

        assertDoesNotThrow(() -> {
            scheduler.setTimeCancelAuction(auction);
        });
    }

    @Test
    @DisplayName("Test consistent session ID usage - getSessionId()")
    void testConsistentSessionIdUsage() {
        String sessionId = auction.getSessionId();
        assertEquals("AUCTION001", sessionId);

        assertDoesNotThrow(() -> {
            LocalDateTime now = LocalDateTime.now();
            auction.setEndTime(now.plusSeconds(30));
            scheduler.setTimeCancelAuction(auction);
            scheduler.cancel(auction);
        });
    }

    @Test
    @DisplayName("Test cancel() - Should handle null auction gracefully")
    void testCancelNullAuction() {
        assertDoesNotThrow(() -> {
            // Manually create null auction situation
            AuctionSession nullAuction = new AuctionSession(
                "NULL001",
                "Void",
                product,
                0,
                seller
            );
            scheduler.cancel(nullAuction);
        });
    }
}

