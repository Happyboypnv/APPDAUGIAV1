package com.mycompany.Action;

import com.mycompany.models.GiaoDich;
import java.util.List;

/**
 * Interface định nghĩa hợp đồng (contract) cho kho lưu trữ giao dịch.
 *
 * Bất kỳ class nào muốn đóng vai trò "nơi lưu giao dịch" đều phải
 * implement interface này — dù là lưu vào file JSON, database, hay bộ nhớ.
 *
 * Các chức năng bắt buộc phải có:
 *  - luu()              : lưu một giao dịch mới vào kho
 *  - layTatCa()         : lấy toàn bộ danh sách giao dịch
 *  - layTheoId()        : tìm một giao dịch theo mã ID
 *  - layTheoEmail()     : lấy danh sách giao dịch của một người dùng (theo email)
 *  - capNhat()          : cập nhật lại thông tin giao dịch (ví dụ: đổi trạng thái)
 */
public interface IKhoLuuTruGiaoDich {

    /**
     * Lưu một giao dịch mới vào kho.
     * ID của giao dịch sẽ được tự động tạo bên trong kho.
     *
     * @param giaoDich đối tượng giao dịch cần lưu
     */
    void luu(GiaoDich giaoDich);

    /**
     * Lấy toàn bộ danh sách giao dịch hiện có trong kho.
     *
     * @return danh sách tất cả giao dịch (có thể rỗng, không bao giờ null)
     */
    List<GiaoDich> layTatCa();

    /**
     * Tìm một giao dịch theo mã ID duy nhất.
     *
     * @param id mã định danh của giao dịch (ví dụ: "GD000001")
     * @return đối tượng GiaoDich nếu tìm thấy, hoặc null nếu không có
     */
    GiaoDich layTheoId(String id);

    /**
     * Lấy toàn bộ giao dịch của một người dùng cụ thể (dựa theo email).
     * Bao gồm cả giao dịch với tư cách người mua lẫn người bán.
     *
     * @param email địa chỉ email của người dùng cần tra cứu
     * @return danh sách giao dịch liên quan (có thể rỗng, không bao giờ null)
     */
    List<GiaoDich> layTheoEmail(String email);

    /**
     * Cập nhật thông tin của một giao dịch đã tồn tại trong kho.
     * Thường dùng để đổi trạng thái (ví dụ: CHO_THANH_TOAN → DA_THANH_TOAN).
     *
     * @param giaoDich đối tượng giao dịch đã được chỉnh sửa, có cùng ID với bản cũ
     */
    void capNhat(GiaoDich giaoDich);
}