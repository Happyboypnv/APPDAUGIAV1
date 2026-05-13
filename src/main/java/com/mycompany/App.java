package com.mycompany;

import com.mycompany.action.PhienDauGiaScheduler;
import com.mycompany.utils.KetNoiCSDL;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public void stop() throws Exception {
        super.stop();
        PhienDauGiaScheduler.getInstance().shutdown();
    }
    @Override
    public void start(Stage primaryStage) {
        try {
            // Khởi tạo database trước khi load giao diện
            logger.info("🗄️ Đang khởi tạo database...");
            KetNoiCSDL.khoiTao();
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
            KhoLuuTruNguoiDungSQLite userStorage = new KhoLuuTruNguoiDungSQLite();
            userStorage.migratePlainTextPasswords();
            logger.info("✅ Migration hoàn thành!");

            // 1. Tải file giao diện đăng ký (SignUp.fxml)
            // Đảm bảo tên file khớp chính xác (phân biệt hoa thường)
            Parent root = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));

            // 2. Tạo Scene
            // Vì giao diện của bạn có bo góc (Radius), nên để Fill là TRANSPARENT
            // nếu bạn muốn làm cửa sổ không có khung Windows mặc định
            Scene scene = new Scene(root);

            // 3. Thiết lập Stage (Cửa sổ)
            primaryStage.setTitle("Sign Up Account");

            // Nếu bạn muốn bỏ thanh tiêu đề trắng của Windows để nhìn app "xịn" hơn:
             primaryStage.initStyle(StageStyle.TRANSPARENT);
             scene.setFill(Color.TRANSPARENT);

            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Giữ nguyên form đẹp như thiết kế
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