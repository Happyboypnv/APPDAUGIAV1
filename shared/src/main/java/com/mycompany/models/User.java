package com.mycompany.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class User extends Person implements CanBeSold, CanLeaveRoom, CanBid {

  @SerializedName("role")
  private String roleNameFromServer;

  @SerializedName("diaChi")
  private String address;

  @SerializedName("soDienThoai")
  private String phoneNumber;

  private double actualBalance;    // ← THAY THẾ availableBalance
  private double frozenBalance;    // ← MỚI
  private List<Transaction> transactions;

  @SerializedName("soTaiKhoanNganHang")
  private String bankAccountNumber;

  @SerializedName("tenNganHang")
  private String bankName;

  private int isBanned = 0; // ban đầu ko bị ban
  private boolean isOnline = false;

  public User(String fullName, String email,
              String password, String dateOfBirth,
              String address, String phoneNumber) {
    super(fullName, email, password, dateOfBirth);
    this.address = address;
    this.phoneNumber = phoneNumber;
    this.actualBalance = 0;      // ← SỬA
    this.frozenBalance = 0;      // ← THÊM
    this.transactions = new ArrayList<>();
  }

  public User(String fullName, String email, String password, String dateOfBirth) {
    super(fullName, email, password, dateOfBirth);
    this.transactions = new ArrayList<>();
    this.actualBalance = 0;      // ← SỬA
    this.frozenBalance = 0;      // ← THÊM
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

  // ← XÓA setAvailableBalance() cũ, THÊM 4 method này vào
  public double getActualBalance() {
    return actualBalance;
  }

  public String getRoleNameFromServer() {
    return roleNameFromServer;
  }

  public void setRoleNameFromServer(String roleNameFromServer) {
    this.roleNameFromServer = roleNameFromServer;
  }

  public void setActualBalance(double actualBalance) {
    this.actualBalance = actualBalance;
  }

  public double getFrozenBalance() {
    return frozenBalance;
  }

  public void setFrozenBalance(double frozenBalance) {
    this.frozenBalance = frozenBalance;
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
    return actualBalance - frozenBalance;  // ← SỬA: tính động thay vì return field
  }

  public String getAddress() {
    return address;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public int getIsBanned() {
    return isBanned;
  }

  public void setIsBanned(int isBanned) {
    this.isBanned = isBanned;
  }

  public void setIsOnline(boolean isOnline) {this.isOnline = isOnline;}

  public boolean isOnline() {return isOnline;}

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

  public void setAvailableBalance(double newBalance) {
    this.actualBalance = newBalance + this.frozenBalance;
  }
}
