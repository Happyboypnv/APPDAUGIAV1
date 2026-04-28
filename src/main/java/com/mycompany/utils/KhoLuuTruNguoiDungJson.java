package com.mycompany.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.models.NguoiDung;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * KhoLuuTruNguoiDungJson - Implementation của IKhoLuuTruNguoiDung dùng JSON file
 *
 * MỤC ĐÍCH:
 * - Lưu trữ dữ liệu người dùng trong file "dulieunguoidung.json"
 * - Cung cấp các phương thức để lưu, cập nhật, lấy, kiểm tra thông tin người dùng
 * - Đảm bảo thread-safe khi many threads truy cập cùng lúc
 *
 * CẤU TRÚC FILE JSON:
 * {
 *   "maCuoiCung": 123,
 *   "danhSach": {
 *     "email1@gmail.com": {...NguoiDung object...},
 *     "email2@gmail.com": {...NguoiDung object...},
 *   }
 * }
 *
 * TÍNH NĂNG:
 * - Thread-safe: Dùng synchronized block để tránh race condition
 * - Auto ID generation: Tạo ID tự động kiểu "PPTT000001", "PPTT000002", ...
 * - Type conversion: Xử lý LocalDate và LocalDateTime qua custom adapter
 * - Legacy migration: Tự động chuyển mật khẩu cũ sang hashed password
 */
public class KhoLuuTruNguoiDungJson implements IKhoLuuTruNguoiDung {

    // Tên file JSON chứa dữ liệu người dùng
    private static final String TEN_FILE = "dulieunguoidung.json";

    // Object lock dùng cho synchronized - đảm bảo thread-safe
    // Dùng static final Object để tất cả methods dùng chung lock này
    private static final Object LOCK = new Object();

    // Gson object sử dụng để chuyển đổi giữa Java object và JSON string
    // Được config trong constructor
    private final Gson boChuyenDoiGson;

    // Type token giúp Gson hiểu cấu trúc Map<String, NguoiDung>
    // Cần thiết vì Generic type bị erase trong Java runtime
    private static final Type KIEU_DU_LIEU = new TypeToken<DuLieuNguoiDung>(){}.getType();

    /**
     * INNER CLASS: DuLieuNguoiDung
     * Đại diện cấu trúc dữ liệu bên trong file JSON
     *
     * GIẢI THÍCH:
     * - maCuoiCung: Counter để tạo ID tiếp theo (PPTT000001, PPTT000002,...)
     * - danhSach: Map chứa tất cả người dùng indexed by email
     *
     * Vì sao cần class này?
     * - Gson cần biết cấu trúc dữ liệu file JSON
     * - Nếu không, sẽ không biết field nào là gì
     */
    private static class DuLieuNguoiDung {
        // Giá trị ID cuối cùng được tạo - dùng để tạo ID mới (tăng lên 1)
        int maCuoiCung = 0;

        // Map chứa tất cả người dùng
        // Key: email (định danh duy nhất)
        // Value: NguoiDung object
        Map<String, NguoiDung> danhSach = new HashMap<>();
    }

    /**
     * CONSTRUCTOR: KhoLuuTruNguoiDungJson()
     * MỤC ĐÍCH: Khởi tạo Gson với các custom adapters
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Tạo GsonBuilder để config Gson
     * 2. Đăng ký BoChuyenDoiNgay adapter cho LocalDate
     * 3. Đăng ký BoChuyenDoiNgayGio adapter cho LocalDateTime
     * 4. Bật pretty printing để JSON dễ đọc
     * 5. Gọi diChuyenMatKhau() để migrate old passwords
     */
   public KhoLuuTruNguoiDungJson() {
        // Tạo Gson builder để cấu hình Gson
        boChuyenDoiGson = new GsonBuilder()
                // Đăng ký adapter cho LocalDate: "2024-01-15" <-> LocalDate object
                .registerTypeAdapter(LocalDate.class, new BoChuyenDoiNgay())
                // Đăng ký adapter cho LocalDateTime: "2024-01-15T10:30:00" <-> LocalDateTime object
                .registerTypeAdapter(LocalDateTime.class, new BoChuyenDoiNgayGio())
                // Bật pretty printing để file JSON có indentation, dễ đọc
                .setPrettyPrinting()
                // Tạo Gson instance
                .create();
        // Migrate dữ liệu cũ (plain text password) sang hashed password
        diChuyenMatKhau();
    }

