package com.mycompany.exception.AuctionRoom;

/**
 * Thrown when a bid is attempted on an auction session
 * that is not in IN_PROGRESS state (e.g. WAITING, PAID, CANCELLED).
 */
public class AuctionClosedException extends Exception {
    private final String sessionId;
    private final String sessionStatus;

    public AuctionClosedException(String sessionId, String sessionStatus) {
        super("Phiên đấu giá '" + sessionId + "' đã đóng hoặc chưa mở. Trạng thái: " + sessionStatus);
        this.sessionId = sessionId;
        this.sessionStatus = sessionStatus;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }
}
