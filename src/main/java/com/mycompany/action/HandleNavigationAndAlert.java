package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.NguoiDung;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader; // Quan trọng
import javafx.scene.Node; // Quan trọng
import javafx.scene.Parent; // Quan trọng
import javafx.scene.Scene; // Quan trọng
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage; // Quan trọng
import java.io.IOException; // Quan trọng
import java.time.*;
import com.mycompany.utils.SessionManager;
import com.mycompany.exception.Login.*;

public class HandleNavigationAndAlert { // Class này đảm nhận nhiệm vụ di chuyển đến các trang khác nhau + in thông báo
    private static HandleNavigationAndAlert instance;

    private HandleNavigationAndAlert() {};

    public static HandleNavigationAndAlert getInstance() {
        if (instance==null){
            instance = new HandleNavigationAndAlert();
        }
        return instance;
    }

    public void handleGoToHome(Event event) throws IOException { // phải dùng Event vì phải nhận cả MouseEvent (nút home) và ActionEvent (còn lại)
        StackPane homeRoot = FXMLLoader.load(getClass().getResource("/view/Home.fxml"));
        Scene homeScene = new Scene(homeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    public void handleGoToSignIn(ActionEvent event) throws IOException {
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    public void handleGoToSignUp(ActionEvent event) throws IOException {
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    public void goToProfile(Stage stage) throws IOException {
        NguoiDung nguoiDung = SessionManager.getInstance().getCurrentUser();
        if (nguoiDung == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để xem trang cá nhân!");
            return;
        }
        Parent profileRoot = FXMLLoader.load(getClass().getResource("/view/Profile.fxml"));
        Scene profileScene = new Scene(profileRoot);

        stage.setScene(profileScene);
        stage.show();
        // Do menu item khong phai la node => k set stage truc tiep tu no duoc ma phai lay qua ImageView ben Controller
    }

    public void goToFinance(Event event) throws IOException {
        StackPane financeRoot = FXMLLoader.load(getClass().getResource("/view/Finance.fxml"));
        Scene homeScene = new Scene(financeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    public void showAlert (Alert.AlertType alertType, String title, String message){
        Alert alert = new Alert(alertType); // 1. Khởi tạo một hộp thoại mới
        alert.setTitle(title);              // 2. Đặt tiêu đề cho cửa sổ Pop-up
        alert.setHeaderText(null);          // 3. Tiêu đề phụ (để null cho gọn)
        alert.setContentText(message);      // 4. Nội dung chi tiết cần thông báo
        alert.showAndWait();                // 5. Hiển thị lên và dừng code lại
    }
}