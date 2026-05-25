package com.mycompany.utils;

import com.mycompany.models.Bid;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidRepositorySQLite {
    private static final String DB_URL = "jdbc:sqlite:hipiti.db";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean insertBid(Bid bid) throws SQLException {
        String sql = "INSERT INTO bids (bid_id, session_id, user_id, amount, bid_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bid.getBidId());
            pstmt.setString(2, bid.getSessionId());
            pstmt.setString(3, bid.getUserId());
            pstmt.setDouble(4, bid.getAmount());
            pstmt.setString(5, bid.getBidTime().format(formatter));
            pstmt.executeUpdate();
            return true;
        }
    }

    public boolean existsByBidId(String bidId) throws SQLException {
        String sql = "SELECT 1 FROM bids WHERE bid_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bidId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
}

