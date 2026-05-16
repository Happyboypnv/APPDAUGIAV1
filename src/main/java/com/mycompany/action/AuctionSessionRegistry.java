package com.mycompany.action;

import com.mycompany.models.AuctionSession;

import java.util.*;
import java.util.concurrent.*;

public class AuctionSessionRegistry {
    // Singleton
    // volatile đảm bảo các thread đọc giá trị mới nhất
    private static volatile AuctionSessionRegistry instance;

    public static AuctionSessionRegistry getInstance() {
        if (instance == null) {                          // kiểm tra lần 1 (không lock)
            synchronized (AuctionSessionRegistry.class) {
                if (instance == null) {                  // kiểm tra lần 2 (có lock)
                    instance = new AuctionSessionRegistry();
                }
            }
        }
        return instance;
    }

    private AuctionSessionRegistry() {
    }

    private final Map<String, AuctionSession> listAuction = new ConcurrentHashMap<>();

    public void them(AuctionSession phien) {
        listAuction.putIfAbsent(phien.getSessionId(), phien);
    }

    public void xoa(AuctionSession phien) {
        listAuction.remove(phien.getSessionId());
    }

    public void xoa(String maPhien) {
        listAuction.remove(maPhien);
    }

    public AuctionSession tim(String maPhien) {
        return listAuction.get(maPhien);
    }
}