    /**
     * PHƯƠNG THỨC: luu(NguoiDung nguoiDung)
     * MỤC ĐÍCH: Lưu một người dùng MỚI vào kho lưu trữ
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Đọc dữ liệu hiện tại từ file
     * 2. Tăng ID counter lên 1
     * 3. Tạo ID mới dạng "PPTT000001"
     * 4. Gán ID cho người dùng
     * 5. Thêm vào map (key = email)
     * 6. Lưu lại vào file
     *
     * THREAD-SAFE: Dùng synchronized block với LOCK
     *
     * @param nguoiDung Người dùng cần lưu
     */
    @Override
    public void luu(NguoiDung nguoiDung) {
        // Khóa để đảm bảo chỉ có 1 thread modify dữ liệu cùng lúc
        synchronized (LOCK) {
            // Bước 1: Đọc dữ liệu hiện tại từ file
            DuLieuNguoiDung duLieu = docTuFile();

            // Bước 2: Tăng counter lên 1
            duLieu.maCuoiCung++;

            // Bước 3: Tạo ID mới dạng "PPTT000001", "PPTT000002", etc
            // String.format("PPTT%06d", 1) = "PPTT000001"
            String maMoi = String.format("PPPT%06d", duLieu.maCuoiCung);

            // Bước 4: Gán ID cho người dùng
            nguoiDung.setMaNguoiDung(maMoi);

            // Bước 5: Thêm vào map
            // Key = email (định danh duy nhất)
            // Value = người dùng object
            duLieu.danhSach.put(nguoiDung.layThuDienTu(), nguoiDung);

            // Bước 6: Lưu lại vào file JSON
            ghiVaoFile(duLieu);
        }
    }

    /**
     * PHƯƠNG THỨC: capNhatNguoiDung(NguoiDung nguoiDung)
     * MỤC ĐÍCH: CẬP NHẬT thông tin người dùng (không tạo ID mới)
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Đọc dữ liệu hiện tại từ file
     * 2. Cập nhật bản ghi cũ (giữ nguyên ID và maCuoiCung)
     * 3. Lưu lại vào file
     *
     * KHÁC BIỆT VỚI luu():
     * - luu() tạo ID mới, tăng counter
     * - capNhatNguoiDung() không thay đổi ID, chỉ cập nhật dữ liệu
     *
     * THREAD-SAFE: Dùng synchronized block
     *
     * @param nguoiDung Người dùng đã được chỉnh sửa
     */
    @Override
    public void capNhatNguoiDung(NguoiDung nguoiDung) {
        synchronized (LOCK) {
            // Bước 1: Đọc dữ liệu hiện tại
            DuLieuNguoiDung duLieu = docTuFile();

            // Bước 2: Cập nhật bản ghi cũ
            // Put với cùng key sẽ ghi đè bản ghi cũ
            duLieu.danhSach.put(nguoiDung.layThuDienTu(), nguoiDung);

            // Bước 3: Lưu lại vào file
            ghiVaoFile(duLieu);
        }
    }

    /**
     * PHƯƠNG THỨC: layTatCa()
     * MỤC ĐÍCH: Lấy Map tất cả người dùng
     *
     * GIẢI THÍCH:
     * - Đọc file JSON
     * - Trả về map chứa tất cả người dùng
     * - Key là email, value là NguoiDung object
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * @return Map<email, NguoiDung>
     */
    @Override
    public synchronized Map<String, NguoiDung> layTatCa() {
        return docTuFile().danhSach;
    }

