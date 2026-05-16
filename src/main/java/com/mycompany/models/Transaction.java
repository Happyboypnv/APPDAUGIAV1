package com.mycompany.models;

import java.time.LocalDateTime;

/**
 * Represents a transaction created AFTER an auction session ends.
 *
 * A Transaction contains:
 *  - id           : unique identifier of the transaction
 *  - auctionSession: the ended auction session, containing seller, buyer, product, and final price
 *  - status       : current status of the transaction (PENDING_PAYMENT, PAID, REFUNDED)
 *  - createdAt    : timestamp when the transaction was created
 */
public class Transaction {

    /**
     * Unique identifier of the transaction (e.g. "TX000001").
     */
    private final String id;

    /**
     * The related auction session — contains seller, buyer, product, and final price.
     */
    private final AuctionSession auctionSession;

    /**
     * Current status of the transaction.
     */
    public volatile TransactionStatus status;

    /**
     * Timestamp when the transaction was created (when the auction session ended).
     */
    private final LocalDateTime createdAt;

    // ===== CONSTRUCTOR =====

    /**
     * Creates a new transaction from a recently ended auction session.
     *
     * @param id             unique transaction identifier
     * @param auctionSession the auction session that has ended
     */
    public Transaction(String id, AuctionSession auctionSession) {
        this.id = id;
        this.auctionSession = auctionSession;
        this.status = TransactionStatus.PENDING_PAYMENT;
        this.createdAt = LocalDateTime.now();
    }

    // ===== GETTERS =====

    public String getId() {
        return id;
    }

    public AuctionSession getAuctionSession() {
        return auctionSession;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ===== SETTER =====

    /**
     * Updates the transaction status.
     * Used when confirming payment or issuing a refund.
     *
     * @param status the new status to set
     */
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    // ===== DISPLAY =====

    /**
     * Returns a formatted string describing the transaction details.
     */
    @Override
    public String toString() {
        String buyerName = (auctionSession.getWinner() != null)
                ? auctionSession.getWinner().getFullName()
                : "None";

        return String.format(
                "--- TRANSACTION INFO [%s] ---\n"
                + "| Product       : %-25s |\n"
                + "| Seller        : %-25s |\n"
                + "| Buyer         : %-25s |\n"
                + "| Final Price   : %,22.0f VND |\n"
                + "| Status        : %-25s |\n"
                + "| Created At    : %-25s |\n"
                + "------------------------------------------",
                id,
                auctionSession.getProduct().getProductName(),
                auctionSession.getSeller().getFullName(),
                buyerName,
                auctionSession.getCurrentPrice(),
                status.name(),
                createdAt.toString()
        );
    }
}
