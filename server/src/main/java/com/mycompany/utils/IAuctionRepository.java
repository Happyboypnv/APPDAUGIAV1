package com.mycompany.utils;

import com.mycompany.models.AuctionSession;

import java.sql.SQLException;
import java.util.Map;

public interface IAuctionRepository {
    void save(AuctionSession AuctionSession);
    AuctionSession findById(String maPhien);
    Map<String, AuctionSession> findAll() throws SQLException;
    boolean update(AuctionSession AuctionSession);
    boolean delete(String maPhien);
    boolean isAuctionAvailable(String maPhien);
}