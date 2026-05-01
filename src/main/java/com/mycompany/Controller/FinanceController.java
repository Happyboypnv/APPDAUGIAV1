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

/**
 * FinanceController - Controller quản lý trang Tài chính (Finance Management)
 *
 * MỤC ĐÍCH:
 * - Quản lý các chức năng tài chính của người dùng
 * - Liên kết tài khoản ngân hàng
 * - Nạp tiền (deposit) và rút tiền (withdraw)
 * - Hiển thị số dư hiện tại
 * - Cập nhật thông tin tài chính vào hệ thống
 *
 * TÍNH NĂNG CHÍNH:
 * - Liên kết tài khoản ngân hàng với validation
 * - Nạp tiền vào tài khoản
 * - Rút tiền từ tài khoản
 * - Hiển thị số dư real-time
 * - Validation input (số tài khoản, số tiền)
 * - Cập nhật dữ liệu vào JSON file
 *
 * LUỒNG HOẠT ĐỘNG:
 * 1. Load trang → Hiển thị thông tin tài chính hiện tại
 * 2. Người dùng nhập thông tin ngân hàng → Validate → Lưu
 * 3. Người dùng nạp/rút tiền → Validate → Cập nhật số dư
 * 4. Tự động refresh UI sau mỗi thao tác
 */
public class FinanceController implements Initializable {

    // @FXML FIELDS - Các thành phần UI được inject từ FXML
    @FXML private TextField  bankAccount, // Số tài khoản ngân hàng
                             depositField, // Ô nhập số tiền nạp
                             withdrawField, // Ô nhập số tiền rút
                             currentBalanceField; // Hiển thị số dư hiện tại

    @FXML private ComboBox<String> banks; // Dropdown chọn ngân hàng

    @FXML private Button linkToBankButton, // Nút liên kết ngân hàng
                         depositButton, // Nút nạp tiền
                         withdrawButton; // Nút rút tiền

    @FXML ImageView editBankAccountBtn; // Nút edit số tài khoản

    /**
     * PHƯƠNG THỨC: initialize(URL url, ResourceBundle resourceBundle)
     * MỤC ĐÍCH: Khởi tạo trang Finance khi load
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Thêm danh sách ngân hàng vào ComboBox
     * 2. Lấy thông tin người dùng từ token
     * 3. Hiển thị số dư và thông tin ngân hàng đã liên kết
     *
     * @param url URL của FXML file
     * @param resourceBundle ResourceBundle (có thể null)
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 🔹 BƯỚC 1: Thêm danh sách ngân hàng vào ComboBox
        banks.getItems().addAll("MB Bank", "Techcombank", "BIDV", "Agribank", "VP Bank"); // Lựa chọn ngân hàng

        // 🔹 BƯỚC 2: Lấy thông tin người dùng từ JWT token
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);

        // 🔹 BƯỚC 3: Hiển thị thông tin nếu token hợp lệ
        if (info != null) {
            // Hiển thị số dư hiện tại từ token
            currentBalanceField.setText(String.valueOf(info.get("balance")));

            // Hiển thị số tài khoản ngân hàng nếu đã liên kết
            bankAccount.setText((String) info.get("bankAccount"));

            // Hiển thị tên ngân hàng đã liên kết nếu có
            String bankName = (String) info.get("bankName");
            if (bankName != null) {
                banks.setValue(bankName); // Set giá trị selected trong ComboBox
            }
        }
    }

    /**
     * PHƯƠNG THỨC: onClickedEditBankAccount()
     * MỤC ĐÍCH: Cho phép chỉnh sửa số tài khoản ngân hàng
     *
     * GIẢI THÍCH:
     * - Gọi FinanceAction để enable/disable editing mode
     * - Khi edit = true: cho phép nhập text
     * - Khi edit = false: disable nhập, chỉ view
     */
    @FXML
    public void onClickedEditBankAccount() {
        FinanceAction.getInstance().editBankAccount(bankAccount, true);
    }

