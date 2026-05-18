package com.mycompany.action;

import com.mycompany.exception.Login.*;
import com.mycompany.models.User;
import com.mycompany.utils.IUserRepository;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.UserRepositorySQLite;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private final IUserRepository khoLuuTruNguoiDung = new UserRepositorySQLite();
    private static final Logger logger = LoggerFactory.getLogger(ProfileAction.class);

    private ProfileAction() {}

    /**
     * getInstance() - Singleton pattern
     * Đảm bảo chỉ có 1 instance ProfileAction trong toàn bộ ứng dụng
     *
     * @return Instance duy nhất của ProfileAction
     */
    private static volatile ProfileAction instance;
    public static ProfileAction getInstance() {
        if (instance == null) {
            synchronized (ProfileAction.class) {
                if (instance == null) instance = new ProfileAction();
            }
        }
        return instance;
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
    public void checkInfo(String name, String phoneNumber,String address) throws UserNameException, PhoneNumberException, AddressException{
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

        if (address == null || address.isEmpty()) throw new AddressException("Địa chỉ đang bỏ trống!");
        String addressRegex = "^[^,]+,\\s*[^,]+$"; // <--- Chỉ dùng 2 dấu gạch chéo
        // ^: bắt đầu chuỗi
        // p{L} : cho phep ngon ngu tieng Viet
        // [^,]+: mỗi phần phải có ít nhất 1 ký tự và không chứa dấu phẩy
        // ,\\s*: cho phép có hoặc không có khoảng trắng sau dấu phẩy
        // $: kết thúc chuỗi
        // Yêu cầu địa chỉ phải đúng nhất 2 phần (xã + thành phố), mỗi phần không chứa dấu phẩy, và được phân tách bằng một dấu phẩy (có thể có khoảng trắng sau dấu phẩy)
        if (!address.matches(addressRegex)) throw new AddressException("Địa chỉ không hợp lệ! Vui lòng nhập đúng định dạng: 'Xã, Thành phố'");
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

    /**
     * changeAvatar(Stage stage) - Cho phép người dùng chọn và lưu avatar mới
     *
     * KẾT NỐI VỚI CONTROLLER:
     * - Được gọi từ ProfileController khi click vào avatar
     * - ProfileController.onClickedChangeAvatar() → changeAvatar(stage)
     *
     * CHỨC NĂNG:
     * 1. Mở FileChooser để chọn file hình ảnh
     * 2. Validate: chỉ cho phép .jpg, .jpeg, .png
     * 3. Copy file hình ảnh vào resources/image folder
     * 4. Trả về đường dẫn hình ảnh mới (relative path)
     *
     * @param stage Stage của cửa sổ ứng dụng
     * @return Đường dẫn hình ảnh mới (ví dụ: "image/avatar_user@email.jpg"), hoặc null nếu user cancel
     * @throws IOException nếu có lỗi copy file
     */
    public String changeAvatar(Stage stage) throws IOException, URISyntaxException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh avatar");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            // User cancelled the dialog
            return null;
        }

        // Validate file extension
        String fileName = selectedFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
            throw new IOException("Chỉ hỗ trợ các định dạng: .jpg, .jpeg, .png");
        }

        // Validate file size (max 5MB)
        long fileSizeInBytes = selectedFile.length();
        long maxSizeInBytes = 5 * 1024 * 1024; // 5MB
        if (fileSizeInBytes > maxSizeInBytes) {
            throw new IOException("Kích thước file quá lớn! Vui lòng chọn file nhỏ hơn 5MB");
        }

        // Generate unique filename to avoid conflicts
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String uniqueFileName = "avatar_" + UUID.randomUUID().toString() + fileExtension;

        // Get the image directory path
        // Gets the JAR/application directory and creates src/main/resources/image path
        String projectDir = System.getProperty("user.dir");
        String imageDir = projectDir + File.separator + "user_data" + File.separator + "image";

        File destDir = new File(imageDir);

        // Create directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Copy file to image directory
        File destFile = new File(destDir, uniqueFileName);
        Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        User currentUser = SessionManager.getInstance().getCurrentUser();
        currentUser.setAvatarPath("image/" + uniqueFileName);
        khoLuuTruNguoiDung.update(currentUser);
        logger.info("Cập nhật avatar path thành công: " + "image/" + uniqueFileName);

        // Return relative path for storage in database
        return "image/" + uniqueFileName;
    }

}