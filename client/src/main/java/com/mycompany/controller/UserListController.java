package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.local.HiddenAuctionRepository;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;

import java.util.*;

import javafx.scene.control.ButtonType;

import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class UserListController implements Initializable {
    @FXML private Label labelWelcome;
    @FXML private Label searchIcon, labelTotalUsers, labelBannedUsers, labelOnlineUsers;
    @FXML private TextField searchField;


    @FXML
    ListView<User> listViewUser;

    private List<User> danhSachUsers;
    private Timeline autoRefreshTimeline;
    private int totalUsers = ApiClient.getTotalUsersCount();
    private int onlineUsers = ApiClient.getTotalOnlineUsersCount();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        labelTotalUsers.setText(String.valueOf(totalUsers));
        labelOnlineUsers.setText(String.valueOf(onlineUsers));


        if (labelWelcome != null && SessionManager.getInstance().getCurrentUser() != null) {
            labelWelcome.setText("Xin chào, " +
                    SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }

        listViewUser.setCellFactory(lv -> new UserCell());

        taiDanhSachUsers();

        // Tự động dừng Timeline khi scene này bị thay thế (rời Home)
        listViewUser.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }
        });
    }


    private void taiDanhSachUsers() {
        // Load lần đầu ngay lập tức
        doRefresh();

        // Auto-refresh mỗi 5 giây
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> doRefresh())
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void doRefresh() {
        new Thread(() -> {
            List<User> allUsers = ApiClient.getAllUsers();
            // Filter to show only WAITING and IN_PROGRESS auctions for users

            Platform.runLater(() -> {
                danhSachUsers = allUsers;
                // Giữ vị trí scroll hiện tại
                int selectedIndex = listViewUser.getSelectionModel().getSelectedIndex();
                listViewUser.setItems(FXCollections.observableArrayList(danhSachUsers));
                if (selectedIndex >= 0 && selectedIndex < danhSachUsers.size()) {
                    listViewUser.getSelectionModel().select(selectedIndex);
                }
            });
        }).start();
    }

    private class UserCell extends ListCell<User> {

        private final HBox root;
        private final Label lblBanBadge;
        private final Label lblTenNguoiDung;
        private final Label lblEmail;
        private final Label lblSdt;
        private final Label lblDiaChi;
        private final Label lblNgaySinh;
        private final Button btnBan;
        private final Button btnUnban;

        UserCell() {
            // ── Badge trạng thái BAN (bên trái) ──────────────────
            lblBanBadge = new Label();
            lblBanBadge.setPrefWidth(100);
            lblBanBadge.setAlignment(Pos.CENTER);
            lblBanBadge.setPadding(new Insets(4, 8, 4, 8));
            lblBanBadge.setFont(Font.font("System Bold", 11));
            lblBanBadge.setWrapText(false);

            // ── Avatar placeholder ────────────────────────────────
            Label lblAvatar = new Label("👤");
            lblAvatar.setFont(Font.font(28));
            lblAvatar.setAlignment(Pos.CENTER);
            lblAvatar.setPrefSize(56, 56);
            lblAvatar.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.10);" +
                            "-fx-background-radius: 28;"
            );

            // ── Cột giữa: tên + email ─────────────────────────────
            lblTenNguoiDung = new Label();
            lblTenNguoiDung.setFont(Font.font("System Bold", 14));
            lblTenNguoiDung.setTextFill(Color.WHITE);

            lblEmail = new Label();
            lblEmail.setFont(Font.font(12));
            lblEmail.setTextFill(Color.web("#aab4d4"));

            VBox colInfo = new VBox(3, lblTenNguoiDung, lblEmail);
            colInfo.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(colInfo, Priority.ALWAYS);

            // ── Cột SĐT + Ngày sinh ───────────────────────────────
            lblSdt = new Label();
            lblSdt.setFont(Font.font(12));
            lblSdt.setTextFill(Color.web("#7ec8e3"));

            lblNgaySinh = new Label();
            lblNgaySinh.setFont(Font.font(12));
            lblNgaySinh.setTextFill(Color.web("#f9a875"));

            VBox colContact = new VBox(3,
                    makeHeader("📞 SĐT"),     lblSdt,
                    makeHeader("🎂 Ngày sinh"), lblNgaySinh);
            colContact.setAlignment(Pos.CENTER_LEFT);
            colContact.setPrefWidth(170);

            // ── Cột địa chỉ ──────────────────────────────────────
            lblDiaChi = new Label();
            lblDiaChi.setFont(Font.font(12));
            lblDiaChi.setTextFill(Color.web("#b0c4de"));
            lblDiaChi.setWrapText(true);
            lblDiaChi.setMaxWidth(160);

            VBox colAddr = new VBox(3, makeHeader("📍 Địa chỉ"), lblDiaChi);
            colAddr.setAlignment(Pos.CENTER_LEFT);
            colAddr.setPrefWidth(170);

            // ── Nút hành động ─────────────────────────────────────
            btnBan = new Button("🚫 Ban");
            btnBan.setPrefWidth(90);
            btnBan.setStyle(
                    "-fx-background-color: rgba(231,76,60,0.20);" +
                            "-fx-text-fill: #e74c3c;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: #e74c3c;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;" +
                            "-fx-font-weight: bold;"
            );

            btnUnban = new Button("✅ Unban");
            btnUnban.setPrefWidth(90);
            btnUnban.setStyle(
                    "-fx-background-color: rgba(46,204,113,0.20);" +
                            "-fx-text-fill: #2ecc71;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: #2ecc71;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;" +
                            "-fx-font-weight: bold;"
            );

            // ── Gộp nút vào VBox để chỉ hiện 1 nút tại 1 thời điểm ──
            VBox colAction = new VBox(btnBan, btnUnban);
            colAction.setAlignment(Pos.CENTER);

            // ── Root card ─────────────────────────────────────────
            root = new HBox(14, lblBanBadge, lblAvatar, colInfo, colContact, colAddr, colAction);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 16, 12, 16));
            root.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: rgba(255,255,255,0.12);" +
                            "-fx-border-radius: 14;" +
                            "-fx-border-width: 1.5;"
            );
        }

        private Label makeHeader(String text) {
            Label l = new Label(text);
            l.setFont(Font.font(10));
            l.setTextFill(Color.web("#6b7db3"));
            return l;
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);

            if (empty || user == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            // ── Điền dữ liệu ──────────────────────────────────────
            lblTenNguoiDung.setText(user.getFullName() != null ? user.getFullName() : "—");
            lblEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "?"));
            lblSdt.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "—");
            lblDiaChi.setText(user.getAddress() != null ? user.getAddress() : "—");
            lblNgaySinh.setText(user.getDateOfBirth() != null
                    ? user.getDateOfBirth() : "—");

            // ── Badge + màu viền card theo trạng thái ban ──────────
            boolean isBanned = user.getIsBanned() == 1;
            btnBan.setVisible(!isBanned);
            btnBan.setManaged(!isBanned);
            btnUnban.setVisible(isBanned);
            btnUnban.setManaged(isBanned);

            if (isBanned) {
                lblBanBadge.setText("✕ BANNED");
                lblBanBadge.setStyle(
                        "-fx-background-color: rgba(231,76,60,0.25);" +
                                "-fx-text-fill: #e74c3c;" +
                                "-fx-background-radius: 6;" +
                                "-fx-border-color: #e74c3c;" +
                                "-fx-border-radius: 6;" +
                                "-fx-border-width: 1;"
                );
                root.setStyle(
                        "-fx-background-color: rgba(231,76,60,0.07);" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-color: rgba(231,76,60,0.45);" +
                                "-fx-border-radius: 14;" +
                                "-fx-border-width: 1.5;"
                );
            } else {
                lblBanBadge.setText("● Hoạt động");
                lblBanBadge.setStyle(
                        "-fx-background-color: rgba(46,204,113,0.25);" +
                                "-fx-text-fill: #2ecc71;" +
                                "-fx-background-radius: 6;" +
                                "-fx-border-color: #2ecc71;" +
                                "-fx-border-radius: 6;" +
                                "-fx-border-width: 1;"
                );
                root.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.06);" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-color: rgba(255,255,255,0.12);" +
                                "-fx-border-radius: 14;" +
                                "-fx-border-width: 1.5;"
                );
            }

            // ── Gán handler (truyền data vào controller) ──────────
            btnBan.setOnAction(e -> handleBanUser());
            btnUnban.setOnAction(e -> handleUnbanUser());

            setGraphic(root);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
        }
    }

    private void handleUnbanUser() {
        User currentUser = listViewUser.getSelectionModel().getSelectedItem();
        String email = currentUser.getEmail();
        String token = SessionManager.getInstance().getServerToken();
        ApiClient.banUser(email,token);
    }

    private void handleBanUser() {
        User currentUser = listViewUser.getSelectionModel().getSelectedItem();
        String email = currentUser.getEmail();
        String token = SessionManager.getInstance().getServerToken();
        ApiClient.unbanUser(email,token);
    }
}