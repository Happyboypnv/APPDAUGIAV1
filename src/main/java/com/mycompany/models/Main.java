package com.mycompany.models;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        User seller = new User("phongvan");
        User buyer1 = new User("phongnguyen");
        User buyer2 = new User("happyboy");
        Product luv = new Product("001", "Ao da");
        PhienDauGia phienDauGia = new PhienDauGia("P01", "Dau gia ao luv", luv, (double)1.0E7F, seller);
        phienDauGia.batDauPhien();
        phienDauGia.setOnAutionFinished(() -> {
            System.out.println("\n--- KẾT QUẢ PHIÊN ĐẤU GIÁ ---");
            System.out.println("Người chiến thắng: " + phienDauGia.getWinner().getName());
            System.out.printf("Giá chốt: %,.0f VNĐ\n", phienDauGia.getGiaChot());
            System.out.println("Tổng thời gian diễn ra: " + phienDauGia.getThoiGianDauGia());
            GiaoDich giaoDich = new GiaoDich("001", phienDauGia);
            System.out.println(giaoDich.toString());
            Platform.exit();
        });
        Timeline timeline = new Timeline(new KeyFrame[]{new KeyFrame(Duration.seconds((double)4.0F), (e) -> {
            System.out.println("Nguoi A tra gia 15000000");
            phienDauGia.nguoiChoiTraGia(buyer1, (double)1.5E7F);
        }, new KeyValue[0]), new KeyFrame(Duration.seconds((double)9.0F), (e) -> {
            System.out.println("Nguoi B tra gia 20000000");
            phienDauGia.nguoiChoiTraGia(buyer2, (double)2.0E7F);
        }, new KeyValue[0])});
        timeline.play();
    }
}
