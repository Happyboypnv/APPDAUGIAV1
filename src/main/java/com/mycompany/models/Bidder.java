package com.mycompany.models;
import java.io.*;
import java.util.*;
public class Bidder implements Serializable {
    private User user;

    public Bidder(User user) {
        this.user = user;
    }

    // Bidder thực hiện hành động tham gia phòng
    public void joinRoom(AuctionRoom room) {
        // Gọi ủy quyền sang phòng để phòng tự quyết định có cho vào không
        room.registerBidder(this);
    }

    // Bidder thực hiện hành động đặt giá
    public void placeBid(AuctionRoom room, double amount) {
        // Ủy quyền sang phòng để xử lý logic so sánh giá
        room.processNewBid(this, amount);
    }

    public void quitRoom(AuctionRoom room) {
        room.removeBidder(this.getUser().getName());
    }

    public User getUser() {
        return user;
    }
}