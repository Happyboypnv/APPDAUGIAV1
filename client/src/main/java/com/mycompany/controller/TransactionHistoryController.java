package com.mycompany.controller;

import com.mycompany.models.User;
import com.mycompany.server.dto.TransactionHistoryItemDTO;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.DecimalFormat;
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

  private final DecimalFormat moneyFormat = new DecimalFormat("#,### VND");

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

    // Hiển thị trạng thái loading
    emptyLabel.setText("Đang tải...");
    emptyLabel.setVisible(true);
    emptyLabel.setManaged(true);

    // Chạy HTTP call trên background thread, không block FX thread
    String token = SessionManager.getInstance().getServerToken();
    new Thread(() -> {
      List<TransactionHistoryItemDTO> transactions =
          ApiClient.getTransactionHistory(token);
      List<TransactionHistoryRow> rows = transactions.stream()
          .map(this::toRow)
          .toList();

      // Cập nhật UI trở lại trên FX thread
      javafx.application.Platform.runLater(() -> {
        transactionTable.setItems(javafx.collections.FXCollections.observableArrayList(rows));
        if (rows.isEmpty()) {
          emptyLabel.setText("Chưa có giao dịch nào.");
          emptyLabel.setVisible(true);
          emptyLabel.setManaged(true);
        } else {
          emptyLabel.setVisible(false);
          emptyLabel.setManaged(false);
        }
      });
    }).start();
  }

  private TransactionHistoryRow toRow(TransactionHistoryItemDTO tx) {
    String role = "SELLER".equals(tx.role) ? "Nguoi ban" : "Nguoi mua";
    return new TransactionHistoryRow(
        tx.transactionId,
        tx.sessionId,
        tx.productName != null ? tx.productName : "",
        role,
        tx.buyerName != null ? tx.buyerName : "",
        tx.sellerName != null ? tx.sellerName : "",
        moneyFormat.format(tx.amount),
        tx.status,
        tx.createdAt
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
