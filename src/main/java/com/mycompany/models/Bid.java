package com.mycompany.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bid {
    private String bidId;
    private String sessionId;
    private String userId;
    private double amount;
    private LocalDateTime bidTime;

    public Bid(String sessionId, String userId, double amount) {
        this.bidId = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.userId = userId;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
    }

    public Bid(String bidId, String sessionId, String userId, double amount, LocalDateTime bidTime) {
        this.bidId = bidId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public String getBidId() {
        return bidId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
}

