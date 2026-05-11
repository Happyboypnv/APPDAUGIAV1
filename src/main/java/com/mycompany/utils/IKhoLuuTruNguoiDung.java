package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;

/**
 * IKhoLuuTruNguoiDung - Interface định nghĩa hợp đồng lưu trữ người dùng
 *
 * MỤC ĐÍCH:
 * - Định nghĩa các phương thức cần thiết để lưu/lấy/kiểm tra dữ liệu người dùng
 * - Sử dụng thiết kế pattern: Strategy Pattern
 * - Cho phép thay thế implementation (hiện tại dùng JSON, tương lai có thể dùng Database)
 *
 * LỢI ÍCH:
 * - Loose coupling: Controller không phụ thuộc vào cách lưu trữ cụ thể
 * - Dễ thay đổi: Nếu muốn đổi từ JSON sang Database, chỉ cần tạo class mới implement interface này
 * - Dễ test: Có thể tạo mock implementation để test
 *
 * HIỆN TẠI:
 * - Implementation: KhoLuuTruNguoiDungJson (lưu vào file JSON)
 *
 * TƯƠNG LAI:
 * - Có thể tạo: KhoLuuTruNguoiDungSQL (lưu vào database)
 */
public interface IKhoLuuTruNguoiDung {

    /**
     * PHƯƠNG THỨC: luu(NguoiDung nguoiDung)
     * MỤC ĐÍCH: Lưu một người dùng mới vào kho lưu trữ
     *
     * GIẢI THÍCH:
     * - Dùng khi người dùng đăng ký (sign up) tài khoản mới
     * - Sẽ gán ID tự động (maCuoiCung++)
     * - Lưu đầy đủ thông tin người dùng vào file JSON hoặc database
     *
     * @param nguoiDung Đối tượng NguoiDung cần lưu
     *                  Lưu ý: ID sẽ được gán tự động, không cần set trước
     */
    void luu(NguoiDung nguoiDung);
    /**
     * PHƯƠNG THỨC: capNhatNguoiDung(NguoiDung nguoiDung)
     * MỤC ĐÍCH: Cập nhật thông tin của một người dùng đã tồn tại
     *
     * GIẢI THÍCH:
     * - Dùng khi người dùng chỉnh sửa hồ sơ cá nhân (profile)
     * - Khác với luu(): không tạo ID mới, giữ ID cũ
     * - Sẽ ghi đè toàn bộ thông tin bản ghi cũ
     *
     * @param nguoiDung Đối tượng NguoiDung đã được cập nhật
     *                  Phải có ID và maCuoiCung để xác định bản ghi nào cập nhật
     */
    void capNhatNguoiDung(NguoiDung nguoiDung);

    /**
     * PHƯƠNG THỨC: layTatCa()
     * MỤC ĐÍCH: Lấy toàn bộ danh sách người dùng
     *
     * GIẢI THÍCH:
     * - Trả về Map<Email, NguoiDung> - email là key (định danh duy nhất)
     * - Dùng để kiểm tra đăng nhập, kiểm tra email đã tồn tại, etc.
     * - Đọc từ file JSON hoặc database
     *
     * @return Map chứa tất cả người dùng, key là email
     *         Ví dụ: {"admin@gmail.com": NguoiDung object, "user@gmail.com": NguoiDung object}
     */
    Map<String, NguoiDung> layTatCa();

    /**
     * PHƯƠNG THỨC: kiemTraNguoiDung(String email, String password)
     * MỤC ĐÍCH: Xác nhận thông tin đăng nhập (authentication)
     *
     * GIẢI THÍCH:
     * - Kiểm tra xem email + password có khớp với bất kỳ người dùng nào không
     * - Dùng khi người dùng nhập form đăng nhập (login)
     * - So sánh mật khẩu được mã hóa để bảo mật
     *
     * @param email Email của người dùng
     * @param password Mật khẩu gốc (plain text) do người dùng nhập
     * @return true nếu email tồn tại và password đúng, false nếu sai
     */
    boolean kiemTraNguoiDung(String email, String password);

    /**
     * PHƯƠNG THỨC: kiemTraEmail(String email)
     * MỤC ĐÍCH: Kiểm tra xem email đã được đăng ký hay chưa
     *
     * GIẢI THÍCH:
     * - Dùng khi người dùng đăng ký (sign up) tài khoản mới
     * - Nên kiểm tra email chưa tồn tại trước khi lưu
     * - Tránh trùng lặp email trong hệ thống
     *
     * @param email Email cần kiểm tra
     * @return true nếu email CHƯA tồn tại (có thể dùng)
     *         false nếu email ĐÃ tồn tại (không thể dùng)
     *
     * LƯU Ý: Phương thức này trả về ngược so với containsKey()
     *        - containsKey("admin@gmail.com") = true nếu tồn tại
     *        - kiemTraEmail("admin@gmail.com") = false nếu tồn tại
     */
    boolean kiemTraEmail(String email);

    /**
     * PHƯƠNG THỨC: xoa(NguoiDung nguoiDung)
     * MỤC ĐÍCH: Xóa một người dùng khỏi hệ thống
     *
     * @param nguoiDung Đối tượng NguoiDung cần xóa
     */
    void xoa(NguoiDung nguoiDung);
}