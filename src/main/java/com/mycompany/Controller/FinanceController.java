package com.mycompany.Controller;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.mycompany.action.FinanceAction;
import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.CapNhatThongTinNguoiDung;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.TokenUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import javafx.scene.image.ImageView;

public class FinanceController implements Initializable {
    @FXML private TextField  bankAccount,depositField, withdrawField, currentBalanceField;

    @FXML private ComboBox<String> banks;

    @FXML private Button linkToBankButton, depositButton, withdrawButton;

    @FXML ImageView editBankAccountBtn;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        banks.getItems().addAll("MB Bank", "Techcombank", "BIDV", "Agribank", "VP Bank"); // Lựa chọn ngân hàng

        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);

        if (info != null) {
            currentBalanceField.setText(String.valueOf(info.get("balance")));// Hiển thị số dư hiện tại
            bankAccount.setText((String) info.get("bankAccount")); // Hiển thị số tài khoản ngân hàng nếu đã liên kết
            String bankName = (String) info.get("bankName");
            if (bankName != null) {
                banks.setValue(bankName); // Hiển thị tên ngân hàng đã liên kết nếu có
            }
        }
    }

    @FXML
    public void onClickedEditBankAccount() {
        FinanceAction.getInstance().editBankAccount(bankAccount, true);
    }

    @FXML
    public void onClickedLinkToBank() {
        String bankAccountRegex = "^\\d{10,15}$";
        String bankAcc = bankAccount.getText().trim();
        String customBoxBankName = banks.getValue();
        if (!bankAcc.matches(bankAccountRegex) || bankAcc.isEmpty() || bankAcc == null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tài khoản ngân hàng không hợp lệ! Vui lòng nhập lại (10-15 chữ số).");
            return;
        } else if (customBoxBankName == null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Vui lòng chọn ngân hàng!");
            return;
        }
        FinanceAction.getInstance().editBankAccount(bankAccount, false);

        Map<String, String> newBankAcc = new HashMap<>();
        newBankAcc.put("bankAccount", bankAcc);
        newBankAcc.put("bankName",customBoxBankName);
        CapNhatThongTinNguoiDung.getInstance().updateUser(newBankAcc); // Cập nhật số tài khoản mới vào JSON
        HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Liên kết tài khoản ngân hàng thành công!");
    }

    @FXML
    public void onClickedDeposit() {
        String amountStr = depositField.getText().trim();
        try {
            FinanceAction.getInstance().deposit(Double.parseDouble(amountStr));
        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền nạp không hợp lệ! Vui lòng nhập lại.");
        }
        depositField.clear(); // Xóa nội dung trong ô điền sau khi nạp tiền

        // Cập nhật lại số dư sau khi nạp tiền thành công
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        if (info != null) {
            currentBalanceField.setText(String.valueOf(info.get("balance")));
        }
    }

    @FXML
    public void onClickedWithdraw() {
        String amountStr = withdrawField.getText().trim();
        try {
            FinanceAction.getInstance().withdraw(Double.parseDouble(amountStr));
        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền rút không hợp lệ! Vui lòng nhập lại.");
        }
        withdrawField.clear(); // Xóa nội dung trong ô điền sau khi rút tiền

        // Cập nhật lại số dư sau khi nạp tiền thành công
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        if (info != null) {
            currentBalanceField.setText(String.valueOf(info.get("balance")));
        }
    }
}