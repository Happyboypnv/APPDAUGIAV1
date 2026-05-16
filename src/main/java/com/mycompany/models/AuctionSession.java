package com.mycompany.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuctionSession {

    private String sessionId;
    private String sessionName;
    private double currentPrice;
    private double priceStep;
    private static final double MIN_PRICE_DIFF_RATIO = 0.06;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;
    private final List<User> bidderList = Collections.synchronizedList(new ArrayList<>());
    private User seller;
    private User winner;
    private Product product;
    private boolean hasBid = false;
    private volatile SessionStatus status;
    private boolean isClosed = false;

    public AuctionSession(String sessionId, String sessionName, Product product,
            double startingPrice, User seller, int duration) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.product = product;
        this.currentPrice = startingPrice;
        this.seller = seller;
        this.duration = duration;
        priceStep = 0.0;
        this.status = SessionStatus.WAITING;
    }

    public AuctionSession(String sessionName, Product product, double startingPrice,
            User seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.sessionName = sessionName;
        this.product = product;
        this.currentPrice = startingPrice;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Overloaded constructor for use with database (without duration).
     *
     * @param sessionId    the session identifier
     * @param sessionName  the name of the session
     * @param product      the product being auctioned
     * @param startingPrice the starting price
     * @param seller       the seller
     */
    public AuctionSession(String sessionId, String sessionName, Product product,
            double startingPrice, User seller) {
        this(sessionId, sessionName, product, startingPrice, seller, 0);
    }

    public void addBidder(User bidder) {
        synchronized (bidderList) {
            bidderList.add(bidder);
        }
    }

    // ===== SETTERS =====

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public void setWinner() {
        if (!bidderList.isEmpty()) {
            winner = bidderList.get(bidderList.size() - 1);
        }
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }

    public void setPriceStep(double priceStep) {
        this.priceStep = priceStep;
    }

    public void setHasBid(boolean hasBid) {
        this.hasBid = hasBid;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // ===== GETTERS =====

    public SessionStatus getStatus() {
        return this.status;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public double getPriceStep() {
        return this.priceStep;
    }

    public Product getProduct() {
        return this.product;
    }

    public User getWinner() {
        return this.winner;
    }

    public User getSeller() {
        return this.seller;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getAuctionSessionId() {
        return this.sessionId;
    }

    public String getSessionName() {
        return this.sessionName;
    }

    public boolean isHasBid() {
        return hasBid;
    }

    public double getMinPriceDiffRatio() {
        return MIN_PRICE_DIFF_RATIO;
    }

    public List<User> getBidderList() {
        synchronized (bidderList) {
            return new ArrayList<>(bidderList);
        }
    }

    public int getDuration() {
        return duration;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }
}