    /**
     * PHƯƠNG THỨC: docTuFile()
     * MỤC ĐÍCH: Đọc dữ liệu từ file JSON
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Tạo File object từ tên file
     * 2. Kiểm tra file tồn tại hay chưa
     * 3. Nếu không tồn tại, trả về object trống mới
     * 4. Nếu tồn tại, đọc JSON và convert thành object
     * 5. Nếu lỗi đọc, trả về object trống
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * @return DuLieuNguoiDung object chứa dữ liệu
     */
    private synchronized DuLieuNguoiDung docTuFile() {
        // Tạo File object
        File file = new File(TEN_FILE);

        // Kiểm tra: File không tồn tại hoặc rỗng?
        if (!file.exists() || file.length() == 0) {
            // Trả về object trống mới
            return new DuLieuNguoiDung();
        }

        try (Reader nguoiDoc = new FileReader(file)) {
            // Dùng Gson để đọc JSON file và convert thành object
            // KIEU_DU_LIEU là Type định nghĩa cấu trúc DuLieuNguoiDung
            DuLieuNguoiDung kq = boChuyenDoiGson.fromJson(nguoiDoc, KIEU_DU_LIEU);

            // Nếu kq null, trả về object trống
            return (kq == null) ? new DuLieuNguoiDung() : kq;
        } catch (IOException e) {
            // Nếu có lỗi đọc, print error và trả về object trống
            System.err.println("Lỗi đọc file: " + e.getMessage());
            return new DuLieuNguoiDung();
        }
    }

