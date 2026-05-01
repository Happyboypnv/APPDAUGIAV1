package com.mycompany;

import com.mycompany.utils.KetNoiCSDL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Khởi tạo database trước khi load giao diện
            System.out.println("🗄️ Đang khởi tạo database...");
            KetNoiCSDL.khoiTao();
            System.out.println("✅ Database đã sẵn sàng!");

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
            System.err.println("Không thể khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}