package com.mycompany.models;
import java.util.*;
import java.io.*;

public class AuctionRoom implements Serializable{
    private static final long serializeVersionUID = 1L;
    private String id,name;
    private String currentStage; // trạng thái hiện tại (đang chờ, đang diễn ra, đã kết thúc...)
    private HashMap<String,Bidder> listBidders;
    private Seller seller;
    private Product product;
    private final double startingPrice;
    private double currentHighestBid;
    private final double increment;
    private final int maxBidder;
    private final int minBidder; // de xuat cho so luong nguoi max va min. >= min moi duoc start, == max => khong cho them nguoi nua

    // timer tim hieu sau

    // tat ca cac thuoc tinh se dc set boi user khi dang tao phien giao dich
    public AuctionRoom(String id, String name, Seller seller, Product product, double startingPrice, int maxBidder, int minBidder) {
        this.name = name;
        this.id = id;
        this.seller = seller;
        this.product = product;
        this.startingPrice = startingPrice;
        this.increment = 0.06 * startingPrice;
        this.maxBidder = maxBidder;
        this.minBidder = minBidder;
        this.currentStage = "PENDING";
    }
// Cac phuong thuc cua bidder
    protected void registerBidder(Bidder bidder) {
        // chi co bidder moi join duoc phong, seller o trong phong lam host san roi => k join phong khac hay phong chinh minh duoc nua)
        // Sau nay them cac dieu kien check va throw loi (vi du o day neu da o trong phong / phong chua mo / du so nguoi choi (maxBidder) thi throw loi va k goi ham nua)
        // Trạng thái (danh sách) chỉ được thay đổi TẠI ĐÂY
        listBidders.put(bidder.getUser().getName(), bidder);
        // de ten nguoi dung la key
    }

    protected void processNewBid(Bidder bidder, double amount) {
        // xu ly loi: phien da ket thuc, chua join phong, gia bid k dung

        // Trạng thái (giá cao nhất) chỉ được thay đổi TẠI ĐÂY
        this.currentHighestBid = amount;
    }

    protected void removeBidder(String bidderName) {
        // xu ly loi: k tim thay ten, k phai bidder...
        listBidders.remove(bidderName);
    }


    // Cac phuong thuc cua seller

    protected void startSession(Seller seller){
        // xu ly cac loi: ko phai seller, ko trong phong, phong chua du nguoi...
        this.currentStage = "RUNNING";
    }

    // ket thuc phien + tao giao dich tu dong
}
