package com.mycompany.action;

import com.mycompany.exception.Login.*;
import javafx.fxml.FXML; // Quan trọng
import javafx.fxml.FXMLLoader; // Quan trọng
import javafx.scene.Parent; // Quan trọng
import javafx.scene.Scene; // Quan trọng // Để xử lý dữ liệu ngày tháng (năm/tháng/ngày)
import javafx.scene.control.Alert;
import javafx.stage.Stage; // Quan trọng
import java.io.IOException; // Quan trọng
import java.time.*;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.utils.SessionManager;
import com.mycompany.exception.Login.*;

import java.util.concurrent.locks.*; // Handle nhieu nguoi dang nhap cung luc

public class HomeAction {
    private static HomeAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();
    private final Lock lock = new ReentrantLock();
    private HomeAction() {};

    public static HomeAction getInstance() {
        if (instance==null) {
            instance = new HomeAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o man hinh chinh
    }

    @FXML
    public void dangXuat(Stage stage) throws IOException {
        if (SessionManager.getInstance().isLoggedIn()) {
            SessionManager.getInstance().logout();
            Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
            Scene signUpScene = new Scene(signUpRoot);
            stage.setScene(signUpScene);
            stage.show();
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bạn đã đăng xuất thành công!");
        } else {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa đăng nhập!");
        } // Nút đăng xuất k phải node nên cũng cần stage riêng từ ImageView
    }

}