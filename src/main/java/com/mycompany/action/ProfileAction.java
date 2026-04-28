package com.mycompany.action;

import com.mycompany.exception.Login.*;
import java.time.*;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.KhoLuuTruNguoiDungJson;
import com.mycompany.exception.Login.*;
import javafx.scene.control.TextField;

/**
 * ProfileAction - Class xử lý các thao tác trên trang Profile
 *
 * MỤC ĐÍCH:
 * - Quản lý logic chỉnh sửa thông tin cá nhân
 * - Validate dữ liệu profile trước khi lưu
 * - Enable/disable editing mode cho các trường
 *
 * KẾT NỐI VỚI CONTROLLER:
 * - ProfileController gọi các phương thức của ProfileAction
 * - ProfileController.onClickedEditName() → ProfileAction.editField()
 * - ProfileController.onClickedEditPhone() → ProfileAction.editField()
 * - ProfileController.onClickedEditAddress() → ProfileAction.editField()
 * - ProfileController.onClickedSaveButton() → ProfileAction.checkInfo()
 *
 * TÍNH NĂNG CHÍNH:
 * - Edit field: Bật/tắt chế độ chỉnh sửa cho TextField
 * - Validation: Kiểm tra name, phone, address theo regex
 * - Error handling: Throw custom exceptions với thông báo rõ ràng
 *
 * DESIGN PATTERN:
 * - Singleton: Chỉ có 1 instance ProfileAction
 * - Business Logic Layer: Tách logic validation ra khỏi UI
 */
public class ProfileAction {
    private static ProfileAction instance;
    private final IKhoLuuTruNguoiDung khoLuuTruNguoiDung = new KhoLuuTruNguoiDungJson();

    private ProfileAction() {}

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance ProfileAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của ProfileAction
     */
    public static ProfileAction getInstance() {
        if (instance == null) {
            instance = new ProfileAction();
        }
        return instance; // Chi nen co 1 doi tuong dam nhan viec xu ly thao tac o trang ca nhan
    }

    /**
     * checkInfo(String name, String phoneNumber, String address) - Validate thông tin profile
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ ProfileController.onClickedSaveButton()
     * - Validate trước khi lưu thông tin mới
     *
     * VALIDATION RULES:
     * - Name: 2-30 ký tự, hỗ trợ tiếng Việt, space, dấu
     * - Phone: 10-15 chữ số
     * - Address: Format "Xã, Thành phố" (2 phần cách nhau bởi dấu phẩy)
     *
     * @param name Họ tên mới
     * @param phoneNumber Số điện thoại mới
     * @param address Địa chỉ mới
     * @throws UserNameException nếu tên không hợp lệ
     * @throws PhoneNumberException nếu số điện thoại không hợp lệ
     */
    public void checkInfo(String name, String phoneNumber,String address) throws UserNameException, PhoneNumberException {
        if(name == null || name.isEmpty()) throw new UserNameException("Tên đang bỏ trống!");
        String nameRegex = "^[\\p{L} .'-]{2,30}$";
        // p{L} : cho phep ngon ngu tieng Viet
        //  .'- : cho phep dau cach, dau cham, dau nhay don, dau gach ngang
        // {2, 30} : do dai trong khoang thu 2 den 30
        if(!name.matches(nameRegex)) throw new UserNameException("Tên không hợp lệ");
        // check xem chuoi name co matches voi luat Regex khong

        if (phoneNumber == null || phoneNumber.isEmpty()) throw new PhoneNumberException("Số điện thoại đang bỏ trống!");
        String phoneRegex = "^\\d{10,15}$";
        // ^: bắt đầu chuỗi
        // \\d: chỉ cho phép chữ số
        // {10,15}: độ dài từ 10 đến 15 chữ số
        // $: kết thúc chuỗi
        if (!phoneNumber.matches(phoneRegex)) throw new PhoneNumberException("Số điện thoại không hợp lệ!");

        if (address == null || address.isEmpty()) throw new UserNameException("Địa chỉ đang bỏ trống!");
        String addressRegex = "^[\\p{L}\\s]+,\\s*[\\p{L}\\s]+$";
        // ^: bắt đầu chuỗi
        // p{L} : cho phep ngon ngu tieng Viet
        // [^,]+: mỗi phần phải có ít nhất 1 ký tự và không chứa dấu phẩy
        // ,\\s*: cho phép có hoặc không có khoảng trắng sau dấu phẩy
        // $: kết thúc chuỗi
        // Yêu cầu địa chỉ phải đúng nhất 2 phần (xã + thành phố), mỗi phần không chứa dấu phẩy, và được phân tách bằng một dấu phẩy (có thể có khoảng trắng sau dấu phẩy)
        if (!address.matches(addressRegex)) throw new UserNameException("Địa chỉ không hợp lệ! Vui lòng nhập đúng định dạng: 'Xã, Thành phố'");
    }

    /**
     * editField(TextField field, boolean editing) - Bật/tắt chế độ chỉnh sửa cho TextField
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ ProfileController khi click các nút edit
     * - ProfileController.onClickedEditName() → editField(nameField, true)
     * - ProfileController.onClickedEditPhone() → editField(phoneField, true)
     * - ProfileController.onClickedSaveButton() → editField(all fields, false)
     *
     * CHỨC NĂNG:
     * - editing = true: Cho phép nhập text, focus vào field
     * - editing = false: Khóa field, không cho nhập
     *
     * @param field TextField cần chỉnh sửa
     * @param editing true = enable editing, false = disable editing
     */
    public void editField(TextField field, boolean editing) {
        field.setEditable(editing);
        field.setOpacity(1.0); // set lại ít mờ hơn
        if (editing) field.requestFocus(); // tự động focus vào ô khi bắt đầu chỉnh sửa
    }

}