    /**
     * PHƯƠNG THỨC: ghiVaoFile(DuLieuNguoiDung duLieu)
     * MỤC ĐÍCH: Ghi object vào file JSON
     *
     * GIẢI THÍCH:
     * 1. Tạo FileWriter để ghi file
     * 2. Dùng Gson để convert object thành JSON
     * 3. Ghi JSON vào file
     * 4. Nếu lỗi, print stack trace
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * @param duLieu Object chứa dữ liệu cần ghi
     */
    private synchronized void ghiVaoFile(DuLieuNguoiDung duLieu) {
        try (Writer nguoiGhi = new FileWriter(TEN_FILE)) {
            // Dùng Gson để convert object thành JSON string
            // Ghi trực tiếp vào file
            boChuyenDoiGson.toJson(duLieu, nguoiGhi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC: kiemTraNguoiDung(String email, String password)
     * MỤC ĐÍCH: Xác nhận đăng nhập (authentication)
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Lấy map tất cả người dùng
     * 2. Kiểm tra email có tồn tại không
     * 3. Nếu tồn tại, kiểm tra mật khẩu bằng BoMaHoaMatKhau
     * 4. Trả về true nếu email + password đúng, false nếu sai
     *
     * THREAD-SAFE: Dùng synchronized block
     *
     * @param email Email của người dùng
     * @param password Mật khẩu gốc (plain text)
     * @return true nếu xác thực thành công
     */
    @Override
    public boolean kiemTraNguoiDung(String email, String password) {
        synchronized (LOCK) {
            // Lấy map tất cả người dùng
            Map<String, NguoiDung> danhSach = layTatCa();

            // Nếu map null, trả về false
            if (danhSach == null) return false;

            // Tìm người dùng có email này
            NguoiDung check = danhSach.get(email);

            // Email không tồn tại
            if (check == null) return false;

            // Email tồn tại, kiểm tra mật khẩu
            // BoMaHoaMatKhau.kiemTraMatKhau(): mã hóa password nhập
            // So sánh với mật khẩu đã mã hóa trong database
            return com.mycompany.utils.BoMaHoaMatKhau.kiemTraMatKhau(
                password,
                check.layMatKhau(),
                check.laySalt()
            );
        }
    }

    /**
     * PHƯƠNG THỨC: kiemTraEmail(String email)
     * MỤC ĐÍCH: Kiểm tra email đã tồn tại hay chưa
     *
     * GIẢI THÍCH:
     * 1. Lấy map tất cả người dùng
     * 2. Kiểm tra email có trong map không
     * 3. TRVAL NGƯỢC: true = email CHƯA tồn tại (OK dùng)
     *                 false = email ĐÃ tồn tại (không dùng được)
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * @param email Email cần kiểm tra
     * @return true nếu email CHƯA dùng (có thể đăng ký)
     *         false nếu email ĐÃ dùng (không được đăng ký)
     */
    public boolean kiemTraEmail(String email) {
        synchronized (LOCK) {
            Map<String, NguoiDung> danhSach = layTatCa();

            // Nếu map null, trả về true (không có ai dùng email này)
            if (danhSach == null) return true;

            // !danhSach.containsKey(email)
            // = không tồn tại email này = OK để dùng
            // = true nếu chưa tồn tại, false nếu đã tồn tại
            return !danhSach.containsKey(email);
        }
    }

    /**
     * PHƯƠNG THỨC: layNguoiDungTheoEmail(String email)
     * MỤC ĐÍCH: Lấy object NguoiDung theo email
     *
     * GIẢI THÍCH:
     * - Tìm người dùng có email cụ thể
     * - Trả về object hoặc null nếu không tìm thấy
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * @param email Email cần tìm
     * @return NguoiDung object hoặc null
     */
    public NguoiDung layNguoiDungTheoEmail(String email) {
        synchronized (LOCK) {
            Map<String, NguoiDung> danhSach = layTatCa();
            return danhSach.get(email);
        }
    }

    /**
     * PHƯƠNG THỨC: diChuyenMatKhau()
     * MỤC ĐÍCH: Migrate dữ liệu cũ (plain text password) sang hashed password
     *
     * GIẢI THÍCH CHI TIẾT:
     * 1. Đọc tất cả người dùng từ file
     * 2. Với mỗi người dùng:
     *    - Kiểm tra salt có trống không
     *    - Nếu trống = mật khẩu cũ (plain text), cần mã hóa
     * 3. Tạo salt ngẫu nhiên
     * 4. Mã hóa mật khẩu cũ với salt mới
     * 5. Cập nhật field matKhau và salt dùng Reflection
     * 6. Lưu lại vào file
     *
     * VỀ REFLECTION:
     * - Dùng vì matKhau và salt là private, không có setter
     * - Reflection cho phép access/modify private fields
     * - Cách:
     *   1. getDeclaredField("fieldName") lấy field
     *   2. setAccessible(true) bỏ qua private restriction
     *   3. set() modify giá trị field
     *
     * THREAD-SAFE: Dùng synchronized
     *
     * GỌI LẦN: Được gọi trong constructor chỉ 1 lần khi app khởi động
     */
    public void diChuyenMatKhau() {
        synchronized (LOCK) {
            // Đọc dữ liệu hiện tại
            DuLieuNguoiDung duLieu = docTuFile();

            // Flag để check xem có thay đổi dữ liệu không
            boolean daThayDoi = false;

            // Duyệt qua tất cả người dùng
            for (NguoiDung nguoiDung : duLieu.danhSach.values()) {
                // Kiểm tra: salt có trống không?
                // Nếu salt rỗng = mật khẩu chưa được mã hóa = dữ liệu cũ
                if (nguoiDung.laySalt() == null || nguoiDung.laySalt().isEmpty()) {

                    // Bước 1: Tạo salt ngẫu nhiên mới
                    String salt = com.mycompany.utils.BoMaHoaMatKhau.taoSalt();

                    // Bước 2: Mã hóa mật khẩu cũ (plain text) với salt mới
                    String hashedPassword = com.mycompany.utils.BoMaHoaMatKhau.maHoaMatKhau(
                        nguoiDung.layMatKhau(),
                        salt
                    );

                    // Bước 3: Cập nhật field dùng Reflection
                    // Vì matKhau và salt là private, không có setter public
                    try {
                        // Lấy class cha (ConNguoi) - vì NguoiDung extend ConNguoi
                        // getClass().getSuperclass() = ConNguoi
                        Field matKhauField = nguoiDung.getClass().getSuperclass().getDeclaredField("matKhau");
                        // Cho phép access private field
                        matKhauField.setAccessible(true);
                        // Set giá trị mới (hashed password)
                        matKhauField.set(nguoiDung, hashedPassword);

                        // Làm tương tự cho salt field
                        Field saltField = nguoiDung.getClass().getSuperclass().getDeclaredField("salt");
                        saltField.setAccessible(true);
                        saltField.set(nguoiDung, salt);

                        // Đánh dấu rằng có thay đổi
                        daThayDoi = true;
                    } catch (Exception e) {
                        System.err.println("Lỗi di chuyển mật khẩu: " + e.getMessage());
                    }
                }
            }

            // Nếu có thay đổi, lưu lại vào file
            if (daThayDoi) {
                ghiVaoFile(duLieu);
                System.out.println("Đã di chuyển mật khẩu cũ sang mã hóa.");
            }
        }
    }
}