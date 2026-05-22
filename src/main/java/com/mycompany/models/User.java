package com.mycompany.models;

import java.util.ArrayList;
import java.util.List;

public class User extends Person implements CanBeSold, CanLeaveRoom, CanBid {

    private String address;
    private String phoneNumber;
    private double availableBalance;
    private List<Transaction> transactions;
    private String bankAccountNumber;
    private String bankName;

    public User(String fullName, String email,
                String password, String dateOfBirth,
                String address, String phoneNumber) {
        super(fullName, email, password, dateOfBirth,0);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.availableBalance = 0;
        this.transactions = new ArrayList<>();
    }

    public User(String fullName, String email, String password, String dateOfBirth) {
        super(fullName, email, password, dateOfBirth, 0);
        this.transactions = new ArrayList<>();
        this.availableBalance = 0;
    }

    public List<Transaction> getTransactions() {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        this.transactions.add(transaction);
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public void buy(Product product) {
        System.out.println("[BUY] User " + this.toString().split("\n")[1]
                + " is placing a bid on: " + product.getProductName());
    }

    @Override
    public void sell(Product product) {
        System.out.println("[SELL] User " + this.toString().split("\n")[1]
                + " is listing for sale: " + product.getProductName());
    }

    @Override
    public void leaveRoom() {
        System.out.println("User has left the auction room.");
    }
}
