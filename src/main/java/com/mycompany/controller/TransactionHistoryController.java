package com.mycompany.controller;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.Transaction;
import com.mycompany.models.User;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.TransactionRepositorySQLite;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class TransactionHistoryController implements Initializable {
  @FXML private TableView<TransactionHistoryRow> transactionTable;
  @FXML private TableColumn<TransactionHistoryRow, String> transactionIdColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> sessionIdColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> productColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> roleColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> buyerColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> sellerColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> amountColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> statusColumn;
  @FXML private TableColumn<TransactionHistoryRow, String> createdAtColumn;
  @FXML private Label emptyLabel;

  private final TransactionRepositorySQLite transactionRepository = new TransactionRepositorySQLite();
  private final DecimalFormat moneyFormat = new DecimalFormat("#,### VNĐ");
  private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    bindColumns();
    loadTransactions();
  }

  private void bindColumns() {
    transactionIdColumn.setCellValueFactory(data -> data.getValue().transactionIdProperty());
    sessionIdColumn.setCellValueFactory(data -> data.getValue().sessionIdProperty());
    productColumn.setCellValueFactory(data -> data.getValue().productProperty());
    roleColumn.setCellValueFactory(data -> data.getValue().roleProperty());
    buyerColumn.setCellValueFactory(data -> data.getValue().buyerProperty());
    sellerColumn.setCellValueFactory(data -> data.getValue().sellerProperty());
    amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());
    statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
    createdAtColumn.setCellValueFactory(data -> data.getValue().createdAtProperty());
  }

  private void loadTransactions() {
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      emptyLabel.setText("Bạn cần đăng nhập để xem lịch sử giao dịch.");
      return;
    }

    List<Transaction> transactions = transactionRepository.findByUserId(currentUser.getUserId());
    List<TransactionHistoryRow> rows = transactions.stream()
        .map(tx -> toRow(tx, currentUser))
        .toList();

    transactionTable.setItems(FXCollections.observableArrayList(rows));
    emptyLabel.setVisible(rows.isEmpty());
    emptyLabel.setManaged(rows.isEmpty());
  }

  private TransactionHistoryRow toRow(Transaction tx, User currentUser) {
    AuctionSession auction = tx.getAuctionSession();
    User winner = auction.getWinner();
    User seller = auction.getSeller();
    boolean isSeller = seller != null && seller.getUserId().equals(currentUser.getUserId());
    String role = isSeller ? "Người bán" : "Người mua";

    return new TransactionHistoryRow(
        tx.getId(),
        auction.getSessionId(),
        auction.getProduct() != null ? auction.getProduct().getProductName() : "",
        role,
        winner != null ? winner.getFullName() : "",
        seller != null ? seller.getFullName() : "",
        moneyFormat.format(auction.getCurrentPrice()),
        tx.getStatus().name(),
        tx.getCreatedAt().format(timeFormat)
    );
  }

  public static class TransactionHistoryRow {
    private final SimpleStringProperty transactionId;
    private final SimpleStringProperty sessionId;
    private final SimpleStringProperty product;
    private final SimpleStringProperty role;
    private final SimpleStringProperty buyer;
    private final SimpleStringProperty seller;
    private final SimpleStringProperty amount;
    private final SimpleStringProperty status;
    private final SimpleStringProperty createdAt;

    public TransactionHistoryRow(String transactionId, String sessionId, String product,
                                 String role, String buyer, String seller, String amount,
                                 String status, String createdAt) {
      this.transactionId = new SimpleStringProperty(transactionId);
      this.sessionId = new SimpleStringProperty(sessionId);
      this.product = new SimpleStringProperty(product);
      this.role = new SimpleStringProperty(role);
      this.buyer = new SimpleStringProperty(buyer);
      this.seller = new SimpleStringProperty(seller);
      this.amount = new SimpleStringProperty(amount);
      this.status = new SimpleStringProperty(status);
      this.createdAt = new SimpleStringProperty(createdAt);
    }

    public SimpleStringProperty transactionIdProperty() { return transactionId; }
    public SimpleStringProperty sessionIdProperty() { return sessionId; }
    public SimpleStringProperty productProperty() { return product; }
    public SimpleStringProperty roleProperty() { return role; }
    public SimpleStringProperty buyerProperty() { return buyer; }
    public SimpleStringProperty sellerProperty() { return seller; }
    public SimpleStringProperty amountProperty() { return amount; }
    public SimpleStringProperty statusProperty() { return status; }
    public SimpleStringProperty createdAtProperty() { return createdAt; }
  }
}