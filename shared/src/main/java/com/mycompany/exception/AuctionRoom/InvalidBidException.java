package com.mycompany.exception.AuctionRoom;

public class InvalidBidException extends Exception {
    public InvalidBidException(String msg) {
        super(msg);
    }
}