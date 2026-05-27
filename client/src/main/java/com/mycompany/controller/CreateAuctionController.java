package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.models.User;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import com.mycompany.utils.UserProfileUpdater;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.UUID;

public class CreateAuctionController implements Initializable {

  @FXML
  private TextField tenPhienField;
  @FXML
  private TextField tenSanPhamField;
  @FXML
  private ComboBox<String> danhMucSanPham;
  @FXML
  private TextArea moTaField;
  @FXML
  private TextField giaKhoiDiemField;

  @FXML
  private DatePicker ngayBatDauPicker;
  @FXML
  private Spinner<Integer> gioBatDauSpinner;
  @FXML
  private Spinner<Integer> phutBatDauSpinner;

  @FXML
  private DatePicker ngayKetThucPicker;
  @FXML
  private Spinner<Integer> gioKetThucSpinner;
  @FXML
  private Spinner<Integer> phutKetThucSpinner;

  @FXML
  private StackPane dropArea;
  @FXML
  private ImageView previewImage;
  @FXML
  private VBox instructionText;

  private File selectedImageFile = null;

  private String filePath = null;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    setupDanhMuc();
    setupSpinners();
    setupDatePickers();
    setupDragAndDrop();
  }

  private void setupDanhMuc() {
    danhMucSanPham.getItems().addAll(
        "Đá quý", "Nghệ thuật",
        "Xe cộ", "Điện tử"
    );
  }

  private void setupSpinners() {
    // Giờ: 0 - 23
    LocalTime defaultStartTime = LocalTime.now().plusMinutes(1);
    // Giờ: 0 - 23
    gioBatDauSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultStartTime.getHour()));
    gioKetThucSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour()));
    gioBatDauSpinner.setEditable(true);
    gioKetThucSpinner.setEditable(true);

    // Phút: 0 - 59, bước nhảy 1
    phutBatDauSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, defaultStartTime.getMinute()));
    phutKetThucSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    phutBatDauSpinner.setEditable(true);
    phutKetThucSpinner.setEditable(true);

    // Commit giá trị khi người dùng gõ trực tiếp rồi bấm Enter hoặc mất focus
    commitOnFocusLost(gioBatDauSpinner);
    commitOnFocusLost(gioKetThucSpinner);
    commitOnFocusLost(phutBatDauSpinner);
    commitOnFocusLost(phutKetThucSpinner);
  }

  /**
   * Đảm bảo spinner editable commit giá trị khi mất focus (không cần nhấn Enter).
   */
  private void commitOnFocusLost(Spinner<Integer> spinner) {
    spinner.focusedProperty().addListener((obs, oldVal, newVal) -> {
      if (!newVal) {
        spinner.increment(0); // trick: commit editor text
      }
    });
  }

  private void setupDatePickers() {
    ngayBatDauPicker.setValue(LocalDateTime.now().plusMinutes(1).toLocalDate());
    ngayKetThucPicker.setValue(LocalDate.now().plusDays(1));
    previewImage.setVisible(false);
  }

  private void setupDragAndDrop() {
    dropArea.setOnDragOver(event -> {
      if (event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY);
      }
      event.consume();
    });

    dropArea.setOnDragDropped(event -> {
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

  @FXML
  public void handleSelectImage() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn ảnh sản phẩm");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
    );
    Stage stage = (Stage) dropArea.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);
    if (selectedFile != null) {
      loadImage(selectedFile);
    } else {
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR,"Chọn Ảnh", "Lỗi chọn ảnh");
      return;
    }

    String fileName = selectedFile.getName().toLowerCase();
    if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi chọn ảnh", "Chỉ hỗ trợ các định dạng: .jpg, .jpeg, .png");
      return;
    }

    // Validate file size (max 5MB)
    long fileSizeInBytes = selectedFile.length();
    long maxSizeInBytes = 5 * 1024 * 1024; // 5MB
    if (fileSizeInBytes > maxSizeInBytes) {
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi chọn ảnh", "Kích thước file quá lớn! Vui lòng chọn file nhỏ hơn 5MB");
      return;
    }

    // Generate unique filename to avoid conflicts
    String fileExtension = fileName.substring(fileName.lastIndexOf("."));
    String uniqueFileName = "product_" + UUID.randomUUID().toString() + fileExtension;

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
    try {
      Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi copy file", "Không copy file được sang đường dẫn!");
      e.printStackTrace();
    }

    this.filePath = "image/" + uniqueFileName;

