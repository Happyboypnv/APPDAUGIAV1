package com.mycompany.models;
import java.io.*;
import java.util.*;

public class Seller {
    private User user;

    public Seller(User user) {
        this.user = user;
    }

    public void startRoom(AuctionRoom room) {
        room.startSession(this);
    }


}