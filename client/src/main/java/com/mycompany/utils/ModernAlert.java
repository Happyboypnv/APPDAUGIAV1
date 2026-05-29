package com.mycompany.utils;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * ModernAlert — thay thế Alert mặc định JavaFX bằng dialog tối, hiện đại
 * khớp với theme #1c254b của ứng dụng.
 *
 * - INFORMATION: tự đóng sau 3 giây kèm thanh progress xanh lá trượt phải→trái
 * - ERROR / WARNING / CONFIRMATION: chỉ đóng khi bấm nút
 *
 * Cách dùng: ModernAlert.show(Alert.AlertType.INFORMATION, "Tiêu đề", "Nội dung");
 */
public class ModernAlert {

  private static final int AUTO_CLOSE_MS = 3000;

  public static void show(Alert.AlertType type, String title, String message) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> show(type, title, message));
      return;
    }
    showOnFxThread(type, title, message);
  }

  private static void showOnFxThread(Alert.AlertType type, String title, String message) {

    // ── Màu sắc theo loại alert ──────────────────────────────
    String icon, accentColor;
    switch (type) {
      case ERROR        -> { icon = "✕"; accentColor = "#e74c3c"; }
      case WARNING      -> { icon = "⚠"; accentColor = "#f39c12"; }
      case CONFIRMATION -> { icon = "?"; accentColor = "#3498db"; }
      default           -> { icon = "✓"; accentColor = "#2ecc71"; }
    }

    boolean autoClose = (type == Alert.AlertType.INFORMATION);

    Stage stage = new Stage();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setResizable(false);

    // ── Backdrop ─────────────────────────────────────────────
    StackPane backdrop = new StackPane();
    backdrop.setStyle("-fx-background-color: rgba(8,12,35,0.65);");
    backdrop.setMinSize(460, autoClose ? 340 : 320);

    // ── Card ─────────────────────────────────────────────────
    VBox card = new VBox(18);
    card.setAlignment(Pos.CENTER);
    card.setMaxWidth(400);
    card.setMinWidth(320);
    card.setPadding(new Insets(34, 36, autoClose ? 0 : 30, 36));
    card.setStyle(
        "-fx-background-color: #1e2a5e;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.13);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;"
    );

    // Thanh accent trên cùng
    Region topAccent = new Region();
    topAccent.setPrefSize(52, 4);
    topAccent.setMaxWidth(52);
    topAccent.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 3;");

    // Icon circle
    StackPane iconCircle = new StackPane();
    iconCircle.setMinSize(62, 62);
    iconCircle.setMaxSize(62, 62);
    iconCircle.setStyle(
        "-fx-background-color: " + accentColor + "22;" +
            "-fx-background-radius: 31;" +
            "-fx-border-color: " + accentColor + "55;" +
            "-fx-border-radius: 31;" +
            "-fx-border-width: 1.5;"
    );
    Label iconLbl = new Label(icon);
    iconLbl.setFont(Font.font("System", FontWeight.BOLD, 24));
    iconLbl.setStyle("-fx-text-fill: " + accentColor + ";");
    iconCircle.getChildren().add(iconLbl);

    // Tiêu đề
    Label lblTitle = new Label(title);
    lblTitle.setFont(Font.font("System", FontWeight.BOLD, 17));
    lblTitle.setStyle("-fx-text-fill: #ffffff;");
    lblTitle.setWrapText(true);
    lblTitle.setMaxWidth(328);
    lblTitle.setAlignment(Pos.CENTER);
    lblTitle.setTextAlignment(TextAlignment.CENTER);

    // Divider
    Region divider = new Region();
    divider.setPrefHeight(1);
    divider.setPrefWidth(160);
    divider.setMaxWidth(160);
    divider.setStyle("-fx-background-color: rgba(255,255,255,0.10);");

    // Nội dung
    Label lblMsg = new Label(message);
    lblMsg.setFont(Font.font(13));
    lblMsg.setStyle("-fx-text-fill: #9aabcc;");
    lblMsg.setWrapText(true);
    lblMsg.setMaxWidth(328);
    lblMsg.setAlignment(Pos.CENTER);
    lblMsg.setTextAlignment(TextAlignment.CENTER);

    // Nút Đóng
    final String btnBase =
        "-fx-background-color: " + accentColor + ";" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian," + accentColor + "55,10,0,0,3);";
    final String btnHover =
        "-fx-background-color: derive(" + accentColor + ",-18%);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;";

    Button btnOk = new Button("  Đóng  ");
    btnOk.setPrefWidth(140);
    btnOk.setPrefHeight(42);
    btnOk.setStyle(btnBase);
    btnOk.setOnMouseEntered(e -> btnOk.setStyle(btnHover));
    btnOk.setOnMouseExited(e -> btnOk.setStyle(btnBase));
    btnOk.setOnAction(e -> stage.close());

    card.getChildren().addAll(topAccent, iconCircle, lblTitle, divider, lblMsg, btnOk);

    // ── Progress bar — dùng Rectangle + clip để animate chắc chắn ──
    // Rectangle không phụ thuộc VBox layout width như Region
    final Rectangle[] barFillRef = { null };

    if (autoClose) {
      Region spacer = new Region();
      spacer.setPrefHeight(18);

      // Container full-width sát đáy card
      // Dùng Pane thay StackPane để Rectangle tự do set x/y/width
      Pane barPane = new Pane();
      barPane.setPrefHeight(6);
      barPane.setMaxHeight(6);
      barPane.setMinHeight(6);
      barPane.setStyle("-fx-background-radius: 0 0 19 19;");
      VBox.setMargin(barPane, new Insets(0, -36, 0, -36));

      // Nền xám — width sẽ được set sau khi layout xong
      Rectangle barBg = new Rectangle();
      barBg.setHeight(6);
      barBg.setFill(Color.web("rgba(255,255,255,0.12)"));
      barBg.setArcWidth(0);
      barBg.setArcHeight(0);

      // Thanh xanh lá
      Rectangle barFill = new Rectangle();
      barFill.setHeight(6);
      barFill.setFill(Color.web("#2ecc71"));
      barFill.setX(0);
      barFill.setY(0);
      barFill.setArcWidth(0);
      barFill.setArcHeight(0);

      barPane.getChildren().addAll(barBg, barFill);
      card.getChildren().addAll(spacer, barPane);
      barFillRef[0] = barFill;

      // Sau khi layout xong, set width Rectangle theo width thực của barPane
      barPane.widthProperty().addListener((obs, oldW, newW) -> {
        double w = newW.doubleValue();
        if (w > 0) {
          barBg.setWidth(w);
          barFill.setWidth(w);
        }
      });
    }

    // ── Build scene ──────────────────────────────────────────
    StackPane.setAlignment(card, Pos.CENTER);
    backdrop.getChildren().add(card);

    Scene scene = new Scene(backdrop, 460, autoClose ? 345 : 320, Color.TRANSPARENT);
    stage.setScene(scene);

    card.setOpacity(0);
    card.setTranslateY(18);

    // Platform.runLater chạy sau khi showAndWait mở stage và layout pass đầu tiên xong
    Platform.runLater(() -> {
      // Animate in
      FadeTransition fadein = new FadeTransition(Duration.millis(260), card);
      fadein.setFromValue(0);
      fadein.setToValue(1);

      TranslateTransition slidein = new TranslateTransition(Duration.millis(260), card);
      slidein.setFromY(18);
      slidein.setToY(0);

      new ParallelTransition(fadein, slidein).play();

      // Auto-close timeline: animate width của Rectangle từ full → 0
      if (autoClose && barFillRef[0] != null) {
        Rectangle barFill = barFillRef[0];
        double fullWidth = barFill.getWidth(); // lấy width thực sau layout
        System.out.println("[ModernAlert] barFill width = " + fullWidth); // debug

        // Nếu width vẫn 0 (chưa layout xong), dùng runLater thêm 1 tick nữa
        Runnable startTimeline = () -> {
          double w = barFill.getWidth() > 0 ? barFill.getWidth() : 400;
          Timeline tl = new Timeline(
              new KeyFrame(Duration.ZERO,
                  new KeyValue(barFill.widthProperty(), w)),
              new KeyFrame(Duration.millis(AUTO_CLOSE_MS),
                  new KeyValue(barFill.widthProperty(), 0))
          );
          tl.setOnFinished(e -> { if (stage.isShowing()) stage.close(); });
          tl.play();
        };

        if (barFill.getWidth() > 0) {
          startTimeline.run();
        } else {
          // Thêm 1 tick nữa để chắc chắn layout xong
          Platform.runLater(startTimeline);
        }
      }
    });

    stage.showAndWait();
  }
}