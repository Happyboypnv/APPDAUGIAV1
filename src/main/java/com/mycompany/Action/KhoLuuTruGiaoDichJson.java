package com.mycompany.Action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.models.*;
import com.mycompany.utils.BoChuyenDoiNgay;
import com.mycompany.utils.BoChuyenDoiNgayGio;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp lưu trữ danh sách GiaoDich vào file JSON.
 *
 * Cách hoạt động:
 *  - Dữ liệu được lưu trong file "dulieugiaodich.json" ở thư mục chạy chương trình.
 *  - Mỗi lần đọc/ghi đều thao tác trực tiếp với file (không cache trong RAM),
 *    đảm bảo dữ liệu luôn nhất quán khi có nhiều luồng (thread) cùng chạy.
 *  - Dùng từ khóa "synchronized" + object khóa (LOCK) để tránh xung đột khi
 *    nhiều luồng đồng thời đọc/ghi file.
 *  - ID giao dịch được tự động sinh theo định dạng "GD000001", "GD000002", ...
 *
 * Implements: IKhoLuuTruGiaoDich
 */
public class KhoLuuTruGiaoDichJson implements IKhoLuuTruGiaoDich {

    // ===== HẰNG SỐ =====

    /** Tên file JSON dùng để lưu trữ dữ liệu giao dịch */
    private static final String TEN_FILE = "dulieugiaodich.json";

    /** Object dùng làm khóa cho các khối synchronized (thread-safe) */
    private static final Object LOCK = new Object();

    // ===== BỘ CHUYỂN ĐỔI GSON =====

    /**
     * Gson được cấu hình với các TypeAdapter tùy chỉnh để xử lý
     * kiểu LocalDate và LocalDateTime — vì Gson mặc định không hỗ trợ.
     */
    private final Gson gson;

    // ===== CẤU TRÚC DỮ LIỆU LƯU VÀO FILE =====

    /**
     * Lớp nội bộ (inner class) đại diện cho toàn bộ nội dung file JSON.
     *
     * Cấu trúc JSON sẽ trông như sau:
     * {
     *   "maCuoiCung": 3,
     *   "danhSach": [ {...}, {...}, {...} ]
     * }
     */
    private static class DuLieuGiaoDich {
        /** Số thứ tự giao dịch cuối cùng — dùng để tạo ID tiếp theo */
        int maCuoiCung = 0;

        /** Danh sách tất cả giao dịch được lưu */
        List<GiaoDich> danhSach = new ArrayList<>();
    }

    // ===== CONSTRUCTOR =====

