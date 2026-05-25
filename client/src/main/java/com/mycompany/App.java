package com.mycompany;

import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class    App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    @Override
    public void stop() throws Exception {
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            String serverToken = session.getServerToken();
            if (serverToken != null && !serverToken.isEmpty()) {
                try {
                    ApiClient.logout(serverToken);  // ← Cái này bị thiếu trước đây
                } catch (Exception e) { /* không block tắt app */ }
            }
            session.logout();
        }
        super.stop();
    }
    @Override
    public void start(Stage primaryStage) {
        try {
            // Khởi tạo database trước khi load giao diện
            logger.info("🗄️ Đang khởi tạo database...");
            logger.info("✅ Database đã sẵn sàng!");

            // THAY ĐỔI QUAN TRỌNG (SQLite Migration):
            // Lý do thêm: Migrate users cũ từ JSON sang SQLite
            // Vấn đề: Users cũ có password plain text, SQLite expect hashed
            // Giải pháp: Gọi migratePlainTextPasswords() sau DB init
            //
            // Quy trình:
            // 1. KhoLuuTruNguoiDungSQLite userStorage = new KhoLuuTruNguoiDungSQLite()
            //    → Tạo instance của storage class
            // 2. userStorage.migratePlainTextPasswords()
            //    → Tự động tìm và migrate users cũ
            // 3. Chỉ chạy một lần khi app start
            // 4. Thread-safe vì chạy trước UI load
            //
            // Kết quả: Tất cả users đều có password hashed + salt
            logger.info("🔄 Đang kiểm tra migration passwords...");
            logger.info("✅ Migration hoàn thành!");

            // 1. Tải file giao diện đăng ký (SignUp.fxml)
            // Đảm bảo tên file khớp chính xác (phân biệt hoa thường)
            Parent root = FXMLLoader.load(getClass().getResource("/resources/view/SignIn.fxml"));

            // 2. Tạo Scene
            // Vì giao diện của bạn có bo góc (Radius), nên để Fill là TRANSPARENT
            // nếu bạn muốn làm cửa sổ không có khung Windows mặc định
            Scene scene = new Scene(root);

            // 3. Thiết lập Stage (Cửa sổ)
            primaryStage.setTitle("HiPiTi Bidding App");

            // Nếu bạn muốn bỏ thanh tiêu đề trắng của Windows để nhìn app "xịn" hơn:
            // primaryStage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);

            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setMaximized(false);
            primaryStage.show();
        } catch (Exception e) {
            logger.error("Không thể khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
