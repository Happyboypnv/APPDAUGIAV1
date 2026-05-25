package com.mycompany.utils;

import com.mycompany.models.Transaction;
import com.mycompany.models.TransactionStatus;

import java.util.Map;
import java.util.List;

/**
 * IKhoLuuTruTransaction - Interface định nghĩa hợp đồng lưu trữ giao dịch
 *
 * MỤC ĐÍCH:
 * - Định nghĩa các phương thức cần thiết để lưu/lấy/kiểm tra dữ liệu giao dịch
 * - Sử dụng thiết kế pattern: Strategy Pattern
 * - Cho phép thay thế implementation (SQLite Database)
 *
 * LỢI ÍCH:
 * - Loose coupling: Controller không phụ thuộc vào cách lưu trữ cụ thể
 * - Dễ thay đổi: Nếu muốn đổi database, chỉ cần tạo class mới implement interface này
 * - Dễ test: Có thể tạo mock implementation để test
 */
public interface ITransactionRepository {

    /**
     * Lưu một giao dịch mới
     * @param transaction giao dịch cần lưu
     */
    void save(Transaction transaction);

    /**
     * Lấy tất cả giao dịch
     * @return Map với key = maTransaction, value = Transaction object
     */
    Map<String, Transaction> findAll();

    /**
     * Lấy một giao dịch theo mã
     * @param maTransaction mã giao dịch cần tìm
     * @return Transaction object hoặc null nếu không tìm thấy
     */
    Transaction findById(String maTransaction);

    /**
     * Xóa một giao dịch theo mã
     * @param maTransaction mã giao dịch cần xóa
     * @return true nếu xóa thành công, false nếu thất bại
     */
    boolean delete(String maTransaction);

    /**
     * Kiểm tra giao dịch có tồn tại hay không
     * @param maTransaction mã giao dịch cần kiểm tra
     * @return true nếu tồn tại, false nếu không
     */
    boolean isTransactionAvailable(String maTransaction);

    /**
     * Cập nhật giao dịch (thay đổi trạng thái hoặc thông tin)
     * @param Transaction giao dịch đã được cập nhật
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    boolean update(Transaction Transaction);

    /**
     * Lấy danh sách giao dịch của một người dùng (bán hoặc mua)
     * @param maNguoiDung mã người dùng
     * @return List các giao dịch liên quan đến người dùng
     */
    List<Transaction> findByUserId(String maNguoiDung);

    /**
     * Lấy danh sách giao dịch theo trạng thái
     * @param trangThai trạng thái giao dịch cần tìm
     * @return List các giao dịch có trạng thái tương ứng
     */
    List<Transaction> findByStatus(TransactionStatus trangThai);
}