    /**
     * Khởi tạo kho lưu trữ, đăng ký các TypeAdapter cần thiết cho Gson.
     */
    public KhoLuuTruGiaoDichJson() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new BoChuyenDoiNgay())
                .registerTypeAdapter(LocalDateTime.class, new BoChuyenDoiNgayGio())
                .setPrettyPrinting()  // Ghi JSON dễ đọc (có xuống dòng, thụt lề)
                .create();
    }

    // ===== IMPLEMENT CÁC PHƯƠNG THỨC =====

    /**
     * Lưu một giao dịch mới vào file JSON.
     * ID sẽ được tự động tạo theo định dạng "GD000001", "GD000002", ...
     *
     * Quy trình:
     *  1. Đọc dữ liệu hiện có từ file
     *  2. Tăng maCuoiCung lên 1 → tạo ID mới
     *  3. Thêm giao dịch vào danh sách
     *  4. Ghi toàn bộ dữ liệu trở lại file
     *
     * @param giaoDich giao dịch cần lưu (chưa cần có ID, ID sẽ được set tự động)
     */
    @Override
    public void luu(GiaoDich giaoDich) {
        synchronized (LOCK) {
            DuLieuGiaoDich duLieu = docTuFile();
            duLieu.maCuoiCung++;

            // Tạo ID mới theo định dạng "GD000001"
            String idMoi = String.format("GD%06d", duLieu.maCuoiCung);

            // Tạo giao dịch mới với ID đã được gán
            // (vì id là final, phải tạo đối tượng mới thay vì set trực tiếp)
            GiaoDich giaoDichVoiId = new GiaoDich(idMoi, giaoDich.getPhienDauGia());
            giaoDichVoiId.setTrangThai(giaoDich.getTrangThai());

            duLieu.danhSach.add(giaoDichVoiId);
            ghiVaoFile(duLieu);
        }
    }

    /**
     * Lấy toàn bộ danh sách giao dịch từ file JSON.
     *
     * @return danh sách tất cả giao dịch, trả về list rỗng nếu chưa có dữ liệu
     */
    @Override
    public List<GiaoDich> layTatCa() {
        synchronized (LOCK) {
            return docTuFile().danhSach;
        }
    }

    /**
     * Tìm một giao dịch theo mã ID duy nhất.
     *
     * @param id mã giao dịch cần tìm (ví dụ: "GD000001")
     * @return đối tượng GiaoDich nếu tìm thấy, null nếu không có
     */
    @Override
    public GiaoDich layTheoId(String id) {
        synchronized (LOCK) {
            return docTuFile().danhSach.stream()
                    .filter(gd -> gd.getId().equals(id))
                    .findFirst()         // Lấy kết quả đầu tiên khớp
                    .orElse(null);       // Trả null nếu không tìm thấy
        }
    }

    /**
     * Lấy tất cả giao dịch liên quan đến một người dùng (theo email).
     * Tìm cả giao dịch mà người đó là NGƯỜI BÁN lẫn NGƯỜI THẮNG CUỘC (người mua).
     *
     * @param email địa chỉ email của người dùng cần tra cứu
     * @return danh sách giao dịch liên quan, trả về list rỗng nếu không có
     */
    @Override
    public List<GiaoDich> layTheoEmail(String email) {
        synchronized (LOCK) {
            return docTuFile().danhSach.stream()
                    .filter(gd -> {
                        // Kiểm tra người dùng có là người BÁN không
                        boolean laNguoiBan = gd.getPhienDauGia().getNguoiBan()
                                .layThuDienTu().equals(email);

                        // Kiểm tra người dùng có là người THẮNG CUỘC (người mua) không
                        NguoiDung nguoiThang = gd.getPhienDauGia().getNguoiThangCuoc();
                        boolean laNguoiMua = nguoiThang != null
                                && nguoiThang.layThuDienTu().equals(email);

                        return laNguoiBan || laNguoiMua;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Cập nhật thông tin một giao dịch đã có trong kho (so khớp theo ID).
     * Thường dùng để thay đổi trạng thái: CHO_THANH_TOAN → DA_THANH_TOAN hoặc DA_HOAN_TIEN.
     *
     * Quy trình:
     *  1. Đọc toàn bộ danh sách từ file
     *  2. Tìm vị trí của giao dịch cần cập nhật theo ID
     *  3. Thay thế giao dịch cũ bằng giao dịch mới
     *  4. Ghi toàn bộ danh sách trở lại file
     *
     * @param giaoDich giao dịch đã được chỉnh sửa (phải có cùng ID với bản đang lưu)
     */
    @Override
    public void capNhat(GiaoDich giaoDich) {
        synchronized (LOCK) {
            DuLieuGiaoDich duLieu = docTuFile();

            // Tìm vị trí giao dịch cũ trong danh sách theo ID
            for (int i = 0; i < duLieu.danhSach.size(); i++) {
                if (duLieu.danhSach.get(i).getId().equals(giaoDich.getId())) {
                    // Thay thế giao dịch cũ bằng giao dịch đã cập nhật
                    duLieu.danhSach.set(i, giaoDich);
                    break; // Tìm thấy rồi, dừng vòng lặp
                }
            }

            ghiVaoFile(duLieu);
        }
    }

    // ===== PHƯƠNG THỨC HỖ TRỢ NỘI BỘ =====

    /**
     * Đọc toàn bộ dữ liệu từ file JSON và chuyển thành đối tượng DuLieuGiaoDich.
     * Nếu file chưa tồn tại hoặc rỗng → trả về đối tượng rỗng (không ném lỗi).
     *
     * @return đối tượng DuLieuGiaoDich chứa danh sách và mã cuối cùng
     */
    private DuLieuGiaoDich docTuFile() {
        File file = new File(TEN_FILE);

        // Nếu file chưa tồn tại hoặc rỗng, trả về dữ liệu mặc định
        if (!file.exists() || file.length() == 0) {
            return new DuLieuGiaoDich();
        }

        try (Reader reader = new FileReader(file)) {
            Type kieu = new TypeToken<DuLieuGiaoDich>() {}.getType();
            DuLieuGiaoDich ketQua = gson.fromJson(reader, kieu);
            // Phòng trường hợp file có nội dung nhưng parse ra null
            return (ketQua != null) ? ketQua : new DuLieuGiaoDich();
        } catch (IOException e) {
            System.err.println("[KhoGiaoDich] Lỗi đọc file: " + e.getMessage());
            return new DuLieuGiaoDich();
        }
    }

    /**
     * Ghi toàn bộ đối tượng DuLieuGiaoDich vào file JSON (ghi đè hoàn toàn).
     *
     * @param duLieu dữ liệu cần ghi vào file
     */
    private void ghiVaoFile(DuLieuGiaoDich duLieu) {
        try (Writer writer = new FileWriter(TEN_FILE)) {
            gson.toJson(duLieu, writer);
        } catch (IOException e) {
            System.err.println("[KhoGiaoDich] Lỗi ghi file: " + e.getMessage());
        }
    }
}