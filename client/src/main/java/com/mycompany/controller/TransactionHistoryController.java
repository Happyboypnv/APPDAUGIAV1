package com.mycompany.controller;

import com.mycompany.models.User;
import com.mycompany.server.dto.TransactionHistoryItemDTO;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

public class TransactionHistoryController implements Initializable {

  @FXML private ListView<TransactionHistoryItemDTO> transactionListView;
  @FXML private Label emptyLabel;
  @FXML private Label subTitleLabel;

  private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
  private static final DateTimeFormatter DISPLAY_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    transactionListView.setCellFactory(lv -> new TransactionCard());
    loadTransactions();
  }

  private void loadTransactions() {
    User currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      emptyLabel.setText("Bạn cần đăng nhập để xem lịch sử giao dịch.");
      emptyLabel.setVisible(true);
      emptyLabel.setManaged(true);
      return;
    }

    emptyLabel.setText("Đang tải...");
    emptyLabel.setVisible(true);
    emptyLabel.setManaged(true);

    String token = SessionManager.getInstance().getServerToken();
    new Thread(() -> {
      List<TransactionHistoryItemDTO> transactions =
          ApiClient.getTransactionHistory(token);

      Platform.runLater(() -> {
        if (transactions.isEmpty()) {
          emptyLabel.setText("Chưa có giao dịch nào.");
          emptyLabel.setVisible(true);
          emptyLabel.setManaged(true);
          transactionListView.setVisible(false);
          if (subTitleLabel != null) subTitleLabel.setText("Chưa có giao dịch nào.");
        } else {
          emptyLabel.setVisible(false);
          emptyLabel.setManaged(false);
          transactionListView.setVisible(true);
          transactionListView.setItems(FXCollections.observableArrayList(transactions));
          if (subTitleLabel != null)
            subTitleLabel.setText(transactions.size() + " giao dịch được tìm thấy");
        }
      });
    }).start();
  }

  private class TransactionCard extends ListCell<TransactionHistoryItemDTO> {

    private final HBox root;
    private final Label iconLabel;
    private final Label productLabel;
    private final Label sessionLabel;
    private final Label roleLabel;
    private final Label priceLabel;
    private final Label statusBadge;
    private final Label timeLabel;

    TransactionCard() {
      iconLabel = new Label();
      iconLabel.setPrefSize(46, 46);
      iconLabel.setMinSize(46, 46);
      iconLabel.setAlignment(Pos.CENTER);

      productLabel = new Label();
      productLabel.setFont(Font.font("System Bold", 15));
      productLabel.setTextFill(Color.WHITE);
      productLabel.setWrapText(false);

      sessionLabel = new Label();
      sessionLabel.setFont(Font.font(12));
      sessionLabel.setTextFill(Color.web("#b6bfff", 0.65));

      roleLabel = new Label();
      roleLabel.setFont(Font.font(11));
      roleLabel.setTextFill(Color.web("#b6bfff", 0.55));

      VBox infoCol = new VBox(3, productLabel, sessionLabel, roleLabel);
      infoCol.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(infoCol, Priority.ALWAYS);

      priceLabel = new Label();
      priceLabel.setFont(Font.font("System Bold", 14));
      priceLabel.setTextFill(Color.web("#2ecc71"));
      priceLabel.setAlignment(Pos.CENTER_RIGHT);

      statusBadge = new Label();

      timeLabel = new Label();
      timeLabel.setFont(Font.font(11));
      timeLabel.setTextFill(Color.web("#6b7db3"));
      timeLabel.setAlignment(Pos.CENTER_RIGHT);

      VBox rightCol = new VBox(4, priceLabel, statusBadge, timeLabel);
      rightCol.setAlignment(Pos.CENTER_RIGHT);
      rightCol.setPrefWidth(170);

      root = new HBox(14, iconLabel, infoCol, rightCol);
      root.setAlignment(Pos.CENTER_LEFT);
      root.setPadding(new Insets(12, 16, 12, 16));
      applyCardStyle(false);

      root.setOnMouseEntered(e -> applyCardStyle(true));
      root.setOnMouseExited(e -> applyCardStyle(false));
    }

    private void applyCardStyle(boolean hovered) {
      root.setStyle(hovered
          ? "-fx-background-color: rgba(255,255,255,0.10);" +
          "-fx-background-radius: 12;" +
          "-fx-border-color: rgba(238,116,85,0.35);" +
          "-fx-border-radius: 12;" +
          "-fx-border-width: 1;" +
          "-fx-cursor: hand;"
          : "-fx-background-color: rgba(255,255,255,0.06);" +
          "-fx-background-radius: 12;" +
          "-fx-border-color: rgba(182,191,255,0.12);" +
          "-fx-border-radius: 12;" +
          "-fx-border-width: 1;" +
          "-fx-cursor: hand;"
      );
    }

    @Override
    protected void updateItem(TransactionHistoryItemDTO tx, boolean empty) {
      super.updateItem(tx, empty);
      if (empty || tx == null) {
        setGraphic(null);
        setText(null);
        setStyle("-fx-background-color: transparent;");
        return;
      }

      boolean isPaid = "PAID".equals(tx.status) || "COMPLETED".equals(tx.status);

      iconLabel.setText(isPaid ? "✓" : "✕");
      iconLabel.setStyle(
          "-fx-background-color: " + (isPaid ? "rgba(46,204,113,0.15)" : "rgba(231,76,60,0.15)") + ";" +
              "-fx-background-radius: 23;" +
              "-fx-border-color: " + (isPaid ? "rgba(46,204,113,0.5)" : "rgba(231,76,60,0.5)") + ";" +
              "-fx-border-radius: 23;" +
              "-fx-border-width: 2;" +
              "-fx-font-size: 17px;" +
              "-fx-text-fill: " + (isPaid ? "#2ecc71" : "#e74c3c") + ";" +
              "-fx-font-weight: bold;"
      );

      productLabel.setText(tx.productName != null ? tx.productName : "Sản phẩm không xác định");
      sessionLabel.setText("Mã phiên: " + (tx.sessionId != null ? tx.sessionId : "—"));
      roleLabel.setText("SELLER".equals(tx.role) ? "👤 Người bán" : "🛒 Người mua");
      priceLabel.setText(moneyFormat.format(tx.amount) + " VNĐ");

      if (isPaid) {
        statusBadge.setText("● Đã thanh toán");
        statusBadge.setStyle(
            "-fx-background-color: rgba(46,204,113,0.18);" +
                "-fx-border-color: rgba(46,204,113,0.55);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: #2ecc71;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 10 3 10;"
        );
      } else {
        statusBadge.setText("✕ Đã hủy");
        statusBadge.setStyle(
            "-fx-background-color: rgba(231,76,60,0.18);" +
                "-fx-border-color: rgba(231,76,60,0.55);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: #e74c3c;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 10 3 10;"
        );
      }

      timeLabel.setText("🕐 " + formatTime(tx.createdAt));

      setGraphic(root);
      setText(null);
      setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
    }

    private String formatTime(String raw) {
      if (raw == null || raw.isBlank()) return "—";
      for (DateTimeFormatter f : new DateTimeFormatter[]{
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      }) {
        try { return LocalDateTime.parse(raw, f).format(DISPLAY_FMT); }
        catch (DateTimeParseException ignored) {}
      }
      return raw.length() > 16 ? raw.substring(0, 16) : raw;
    }
  }
}