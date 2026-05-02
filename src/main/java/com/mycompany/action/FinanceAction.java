package com.mycompany.action;

import java.util.HashMap;
import java.util.Map;

import com.mycompany.models.NguoiDung;
import com.mycompany.utils.CapNhatThongTinNguoiDung;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import com.mycompany.utils.SessionManager;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

/**
 * FinanceAction - Class xử lý các thao tác tài chính
 *
 * MỤC ĐÍCH:
 * - Quản lý các chức năng tài chính của người dùng
 * - Xử lý nạp tiền và rút tiền
 * - Cập nhật số dư tài khoản
 * - Validate các thao tác tài chính
 *
 * KẾT NỐI VỚI CONTROLLER:
 * - FinanceController gọi các phương thức của FinanceAction
 * - FinanceController.onClickedDeposit() → FinanceAction.deposit()
 * - FinanceController.onClickedWithdraw() → FinanceAction.withdraw()
 * - FinanceController.onClickedEditBankAccount() → FinanceAction.editBankAccount()
 *
 * TÍNH NĂNG CHÍNH:
 * - Nạp tiền: Cộng tiền vào số dư, cập nhật database
 * - Rút tiền: Trừ tiền từ số dư (kiểm tra đủ tiền), cập nhật database
 * - Edit bank account: Enable/disable editing mode cho TextField
 * - Validation: Kiểm tra số tiền hợp lệ, số dư đủ để rút
 *
 * DESIGN PATTERN:
 * - Singleton: Chỉ có 1 instance FinanceAction
 * - Business Logic Layer: Tách logic kinh doanh ra khỏi UI controller
 */
public class FinanceAction {
    private static FinanceAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungSQLite();

    private FinanceAction() {}

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance FinanceAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của FinanceAction
     */
    public static FinanceAction getInstance() {
        if (instance == null) {
            instance = new FinanceAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o trang ca nhan
    }

    /**
     * editBankAccount(TextField field, boolean editing) - Bật/tắt chế độ chỉnh sửa tài khoản ngân hàng
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ FinanceController.onClickedEditBankAccount()
     * - Khi người dùng click nút edit bên cạnh ô nhập tài khoản
     *
     * CHỨC NĂNG:
     * - editing = true: Cho phép nhập text, focus vào field
     * - editing = false: Khóa field, không cho nhập
     *
     * @param field TextField cần chỉnh sửa (bankAccount field)
     * @param editing true = enable editing, false = disable editing
     */
    public void editBankAccount(TextField field, boolean editing) {
        field.setEditable(editing);
        field.setOpacity(1.0); // set lại ít mờ hơn
        if (editing) field.requestFocus(); // tự động focus vào ô khi bắt đầu chỉnh sửa
    }

    /**
     * deposit(double amount) - Xử lý nạp tiền vào tài khoản
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ FinanceController.onClickedDeposit()
     * - Khi người dùng nhập số tiền và click nút "Nạp tiền"
     *
     * QUY TRÌNH:
     * 1. Validate số tiền (> 0)
     * 2. Lấy người dùng hiện tại từ SessionManager
     * 3. Tính số dư mới = số dư cũ + số tiền nạp
     * 4. Tạo Map chứa update (balance)
     * 5. Gọi CapNhatThongTinNguoiDung.updateUser() để lưu
     * 6. Hiển thị thông báo thành công
     *
     * XỬ LÝ LỖI:
     * - Số tiền <= 0 → NumberFormatException → thông báo lỗi
     *
     * @param amount Số tiền cần nạp (phải > 0)
     */
    public void deposit(double amount) {
        try {
            if (amount <= 0) {
                throw new NumberFormatException();
            }
            NguoiDung currentUser = SessionManager.getInstance().getCurrentUser(); // lấy người dùng hiện tại
            double newBalance = currentUser.getSoDuKhaDung() + amount; // cộng thêm số tiền thêm vào
            Map<String, String> updateBalance = new HashMap<>();
            updateBalance.put("balance", String.valueOf(newBalance));
            CapNhatThongTinNguoiDung.getInstance().updateUser(updateBalance); // tạo map để update

            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công!");
        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi thông tin", "Số tiền nạp không hợp lệ! Vui lòng nhập lại.");
        }
    }

    /**
     * withdraw(double amount) - Xử lý rút tiền từ tài khoản
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ FinanceController.onClickedWithdraw()
     * - Khi người dùng nhập số tiền và click nút "Rút tiền"
     *
     * QUY TRÌNH:
     * 1. Validate số tiền (> 0)
     * 2. Lấy người dùng hiện tại và số dư hiện tại
     * 3. Kiểm tra số dư đủ để rút (amount <= currentBalance)
     * 4. Tính số dư mới = số dư cũ - số tiền rút
     * 5. Tạo Map chứa update (balance)
     * 6. Gọi CapNhatThongTinNguoiDung.updateUser() để lưu
     * 7. Hiển thị thông báo thành công
     *
     * XỬ LÝ LỖI:
     * - Số tiền <= 0 → NumberFormatException → thông báo lỗi
     * - Số dư không đủ → Alert lỗi "Số tiền rút vượt quá số dư hiện tại"
     *
     * @param amount Số tiền cần rút (phải > 0 và <= số dư hiện tại)
     */
    public void withdraw(double amount) {
        NguoiDung currentUser = SessionManager.getInstance().getCurrentUser();
        double currentBalance = currentUser.getSoDuKhaDung();
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