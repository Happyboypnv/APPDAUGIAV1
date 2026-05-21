package com.mycompany.controller;

import com.mycompany.action.HandleNavigationAndAlert;
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

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
    gioBatDauSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour()));
    gioKetThucSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour()));
    gioBatDauSpinner.setEditable(true);
    gioKetThucSpinner.setEditable(true);

    // Phút: 0 - 59, bước nhảy 1
    phutBatDauSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
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
    ngayBatDauPicker.setValue(LocalDate.now());
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
    File file = fileChooser.showOpenDialog(stage);
    if (file != null) {
      loadImage(file);
    }
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

    LocalDate ngayBD = ngayBatDauPicker.getValue();
    LocalDate ngayKT = ngayKetThucPicker.getValue();
    int gioBD = gioBatDauSpinner.getValue();
    int phutBD = phutBatDauSpinner.getValue();
    int gioKT = gioKetThucSpinner.getValue();
    int phutKT = phutKetThucSpinner.getValue();

    String error = validateForm(tenPhien, tenSanPham, danhMuc, giaStr, ngayBD, ngayKT, gioBD, phutBD, gioKT, phutKT);
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

    Task<Boolean> task = new Task<>() {
      @Override
      protected Boolean call() {
        String thoiGianBatDauISO = thoiGianBD.toString(); // LocalDateTime.toString() = ISO-860
        return ApiClient.createAuction(
            tenPhien, tenSanPham, maSanPham,
            danhMuc, moTa,
            thoiGianBatDauISO,
            giaKhoiDiem,
            (int) thoiGianGiay,
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
            HandleNavigationAndAlert.getInstance().handleGoToHome(event);
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

  private String validateForm(String tenPhien, String tenSanPham, String danhMuc,
                              String giaStr, LocalDate ngayBD, LocalDate ngayKT,
                              int gioBD, int phutBD, int gioKT, int phutKT) {
    if (tenPhien.isEmpty() || tenSanPham.isEmpty())
      return "Tên phiên và tên sản phẩm không được để trống!";
    if (danhMuc == null) return "Vui lòng chọn danh mục!";

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

    if (!end.isAfter(start)) return "Thời gian kết thúc phải sau thời gian bắt đầu!";

    long giay = Duration.between(start, end).getSeconds();
    if (giay < 60) return "Thời gian đấu giá tối thiểu là 1 phút!";

    return null;
  }
}