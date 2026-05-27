package com.mycompany.action;

import com.mycompany.models.User;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

import com.mycompany.utils.SessionManager;

public class HandleNavigationAndAlert {

    private HandleNavigationAndAlert() {};

    private static volatile HandleNavigationAndAlert instance;
    public static HandleNavigationAndAlert getInstance() {
        if (instance == null) {
            synchronized (HandleNavigationAndAlert.class) {
                if (instance == null) instance = new HandleNavigationAndAlert();
            }
        }
        return instance;
    }

    public void handleGoToHome(Event event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        StackPane homeRoot = FXMLLoader.load(getClass().getResource("/view/Home.fxml"));
        Scene homeScene = new Scene(homeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    public void handleGoToSignIn(ActionEvent event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignIn.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    public void handleGoToSignUp(ActionEvent event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        Parent signUpRoot = FXMLLoader.load(getClass().getResource("/view/SignUp.fxml"));
        Scene signUpScene = new Scene(signUpRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();
    }

    public void goToCreateAuction(Event event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        javafx.scene.layout.StackPane root =
                FXMLLoader.load(getClass().getResource("/view/CreateAuction.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.show();
    }

    public void goToProfile(Stage stage) throws IOException {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn cần đăng nhập để xem trang cá nhân!");
            return;
        }
        // Đã sửa đường dẫn thêm /resources
        Parent profileRoot = FXMLLoader.load(getClass().getResource("/view/Profile.fxml"));
        Scene profileScene = new Scene(profileRoot);

        stage.setScene(profileScene);
        stage.show();
    }

    public void goToFinance(Event event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        StackPane financeRoot = FXMLLoader.load(getClass().getResource("/view/Finance.fxml"));
        Scene homeScene = new Scene(financeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(homeScene);
        window.show();
    }

    public void goToTransactionHistory(Event event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        StackPane root = FXMLLoader.load(getClass().getResource("/view/TransactionHistory.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.show();
    }

    public void handleGoToAdminHome(ActionEvent event) throws IOException {
        Node node = (Node) event.getSource();
        Scene scene = node.getScene();
        Parent root = FXMLLoader.load(getClass().getResource("/view/Admin_Home.fxml"));
        Stage stage = (Stage) scene.getWindow();
        scene.setRoot(root);
    }

    public void goToAdminProfile(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/AdminProfile.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void goToUserManagement(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/UserManagement.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void goToAuctionRequests(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/ReviewAuctionRequest.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void handleGoToReviewAuction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/ReviewAuctionRequest.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void goToAuctionReview(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/ReviewPage.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void goToChangePassword(ActionEvent event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        StackPane changeRoot = FXMLLoader.load(getClass().getResource("/view/ChangePassword.fxml"));
        Scene scene = new Scene(changeRoot);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.show();
    }

    public void goToBiddingRoom(javafx.scene.input.MouseEvent event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        javafx.scene.layout.StackPane root = FXMLLoader.load(
                getClass().getResource("/view/BiddingRoom.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.show();
    }

    public void goToCreatePhienDauGia(Event event) throws IOException {
        // Đã sửa đường dẫn thêm /resources
        javafx.scene.layout.StackPane root = FXMLLoader.load(
                getClass().getResource("/view/CreatePhienDauGia.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.show();
    }

    public void showAlert (Alert.AlertType alertType, String title, String message){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleGoToAdminHome(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/Admin_Home.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void handleGoToUserList(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/UserManagement.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void handleGoToReviewAuction(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/ReviewAuctionRequest.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    public void goToAdminProfile(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/AdminProfile.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}