package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

/**
 * CreateAuctionController - Controller quản lý form "Tạo Phiên Đấu Giá Mới"
 *
 * MAP FX:ID → FIELD:
 * - tenPhienField      → TextField  : tên phiên
 * - tenSanPhamField    → TextField  : tên sản phẩm
 * - danhMucSanPham     → ComboBox  : danh mục
 * - moTaField          → TextField  : mô tả sản phẩm
 * - giaKhoiDiemField   → TextField  : giá khởi điểm
 * - ngayBatDauPicker   → DatePicker : ngày bắt đầu
 * - gioBatDauSpinner   → Spinner    : giờ bắt đầu
 * - ngayKetThucPicker  → DatePicker : ngày kết thúc
 * - gioKetThucSpinner  → Spinner    : giờ kết thúc
 * - dropArea           → StackPane  : vùng kéo thả / click ảnh
 * - previewImage       → ImageView  : preview ảnh đã chọn
 * - instructionText    → VBox       : label hướng dẫn (ẩn khi có ảnh)
 *
 * MAP HANDLER → PHƯƠNG THỨC:
 * - onAction="#handleTaoPhien"          → handleTaoPhien()
 * - onAction="#handleHuy"               → handleHuy()
 * - onMouseClicked="#handleSelectImage" → handleSelectImage()
 */
public class CreateAuctionController implements Initializable {

    // ===== @FXML FIELDS — khớp chính xác fx:id trong FXML =====

    @FXML private Label labelWelcome;

    @FXML private TextField       tenPhienField;
    @FXML private TextField       tenSanPhamField;
    @FXML private ComboBox<String> danhMucSanPham;
    @FXML private TextArea       moTaField;
    @FXML private TextField       giaKhoiDiemField;

    @FXML private DatePicker         ngayBatDauPicker;
    @FXML private Spinner<Integer>   gioBatDauSpinner;
    @FXML private DatePicker         ngayKetThucPicker;
    @FXML private Spinner<Integer>   gioKetThucSpinner;

    @FXML private StackPane dropArea;
    @FXML private ImageView previewImage;
    @FXML private VBox      instructionText;

    // ===== PRIVATE STATE =====

    /** File ảnh người dùng đã chọn (null nếu chưa chọn) */
    private File selectedImageFile = null;

