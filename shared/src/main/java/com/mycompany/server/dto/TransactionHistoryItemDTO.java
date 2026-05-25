package com.mycompany.server.dto;

public class TransactionHistoryItemDTO {
  public String transactionId;
  public String sessionId;
  public String productName;
  public String role;
  public String buyerName;
  public String sellerName;
  public double amount;
  public String status;
  public String createdAt;

  public TransactionHistoryItemDTO(String transactionId, String sessionId, String productName,
                                   String role, String buyerName, String sellerName,
                                   double amount, String status, String createdAt) {
    this.transactionId = transactionId;
    this.sessionId = sessionId;
    this.productName = productName;
    this.role = role;
    this.buyerName = buyerName;
    this.sellerName = sellerName;
    this.amount = amount;
    this.status = status;
    this.createdAt = createdAt;
  }
}