//    User currentUser = SessionManager.getInstance().getCurrentUser();
//    currentUser.setAvatarPath("image/" + uniqueFileName);
//
//    // Gửi lên server thay vì ghi thẳng vào DB local
//    java.util.Map<String, String> updates = new java.util.HashMap<>();
//    updates.put("avatar", "image/" + uniqueFileName);
//    UserProfileUpdater.getInstance().updateUser(updates);

    HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.INFORMATION, "Cập nhật ảnh sản phẩm","Cập nhật product path thành công: " + "image/" + uniqueFileName);

  }

  private void loadImage(File file) {
    try {
      Image image = new Image(file.toURI().toString());
      previewImage.setImage(image);
      previewImage.setVisible(true);
      instructionText.setVisible(false);
      selectedImageFile = file;
    } catch (Exception e) {
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đọc ảnh!");
    }
  }

  // Guard chống double-submit: khi đang xử lý thì bỏ qua mọi click tiếp theo
  private boolean isSubmitting = false;

  @FXML
  public void handleTaoPhien(ActionEvent event) {
    // FIX: Chặn double-click/double-submit ngay lập tức trước mọi xử lý khác
    if (isSubmitting) return;
    isSubmitting = true;

    Button btn = (Button) event.getSource();
    btn.setDisable(true);
    btn.setText("Đang tạo...");

    String tenPhien = tenPhienField.getText().trim();
    String tenSanPham = tenSanPhamField.getText().trim();
    String danhMuc = danhMucSanPham.getValue();
    String moTa = moTaField.getText().trim();
    String giaStr = giaKhoiDiemField.getText().trim().replace(",", "").replace(".", "");
    String duongDan = filePath;

    LocalDate ngayBD = ngayBatDauPicker.getValue();
    LocalDate ngayKT = ngayKetThucPicker.getValue();
    int gioBD = gioBatDauSpinner.getValue();
    int phutBD = phutBatDauSpinner.getValue();
    int gioKT = gioKetThucSpinner.getValue();
    int phutKT = phutKetThucSpinner.getValue();

    String error = validateForm(tenPhien, tenSanPham, danhMuc, moTa, giaStr, ngayBD, ngayKT, gioBD, phutBD, gioKT, phutKT, duongDan);
    if (error != null) {
      isSubmitting = false;
      btn.setDisable(false);
      btn.setText("Tạo phiên");
      HandleNavigationAndAlert.getInstance().showAlert(Alert.AlertType.WARNING, "Thông báo", error);
      return;
    }

    final double giaKhoiDiem = Double.parseDouble(giaStr);
    final LocalDateTime thoiGianBD = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, phutBD));
    final LocalDateTime thoiGianKT = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, phutKT));
    final long thoiGianGiay = Duration.between(thoiGianBD, thoiGianKT).getSeconds();
    final String maSanPham = "SP_" + System.currentTimeMillis();
    final String thoiGianBatDauISO = thoiGianBD.toString();

    // *** FIX QUAN TRỌNG: Dùng serverToken (USER_...) thay vì localToken (Base64) ***
    String token = SessionManager.getInstance().getServerToken();

    if (token == null) {
      // Fallback: thử tạo token thủ công từ email nếu chưa có serverToken
      // (trường hợp user chưa đăng xuất từ phiên cũ)
      if (SessionManager.getInstance().getCurrentUser() != null) {
        String email = SessionManager.getInstance().getCurrentUser().getEmail();
        token = "USER_" + email + "_" + System.currentTimeMillis();
        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.WARNING, "Cảnh báo",
            "Phiên đăng nhập có thể đã hết hạn. Vui lòng đăng xuất và đăng nhập lại nếu gặp lỗi.");
      } else {
        isSubmitting = false;
        btn.setDisable(false);
        btn.setText("Tạo phiên");
        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
        return;
      }
    }

    // Tạo bản sao final để dùng trong lambda
    final String finalToken = token;

    final ActionEvent finalEvent = event;
    Task<Boolean> task = new Task<>() {
      @Override
      protected Boolean call() {
        String thoiGianBatDauISO = thoiGianBD.toString(); // LocalDateTime.toString() = ISO-860
        return ApiClient.createAuction(
            tenPhien, tenSanPham, maSanPham,
            danhMuc, moTa,
            thoiGianBatDauISO,
            giaKhoiDiem,
            (int) thoiGianGiay, duongDan,
            finalToken
        );
      }
    };

    task.setOnSucceeded(e -> {
      isSubmitting = false;
      btn.setDisable(false);
      btn.setText("Tạo phiên");
      if (task.getValue()) {
        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.INFORMATION, "Thành công", "Đã tạo phiên đấu giá thành công!");
        // Delay nhỏ để server kịp lưu và index phiên trước khi Home load lại
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
            javafx.util.Duration.millis(600)
        );
        pause.setOnFinished(pauseEvent -> {
          try {
            HandleNavigationAndAlert.getInstance().handleGoToHome(finalEvent);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });
        pause.play();
      } else {
        System.out.println("TOKEN KHI TẠO PHIÊN: " + finalToken);

        HandleNavigationAndAlert.getInstance().showAlert(
            Alert.AlertType.ERROR, "Lỗi",
            "Server từ chối yêu cầu. Kiểm tra server đã chạy chưa (port 8080).");
      }
    });

    task.setOnFailed(e -> {
      isSubmitting = false;
      btn.setDisable(false);
      btn.setText("Tạo phiên");
      HandleNavigationAndAlert.getInstance().showAlert(
          Alert.AlertType.ERROR, "Lỗi kết nối",
          "Không thể kết nối tới Server (localhost:8080). Hãy chắc chắn ServerApp đang chạy!");
    });

    new Thread(task).start();
  }

  @FXML
  public void handleHuy(ActionEvent event) {
    try {
      HandleNavigationAndAlert.getInstance().handleGoToHome(event);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String validateForm(String tenPhien, String tenSanPham, String danhMuc, String moTa,
                              String giaStr, LocalDate ngayBD, LocalDate ngayKT,
                              int gioBD, int phutBD, int gioKT, int phutKT, String duongDan) {
    if (tenPhien.isEmpty() || tenSanPham.isEmpty())
      return "Tên phiên và tên sản phẩm không được để trống!";
    if (moTa.isEmpty()) return "Vui lòng nhập mô tả!";
    if (danhMuc == null) return "Vui lòng chọn danh mục!";
    if (duongDan == null) return "Vui lòng tải ảnh sản phẩm!";

    try {
      double gia = Double.parseDouble(giaStr);
      if (gia < 1000) return "Giá khởi điểm phải từ 1.000 VNĐ!";
    } catch (NumberFormatException e) {
      return "Giá khởi điểm phải là số!";
    }

    if (ngayBD == null || ngayKT == null)
      return "Vui lòng chọn đầy đủ ngày bắt đầu/kết thúc!";

    LocalDateTime start = LocalDateTime.of(ngayBD, LocalTime.of(gioBD, phutBD));
    LocalDateTime end = LocalDateTime.of(ngayKT, LocalTime.of(gioKT, phutKT));

    if (!start.isAfter(LocalDateTime.now())) return "Thời gian bắt đầu phải ở tương lai!";

    if (!end.isAfter(start)) return "Thời gian kết thúc phải sau thời gian bắt đầu!";

    long giay = Duration.between(start, end).getSeconds();
    if (giay < 60) return "Thời gian đấu giá tối thiểu là 1 phút!";

    return null;
  }
}