    // ===== INITIALIZE =====

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDanhMuc();
        setupSpinners();
        setupDatePickers();
        setupDragAndDrop();
    }

    /** Thêm danh mục sản phẩm vào ComboBox */
    private void setupDanhMuc() {
        danhMucSanPham.getItems().addAll(
                "Đồng hồ",
                "Trang sức",
                "Nghệ thuật",
                "Bất động sản",
                "Xe cộ",
                "Điện tử",
                "Thời trang",
                "Khác"
        );
    }

    /** Khởi tạo Spinner giờ bắt đầu và kết thúc (0-23) */
    private void setupSpinners() {
        gioBatDauSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        gioKetThucSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
        gioBatDauSpinner.setEditable(true);
        gioKetThucSpinner.setEditable(true);
    }

    /** Đặt giá trị mặc định cho DatePicker */
    private void setupDatePickers() {
        ngayBatDauPicker.setValue(LocalDate.now());
        ngayKetThucPicker.setValue(LocalDate.now().plusDays(1));
        previewImage.setVisible(false); //
    }

    /**
     * Gắn sự kiện Drag & Drop vào dropArea.
     * onDragOver   → chấp nhận thả file
     * onDragDropped → lấy file đầu tiên và xử lý
     */
    private void setupDragAndDrop() {
        dropArea.setOnDragOver((DragEvent event) -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropArea.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                loadImage(db.getFiles().get(0));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // ===== HANDLER: CLICK VÀO VÙNG UPLOAD =====

    /**
     * Gọi từ FXML: onMouseClicked="#handleSelectImage" trên dropArea.
     * Mở FileChooser để người dùng chọn ảnh từ máy tính.
     */
    @FXML
    public void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Ảnh (PNG, JPG, GIF, BMP)",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) dropArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadImage(file);
        }
    }

    /**
     * Kiểm tra định dạng file, lưu vào selectedImageFile,
     * hiển thị preview và ẩn label hướng dẫn.
     *
     * @param file File ảnh được chọn (drag-drop hoặc FileChooser)
     */
    private void loadImage(File file) {
        String name = file.getName().toLowerCase();
        boolean isImage = name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif")
                || name.endsWith(".bmp");

        if (!isImage) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.WARNING, "Định dạng không hợp lệ",
                    "Vui lòng chọn file ảnh (PNG, JPG, JPEG, GIF, BMP)!"
            );
            return;
        }

        try {
            Image image = new Image(file.toURI().toString());
            previewImage.setImage(image);
            previewImage.setVisible(true);
            instructionText.setVisible(false); // Ẩn hướng dẫn khi có ảnh
            selectedImageFile = file;
        } catch (Exception e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi ảnh",
                    "Không thể đọc file ảnh: " + e.getMessage()
            );
        }
    }

    // ===== HANDLER: NÚT TẠO PHIÊN =====

    /**
     * Gọi từ FXML: onAction="#handleTaoPhien" trên Button "Tạo phiên".
     *
     * QUY TRÌNH:
     * 1. Thu thập dữ liệu từ tất cả các trường
     * 2. Validate — nếu lỗi thì dừng và hiện thông báo
     * 3. Tính thoiGianGiay (khoảng cách bắt đầu → kết thúc)
     * 4. Gọi ApiClient.createAuction() → server lưu vào DB
     * 5. Nếu thành công → thông báo → về Home
     */
    @FXML
    public void handleTaoPhien(ActionEvent event) {
        // 1. Thu thập dữ liệu
        String tenPhien   = tenPhienField.getText().trim();
        String tenSanPham = tenSanPhamField.getText().trim();
        String danhMuc    = danhMucSanPham.getValue();
        String moTa       = moTaField.getText().trim();
        String giaStr     = giaKhoiDiemField.getText().trim()
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "");
        LocalDate ngayBD  = ngayBatDauPicker.getValue();
        LocalDate ngayKT  = ngayKetThucPicker.getValue();
        int gioBD         = gioBatDauSpinner.getValue();
        int gioKT         = gioKetThucSpinner.getValue();

        // 2. Validate
        String loi = validateForm(tenPhien, tenSanPham, danhMuc, giaStr, ngayBD, ngayKT, gioBD, gioKT);
        if (loi != null) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.WARNING, "Lỗi thông tin", loi);
            return;
        }

        // 3. Parse và tính thời gian
        double giaKhoiDiem = Double.parseDouble(giaStr);
        LocalDateTime thoiGianBD = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, 0));
        LocalDateTime thoiGianKT = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, 0));
        long thoiGianGiay = java.time.Duration.between(thoiGianBD, thoiGianKT).getSeconds();

        // 4. Kiểm tra đăng nhập
        String token = SessionManager.getInstance().getCurrentToken();
        if (token == null) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi xác thực", "Bạn chưa đăng nhập!");
            return;
        }

        // Tạo mã sản phẩm tự động
        String maSanPham = "SP_" + System.currentTimeMillis();

        // 5. Gọi API tạo phiên
        boolean thanhCong = ApiClient.createAuction(
                tenPhien, tenSanPham, maSanPham,
                giaKhoiDiem, (int) thoiGianGiay, token
        );

        if (thanhCong) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.INFORMATION, "Thành công",
                    "Phiên đấu giá \"" + tenPhien + "\" đã được tạo thành công!"
            );
            try {
                HandleNavigationAndAlert.getInstance().handleGoToHome(event);
            } catch (IOException e) {
                HandleNavigationAndAlert.getInstance().showAlert(
                        Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể quay về trang chủ!");
            }
        } else {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi tạo phiên",
                    "Không thể tạo phiên. Vui lòng kiểm tra kết nối server!"
            );
        }
    }

    // ===== HANDLER: NÚT HỦY =====

    /**
     * Gọi từ FXML: onAction="#handleHuy" trên Button "Hủy".
     * Quay về Home mà không lưu dữ liệu.
     */
    @FXML
    public void handleHuy(ActionEvent event) {
        try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
        } catch (IOException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể quay về trang chủ!");
        }
    }

    // ===== VALIDATION =====

    /**
     * Validate toàn bộ dữ liệu form trước khi gửi lên server.
     *
     * QUY TẮC:
     * - tenPhien    : không rỗng, 3-100 ký tự
     * - tenSanPham  : không rỗng, 2-100 ký tự
     * - danhMuc     : phải chọn (không null)
     * - giaStr      : số hợp lệ, >= 1.000 VNĐ
     * - ngayBatDau  : không null
     * - ngayKetThuc : không null, thời điểm kết thúc > bắt đầu
     * - thoiGianGiay: 60 giây → 7 ngày
     *
     * @return null nếu hợp lệ, chuỗi thông báo lỗi nếu không
     */
    private String validateForm(String tenPhien, String tenSanPham, String danhMuc,
                                String giaStr, LocalDate ngayBD, LocalDate ngayKT,
                                int gioBD, int gioKT) {
        if (tenPhien.isEmpty())
            return "Vui lòng nhập tên phiên đấu giá!";
        if (tenPhien.length() < 3 || tenPhien.length() > 100)
            return "Tên phiên phải từ 3 đến 100 ký tự!";

        if (tenSanPham.isEmpty())
            return "Vui lòng nhập tên sản phẩm!";
        if (tenSanPham.length() < 2 || tenSanPham.length() > 100)
            return "Tên sản phẩm phải từ 2 đến 100 ký tự!";

        if (danhMuc == null)
            return "Vui lòng chọn danh mục sản phẩm!";

        if (giaStr.isEmpty())
            return "Vui lòng nhập giá khởi điểm!";
        double gia;
        try {
            gia = Double.parseDouble(giaStr);
        } catch (NumberFormatException e) {
            return "Giá khởi điểm phải là số hợp lệ!";
        }
        if (gia <= 0)
            return "Giá khởi điểm phải lớn hơn 0!";
        if (gia < 1000)
            return "Giá khởi điểm tối thiểu là 1.000 VNĐ!";

        if (ngayBD == null)
            return "Vui lòng chọn ngày bắt đầu!";
        if (ngayKT == null)
            return "Vui lòng chọn ngày kết thúc!";

        LocalDateTime thoiGianBD = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, 0));
        LocalDateTime thoiGianKT = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, 0));

        if (!thoiGianKT.isAfter(thoiGianBD))
            return "Thời gian kết thúc phải sau thời gian bắt đầu!";

        long giay = java.time.Duration.between(thoiGianBD, thoiGianKT).getSeconds();
        if (giay < 60)
            return "Phiên đấu giá phải kéo dài ít nhất 1 phút!";
        if (giay > 7L * 24 * 3600)
            return "Phiên đấu giá không được kéo dài quá 7 ngày!";

        return null; // Hợp lệ
    }
}