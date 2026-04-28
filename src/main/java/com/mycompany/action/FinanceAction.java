package com.mycompany.action;

import com.mycompany.Controller.FinanceController;
import com.mycompany.exception.Login.*;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

import com.mycompany.models.NguoiDung;
import com.mycompany.utils.CapNhatThongTinNguoiDung;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.exception.Login.*;
import com.mycompany.utils.SessionManager;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class FinanceAction {
    private static FinanceAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();

    private FinanceAction() {}

    public static FinanceAction getInstance() {
        if (instance == null) {
            instance = new FinanceAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o trang ca nhan
    }

    public void editBankAccount(TextField field, boolean editing) {
        field.setEditable(editing);
        field.setOpacity(1.0); // set lại ít mờ hơn
        if (editing) field.requestFocus(); // tự động focus vào ô khi bắt đầu chỉnh sửa
    }

    public void deposit(double amount) {
        try {
            if (amount <= 0) {
                throw new NumberFormatException();
            }
            NguoiDung currentUser = SessionManager.getInstance().getCurrentUser(); // lấy người dùng hiện tại
            double newBalance = currentUser.laySoDuKhaDung() + amount; // cộng thêm số tiền thêm vào
            Map<String, String> updateBalance = new HashMap<>();
            updateBalance.put("balance", String.valueOf(newBalance));
            CapNhatThongTinNguoiDung.getInstance().updateUser(updateBalance); // tạo map để update

            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công!");
        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền nạp không hợp lệ! Vui lòng nhập lại.");
        }
    }

    public void withdraw(double amount) {
        NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();
        double currentBalance = currentUser.laySoDuKhaDung();
        try {
            if (amount <=0) {
                throw new NumberFormatException();
            } else if (amount > currentBalance) {
                HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền rút vượt quá số dư hiện tại!");
                return;
            }
            double newBalance = currentBalance - amount;
            Map<String, String> updateBalance = new HashMap<>();
            updateBalance.put("balance", String.valueOf(newBalance));
            CapNhatThongTinNguoiDung.getInstance().updateUser(updateBalance); // tạo map để update
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Rút tiền thành công!");
        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền rút không hợp lệ! Vui lòng nhập lại.");
        }
    }
}