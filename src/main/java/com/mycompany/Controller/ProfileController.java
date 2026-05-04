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

import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.shape.Circle;

import javax.swing.*;

/**
 * ProfileController - Controller quản lý trang Profile (Thông tin cá nhân)
 *
 * MỤC ĐÍCH:
 * - Quản lý giao diện và logic của trang profile
 * - Cho phép người dùng xem và chỉnh sửa thông tin cá nhân
 * - Validate và lưu thay đổi vào hệ thống
 * - Hiển thị avatar và các trường thông tin
 *
 * TÍNH NĂNG CHÍNH:
 * - Hiển thị thông tin cá nhân từ token/session
 * - Cho phép edit từng field riêng biệt (name, phone, address)
 * - Validate input trước khi lưu
 * - Lưu thay đổi vào JSON file
 * - Hiển thị avatar hình tròn
 * - Thông báo thành công/lỗi
 *
 * LUỒNG HOẠT ĐỘNG:
 * 1. Load trang → Hiển thị thông tin hiện tại
 * 2. Click edit button → Enable editing cho field đó
 * 3. Nhập thông tin mới → Click Save
 * 4. Validate → Lưu vào hệ thống → Thông báo
 * 5. Disable editing mode
 */
public class ProfileController implements Initializable {

    // @FXML FIELDS - Các thành phần UI được inject từ FXML
    @FXML private ImageView avatarPicture; // Hình ảnh avatar (hình tròn)

    @FXML private TextField nameField, // Họ tên
                             emailField, // Email (read-only)
                             birthField, // Ngày sinh (read-only)
                             phoneField, // Số điện thoại
                             addressField; // Địa chỉ

    @FXML private ImageView nameEditBtn, // Nút edit tên
                            addressEditBtn, // Nút edit địa chỉ
                            phoneEditBtn; // Nút edit số điện thoại

    @FXML private Button saveButton, // Nút lưu thay đổi
                         changePasswordButton; // Nút đổi mật khẩu (chưa implement)

    /**
     * PHƯƠNG THỨC: initialize(URL url, ResourceBundle resourceBundle)
     * MỤC ĐÍCH: Khởi tạo trang Profile khi load
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy thông tin người dùng từ JWT token
     * 2. Hiển thị thông tin vào các TextField
     * 3. Load và hiển thị avatar hình tròn
     *
     * @param url URL của FXML file
     * @param resourceBundle ResourceBundle (có thể null)
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 🔹 BƯỚC 1: Lấy thông tin người dùng từ JWT token
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);

        // 🔹 BƯỚC 2: Hiển thị thông tin nếu token hợp lệ
        if (info != null) {
            nameField.setText((String) info.get("name"));
            emailField.setText((String) info.get("email"));
            birthField.setText((String) info.get("birth"));
            phoneField.setText((String) info.get("phone"));
            addressField.setText((String) info.get("address"));
        }

        // 🔹 BƯỚC 3: Load và hiển thị avatar
        Image avt = new Image(getClass().getResource("/image/default_avatar.jpg").toExternalForm());
        avatarPicture.setImage(avt);

        // Tạo hình tròn cho avatar (centerX, centerY, radius)
        Circle clip = new Circle(75, 75, 75);
        avatarPicture.setClip(clip); // Cắt avatar thành hình tròn
    }

    /**
     * PHƯƠNG THỨC: onClickedEditName()
     * MỤC ĐÍCH: Cho phép chỉnh sửa trường "Họ tên"
     *
     * GIẢI THÍCH:
     * - Gọi ProfileAction để enable editing mode cho nameField
     * - Khi edit = true: cho phép nhập text
     * - Khi edit = false: disable nhập, chỉ view
     */
    @FXML
    public void onClickedEditName() {
        ProfileAction.getInstance().editField(nameField, true);
    }

    /**
     * PHƯƠNG THỨC: onClickedEditPhone()
     * MỤC ĐÍCH: Cho phép chỉnh sửa trường "Số điện thoại"
     *
     * GIẢI THÍCH:
     * - Gọi ProfileAction để enable editing mode cho phoneField
     * - Khi edit = true: cho phép nhập text
     * - Khi edit = false: disable nhập, chỉ view
     */
    @FXML
    public void onClickedEditPhone() {
        ProfileAction.getInstance().editField(phoneField, true);
    }

    /**
     * PHƯƠNG THỨC: onClickedEditAddress()
     * MỤC ĐÍCH: Cho phép chỉnh sửa trường "Địa chỉ"
     *
     * GIẢI THÍCH:
     * - Gọi ProfileAction để enable editing mode cho addressField
     * - Khi edit = true: cho phép nhập text
     * - Khi edit = false: disable nhập, chỉ view
     */
    @FXML
    public void onClickedEditAddress() {
        ProfileAction.getInstance().editField(addressField, true);
    }

    /**
     * PHƯƠNG THỨC: onClickedSaveButton()
     * MỤC ĐÍCH: Lưu tất cả thay đổi thông tin cá nhân
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy giá trị từ các TextField
     * 2. Validate thông tin (gọi ProfileAction.checkInfo)
     * 3. Disable editing mode cho tất cả fields
     * 4. Tạo Map chứa updates
     * 5. Gọi CapNhatThongTinNguoiDung để lưu
     * 6. Hiển thị thông báo thành công
     *
     * XỬ LÝ LỖI:
     * - Nếu validation fail → hiển thị alert lỗi
     * - Nếu lưu thành công → hiển thị alert thành công
     */
    @FXML
    public void onClickedSaveButton() {
        // 🔹 BƯỚC 1: Lấy giá trị từ các field
        String newName = nameField.getText().trim();
        String newPhone = phoneField.getText().trim();
        String newAddress = addressField.getText().trim();

        try {
            // 🔹 BƯỚC 2: Validate thông tin
            ProfileAction.getInstance().checkInfo(newName, newPhone, newAddress);

            // 🔹 BƯỚC 3: Disable editing mode cho tất cả fields
            ProfileAction.getInstance().editField(nameField, false);
            ProfileAction.getInstance().editField(phoneField, false);
            ProfileAction.getInstance().editField(addressField, false);

            // 🔹 BƯỚC 4: Tạo Map chứa các thay đổi
            Map<String,String> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("phone", newPhone);
            updates.put("address", newAddress);

            // 🔹 BƯỚC 5: Lưu vào hệ thống
            CapNhatThongTinNguoiDung.getInstance().updateUser(updates);

            // 🔹 BƯỚC 6: Thông báo thành công
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thông tin cá nhân đã được cập nhật!");

        } catch (Exception e) {
            // Xử lý lỗi: hiển thị thông báo lỗi
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", e.getMessage());
        }
    }

    @FXML
    public void onClickedChangePassword(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().goToChangePassword(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Tải giao diện đổi mật khẩu không thành công!");
        }
    }

}