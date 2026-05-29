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

    private List<User> danhSachUsers = new ArrayList<>();
    private Timeline autoRefreshTimeline;
    private int totalUsers = ApiClient.getTotalUsersCount();
    private int onlineUsers = ApiClient.getTotalOnlineUsersCount();
    private int bannedUsers = ApiClient.getBannedUsersCount();
    private User currentUser = SessionManager.getInstance().getCurrentUser();
    private final ObservableList<User> danhSachUserObservable = FXCollections.observableArrayList();
    private String currentStatusFilter = "ALL"; // Thêm dòng này để lưu trạng thái lọc hiện tại

    @FXML
    private VBox adminNavBar;
    @FXML
    private AdminNavBarController adminNavBarController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        adminNavBarController.setActive("user");
        loadDataFromApi();
        labelTotalUsers.setText(String.valueOf(totalUsers));
        labelOnlineUsers.setText(String.valueOf(onlineUsers));
        labelBannedUsers.setText(String.valueOf(bannedUsers));

        if (labelWelcome != null && SessionManager.getInstance().getCurrentUser() != null) {
            labelWelcome.setText("Xin chào, " +
                    SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }

        listViewUser.setCellFactory(lv -> new UserCell());
        listViewUser.setItems(danhSachUserObservable);

        taiDanhSachUsers();

        // Tự động dừng Timeline khi scene này bị thay thế (rời Home)
        listViewUser.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }
        });
    }


    private void taiDanhSachUsers() {
        // Khởi động lấy dữ liệu từ API lần đầu tiên
        loadDataFromApi();

        // Auto-refresh: Cứ mỗi 5 giây gọi API lấy dữ liệu mới nhất
        // Tránh việc gọi doRefresh() thủ công làm mất dữ liệu gốc
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> loadDataFromApi())
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void loadDataFromApi() {
        new Thread(() -> {
            try {
                // 1. Gọi API lấy dữ liệu mới nhất từ Server
                List<User> allUsers = ApiClient.getAllUsers();

                if (allUsers != null) {
                    // 2. Loại bỏ chính mình (currentUser) khỏi danh sách
                    allUsers.removeIf(user -> user.getEmail().equals(currentUser.getEmail()));

                    // 3. Cập nhật vào bộ nhớ đệm danhSachUsers
                    danhSachUsers = allUsers;

                    // 4. Tiến hành lọc theo từ khóa hiện tại và đẩy lên giao diện
                    doRefresh();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void doRefresh() {
        List<User> targetList;

        if (currentStatusFilter == null || "ALL".equals(currentStatusFilter) || currentStatusFilter.trim().isEmpty()) {
            targetList = new ArrayList<>(danhSachUsers);
        } else {
            targetList = new ArrayList<>();
            String filterText = currentStatusFilter.trim().toLowerCase();

            for (User user : danhSachUsers) {
                if (user.getFullName() != null) {
                    String fullName = user.getFullName().trim().toLowerCase();
                    if (fullName.contains(filterText)) {
                        targetList.add(user);
                    }
                }
            }
        }

        // Đẩy dữ liệu đã lọc lên giao diện trên UI Thread
        Platform.runLater(() -> {
            // TUYỆT ĐỐI KHÔNG ghi đè: danhSachUsers = targetList;

            // Giữ vị trí scroll hiện tại
            int selectedIndex = listViewUser.getSelectionModel().getSelectedIndex();

            // Cập nhật List View
            danhSachUserObservable.setAll(targetList);

            if (selectedIndex >= 0 && selectedIndex < targetList.size()) {
                listViewUser.getSelectionModel().select(selectedIndex);
            }
        });
    }

    @FXML
    public void handleSearchUser() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            currentStatusFilter = "ALL";
        } else {
            currentStatusFilter = text.trim().toLowerCase();
        }

        // Chạy hàm lọc cục bộ ngay lập tức (mượt mà, không bị lag)
        doRefresh();
    }

    private class UserCell extends ListCell<User> {

        private final HBox root;
        private final Label lblBanBadge;
        private final Label lblAdminBadge;
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

            lblAdminBadge = new Label("ADMIN");
            lblAdminBadge.setPadding(new Insets(2, 6, 2, 6)); // Padding nhỏ hơn badge chính
            lblAdminBadge.setFont(Font.font("System Bold", 10)); // Font nhỏ hơn
            // Định kiểu màu vàng Admin (customize & filter)
            lblAdminBadge.setStyle(
                    "-fx-background-color: rgba(243, 156, 18, 0.2);" + // vàng cam nhạt nền (Flat UI Orange/Yellow)
                            "-fx-text-fill: #f39c12;" + // màu vàng chữ (Flat UI Orange/Yellow)
                            "-fx-background-radius: 6;" +
                            "-fx-border-color: #f39c12;" +
                            "-fx-border-radius: 6;" +
                            "-fx-border-width: 1;"
            );
            lblAdminBadge.setVisible(false); // Ẩn mặc định
            lblAdminBadge.setManaged(false); // Quan trọng để không chiếm không gian layout khi ẩn

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

            VBox colInfo = new VBox(3, lblAdminBadge, lblTenNguoiDung, lblEmail);
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
            boolean isUserAdmin = user.getRole()==1;

            if (isUserAdmin) {
                // Hiện badge ADMIN màu vàng
                lblAdminBadge.setVisible(true);
                lblAdminBadge.setManaged(true);

                // Ẩn nút Ban/Unban nhưng giữ khoảng trống tàng hình hoàn hảo (Không lo viền đỏ)
                btnBan.setVisible(false);
                btnBan.setManaged(true);
                btnBan.setDisable(true);
                btnBan.setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");

                btnUnban.setVisible(false);
                btnUnban.setManaged(true);
                btnUnban.setDisable(true);
                btnUnban.setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");

            } else {
                // ====== 2. ĐỐI VỚI USER THƯỜNG (PHẢI XOÁ SẠCH TRẠNG THÁI ADMIN CŨ) ======
                // Bắt buộc ẩn badge ADMIN đi để tránh bị lỗi lây từ cell cũ sang
                lblAdminBadge.setVisible(false);
                lblAdminBadge.setManaged(false);

                // Mở khóa lại nút bấm cho user thường
                btnBan.setDisable(false);
                btnUnban.setDisable(false);

                // Khôi phục lại style màu sắc nguyên bản của nút bấm
                btnBan.setStyle(
                        "-fx-background-color: rgba(231,76,60,0.20);" +
                                "-fx-text-fill: #e74c3c;" +
                                "-fx-border-color: #e74c3c;" +
                                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1; -fx-cursor: hand; -fx-font-weight: bold;"
                );
                btnUnban.setStyle(
                        "-fx-background-color: rgba(46,204,113,0.20);" +
                                "-fx-text-fill: #2ecc71;" +
                                "-fx-border-color: #2ecc71;" +
                                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1; -fx-cursor: hand; -fx-font-weight: bold;"
                );

                // Hiển thị nút dựa trên trạng thái Bị cấm (isBanned)
                boolean isBanned = user.getIsBanned() == 1;

                if (isBanned) {
                    // Nếu bị BAN: hiện nút Unban, ẩn nút Ban
                    btnBan.setVisible(false);
                    btnBan.setManaged(false); // Ở đây đặt false vì nút Unban sẽ hiện lên thay thế vào chỗ đó

                    btnUnban.setVisible(true);
                    btnUnban.setManaged(true);
                } else {
                    // Nếu bình thường: hiện nút Ban, ẩn nút Unban
                    btnBan.setVisible(true);
                    btnBan.setManaged(true);

                    btnUnban.setVisible(false);
                    btnUnban.setManaged(false);
                }
            }
            boolean isBanned = user.getIsBanned() == 1;
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
                if (user.isOnline()) {
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
                } else {
                    lblBanBadge.setText("● Offline");
                    lblBanBadge.setStyle(
                            "-fx-background-color: rgba(43, 43, 43, 1.0);" +
                                    "-fx-text-fill: #dee2e6;" +
                                    "-fx-background-radius: 6;" +
                                    "-fx-border-color: #dee2e6;" +
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
            }

            // ── Gán handler (truyền data vào controller) ──────────
            // Use the current `user` instance from updateItem instead of relying on
            // listView selection (which can be null when clicking a button inside the cell).
            btnBan.setOnAction(e -> handleBanUser(user));
            btnUnban.setOnAction(e -> handleUnbanUser(user));

            setGraphic(root);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
        }
    }

    private void handleUnbanUser(User user) {
        // Called from the cell with the specific User instance. Avoids relying on
        // listView selection which may be null when user clicks a button inside a cell.
        if (user == null) return;
        String email = user.getEmail();
        String token = SessionManager.getInstance().getServerToken();
        ApiClient.unbanUser(email, token);
    }

    private void handleBanUser(User user) {
        if (user == null) return;
        String email = user.getEmail();
        String token = SessionManager.getInstance().getServerToken();
        ApiClient.banUser(email, token);
    }
}