    /**
     * PHƯƠNG THỨC: onClickedLinkToBank()
     * MỤC ĐÍCH: Liên kết tài khoản ngân hàng với validation
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Validate số tài khoản (10-15 chữ số)
     * 2. Validate đã chọn ngân hàng
     * 3. Disable editing mode
     * 4. Cập nhật thông tin vào hệ thống
     * 5. Hiển thị thông báo thành công
     */
    @FXML
    public void onClickedLinkToBank() {
        // 🔹 BƯỚC 1: Validate số tài khoản
        String bankAccountRegex = "^\\d{10,15}$"; // Regex: 10-15 chữ số
        String bankAcc = bankAccount.getText().trim();

        // Kiểm tra format số tài khoản
        if (!bankAcc.matches(bankAccountRegex) || bankAcc.isEmpty() || bankAcc == null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tài khoản ngân hàng không hợp lệ! Vui lòng nhập lại (10-15 chữ số).");
            return;
        }

        // Kiểm tra đã chọn ngân hàng
        String customBoxBankName = banks.getValue();
        if (customBoxBankName == null) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Vui lòng chọn ngân hàng!");
            return;
        }

        // 🔹 BƯỚC 2: Disable editing mode (lock field)
        FinanceAction.getInstance().editBankAccount(bankAccount, false);

        // 🔹 BƯỚC 3: Cập nhật thông tin ngân hàng
        Map<String, String> newBankAcc = new HashMap<>();
        newBankAcc.put("bankAccount", bankAcc);
        newBankAcc.put("bankName", customBoxBankName);

        // Lưu vào JSON file và cập nhật session
        CapNhatThongTinNguoiDung.getInstance().updateUser(newBankAcc);

        // 🔹 BƯỚC 4: Thông báo thành công
        HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Liên kết tài khoản ngân hàng thành công!");
    }

    /**
     * PHƯƠNG THỨC: onClickedDeposit()
     * MỤC ĐÍCH: Xử lý nạp tiền vào tài khoản
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy số tiền từ input field
     * 2. Validate số tiền (phải là số)
     * 3. Gọi FinanceAction.deposit() để xử lý
     * 4. Clear input field
     * 5. Refresh hiển thị số dư
     */
    @FXML
    public void onClickedDeposit() {
        // 🔹 BƯỚC 1: Lấy và validate số tiền
        String amountStr = depositField.getText().trim();
        try {
            // Chuyển string thành double, nếu lỗi → NumberFormatException
            double amount = Double.parseDouble(amountStr);

            // 🔹 BƯỚC 2: Gọi FinanceAction để xử lý nạp tiền
            FinanceAction.getInstance().deposit(amount);

        } catch (NumberFormatException e) {
            // Hiển thị lỗi nếu input không phải số
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền nạp không hợp lệ! Vui lòng nhập lại.");
        }

        // 🔹 BƯỚC 3: Clear input field sau khi nạp
        depositField.clear();

        // 🔹 BƯỚC 4: Refresh số dư hiển thị
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        if (info != null) {
            currentBalanceField.setText(String.valueOf(info.get("balance")));
        }
    }

    /**
     * PHƯƠNG THỨC: onClickedWithdraw()
     * MỤC ĐÍCH: Xử lý rút tiền từ tài khoản
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy số tiền từ input field
     * 2. Validate số tiền (phải là số)
     * 3. Gọi FinanceAction.withdraw() để xử lý
     * 4. Clear input field
     * 5. Refresh hiển thị số dư
     */
    @FXML
    public void onClickedWithdraw() {
        // 🔹 BƯỚC 1: Lấy và validate số tiền
        String amountStr = withdrawField.getText().trim();
        try {
            // Chuyển string thành double, nếu lỗi → NumberFormatException
            double amount = Double.parseDouble(amountStr);

            // 🔹 BƯỚC 2: Gọi FinanceAction để xử lý rút tiền
            FinanceAction.getInstance().withdraw(amount);

        } catch (NumberFormatException e) {
            // Hiển thị lỗi nếu input không phải số
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền rút không hợp lệ! Vui lòng nhập lại.");
        }

        // 🔹 BƯỚC 3: Clear input field sau khi rút
        withdrawField.clear();

        // 🔹 BƯỚC 4: Refresh số dư hiển thị
        String token = SessionManager.getInstance().getCurrentToken();
        Map<String, Object> info = TokenUtil.getUserInfoFromToken(token);
        if (info != null) {
            currentBalanceField.setText(String.valueOf(info.get("balance")));
        }
    }
}

