package com.mycompany.Controller;

import com.mycompany.utils.CapNhatThongTinNguoiDung;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.action.ProfileAction;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.TokenUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.Initializable;

import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.shape.Circle;


import javax.swing.*;

public class ProfileController implements Initializable {
    @FXML private ImageView avatarPicture;

    @FXML private TextField nameField, emailField, birthField, phoneField, addressField;
    @FXML private ImageView nameEditBtn, addressEditBtn, phoneEditBtn;
    @FXML private Button saveButton, changePasswordButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);

        if (info != null) {
            nameField.setText((String) info.get("name"));
            emailField.setText((String) info.get("email"));
            birthField.setText((String) info.get("birth"));
            phoneField.setText((String) info.get("phone"));
            addressField.setText((String) info.get("address"));
        }

        Image avt = new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm());
        avatarPicture.setImage(avt);
        Circle clip = new Circle(75, 75, 75);
        avatarPicture.setClip(clip); // tạo Avatar hình tròn, cho vào ImageView
    }

    @FXML
    public void onClickedEditName() {
        ProfileAction.getInstance().editField(nameField, true);
    }

    @FXML
    public void onClickedEditPhone() {
        ProfileAction.getInstance().editField(phoneField, true);
    }

    @FXML
    public void onClickedEditAddress() {
        ProfileAction.getInstance().editField(addressField, true);
    }

    @FXML
    public void onClickedSaveButton() {
        String newName = nameField.getText().trim();
        String newPhone = phoneField.getText().trim();
        String newAddress = addressField.getText().trim();
        try {
            ProfileAction.getInstance().checkInfo(newName, newPhone, newAddress);
            // Nếu không có lỗi, tiến hành lưu thông tin mới (có thể gọi action để lưu vào file hoặc database)
            // Sau khi lưu thành công, chuyển về chế độ không chỉnh sửa
            ProfileAction.getInstance().editField(nameField, false);
            ProfileAction.getInstance().editField(phoneField, false);
            ProfileAction.getInstance().editField(addressField, false);

            Map<String,String> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("phone", newPhone);
            updates.put("address", newAddress);
            CapNhatThongTinNguoiDung.getInstance().updateUser(updates);

            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thông tin cá nhân đã được cập nhật!");
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", e.getMessage());
        }
    }

}