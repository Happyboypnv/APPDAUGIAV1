package com.mycompany.Action;

import com.mycompany.models.GiaoDich;
import com.mycompany.models.GiaoDich.TrangThaiGiaoDich;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.TrangThaiPhien;

import java.util.List;

/**
 * GiaoDichService — Lớp xử lý toàn bộ nghiệp vụ liên quan đến giao dịch đấu giá.
 *
 * Đây là tầng Service (Service Layer), chịu trách nhiệm:
 *  - Kiểm tra điều kiện hợp lệ trước khi thực hiện bất kỳ thao tác nào
 *  - Xử lý logic nghiệp vụ: trừ/cộng tiền, đổi trạng thái giao dịch
 *  - Gọi kho lưu trữ (IKhoLuuTruGiaoDich) để ghi/đọc dữ liệu vào file JSON
 *
 * Bốn chức năng chính:
 *  1. taoGiaoDich()       — Tạo giao dịch mới khi phiên đấu giá kết thúc
 *  2. xacNhanThanhToan()  — Người thắng chuyển tiền, cập nhật trạng thái, thông báo người bán
 *  3. hoanTien()          — Hoàn tiền khi hủy phiên hoặc khiếu nại, lưu lịch sử
 *  4. xemLichSuGiaoDich() — Xem lịch sử giao dịch của user, hiển thị chi tiết, phân trang
 *
 * Cách dùng:
 *   GiaoDichService service = new GiaoDichService(new KhoLuuTruGiaoDichJson());
 */
public class GiaoDichService {

    // ===== PHỤ THUỘC =====

    /**
     * Kho lưu trữ giao dịch — được inject từ bên ngoài qua constructor.
     * Dùng interface thay vì class cụ thể để dễ thay đổi (JSON → Database)
     * mà không cần sửa code ở đây (nguyên tắc Dependency Inversion - SOLID).
     */
    private final IKhoLuuTruGiaoDich kho;

    // ===== CONSTRUCTOR =====

    /**
     * Khởi tạo GiaoDichService với kho lưu trữ cụ thể.
     *
     * @param kho implementation lưu trữ, ví dụ: new KhoLuuTruGiaoDichJson()
     */
    public GiaoDichService(IKhoLuuTruGiaoDich kho) {
        this.kho = kho;
    }

    // =========================================================
    // CHỨC NĂNG 1: TẠO GIAO DỊCH MỚI
    // =========================================================

    /**
     * Tạo giao dịch mới khi một phiên đấu giá kết thúc.
     *
     * Điều kiện hợp lệ:
     *  - Phiên phải ở trạng thái KET_THUC
     *  - Phải có người thắng cuộc (tức là có ít nhất một người đã đặt giá)
     *
     * Quy trình:
     *  1. Kiểm tra phiên đã kết thúc chưa
     *  2. Kiểm tra có người thắng không
     *  3. Tạo đối tượng GiaoDich (trạng thái mặc định: CHO_THANH_TOAN)
     *  4. Liên kết giao dịch vào lịch sử của người thắng (người mua)
     *  5. Liên kết giao dịch vào lịch sử của người bán
     *  6. Lưu giao dịch vào kho JSON (kho tự sinh ID: GD000001, GD000002...)
     *
     * @param phien phiên đấu giá đã kết thúc
     * @return đối tượng GiaoDich vừa tạo, hoặc null nếu không hợp lệ
     */
    public GiaoDich taoGiaoDich(PhienDauGia phien) {

        // Bước 1: Phiên phải ở trạng thái KẾT THÚC
        if (phien.getTrangThai() != TrangThaiPhien.KET_THUC) {
            System.out.println("[taoGiaoDich] Không thể tạo: phiên chưa kết thúc.");
            return null;
        }

        // Bước 2: Phải có người thắng cuộc
        NguoiDung nguoiThang = phien.getNguoiThangCuoc();
        if (nguoiThang == null) {
            System.out.println("[taoGiaoDich] Không thể tạo: không có người đặt giá.");
            return null;
        }

        // Bước 3: Tạo giao dịch mới
        // ID tạm "TEMP" — kho sẽ tự sinh ID thật khi lưu
        GiaoDich giaoDichMoi = new GiaoDich("TEMP", phien);

        // Bước 4: Liên kết vào lịch sử của người thắng (người mua)
        nguoiThang.themGiaoDich(giaoDichMoi);

        // Bước 5: Liên kết vào lịch sử của người bán
        phien.getNguoiBan().themGiaoDich(giaoDichMoi);

        // Bước 6: Lưu vào kho (file dulieugiaodich.json)
        kho.luu(giaoDichMoi);

        System.out.println("========================================");
        System.out.println("[taoGiaoDich] Giao dịch tạo thành công!");
        System.out.println("  Sản phẩm  : " + phien.getSanPham().layTenSanPham());
        System.out.println("  Người bán : " + phien.getNguoiBan().layHoTen());
        System.out.println("  Người mua : " + nguoiThang.layHoTen());
        System.out.printf ("  Giá chốt  : %,.0f VNĐ%n", phien.getGiaHienTai());
        System.out.println("  Trạng thái: CHỜ THANH TOÁN");
        System.out.println("========================================");

        return giaoDichMoi;
    }

    // =========================================================
    // CHỨC NĂNG 2: XÁC NHẬN THANH TOÁN
    // =========================================================

    /**
     * Xác nhận người thắng cuộc đã chuyển tiền thành công cho người bán.
     *
     * Điều kiện hợp lệ:
     *  - Giao dịch không được null
     *  - Giao dịch phải đang ở trạng thái CHO_THANH_TOAN
     *  - Số dư của người thắng phải đủ để thanh toán
     *
     * Quy trình:
     *  1. Kiểm tra điều kiện hợp lệ
     *  2. Trừ tiền từ số dư người thắng (người mua chuyển tiền)
     *  3. Cộng tiền vào số dư người bán (người bán nhận tiền)
     *  4. Cập nhật trạng thái giao dịch → DA_THANH_TOAN
     *  5. Lưu cập nhật vào kho
     *  6. Thông báo cho người bán
     *
     * @param giaoDich giao dịch cần xác nhận thanh toán
     * @return true nếu thành công, false nếu không hợp lệ
     */
    public boolean xacNhanThanhToan(GiaoDich giaoDich) {

        // Kiểm tra giao dịch tồn tại
        if (giaoDich == null) {
            System.out.println("[xacNhanThanhToan] Giao dịch không tồn tại.");
            return false;
        }

        // Chỉ xử lý khi đang CHỜ THANH TOÁN
        if (giaoDich.getTrangThai() != TrangThaiGiaoDich.CHO_THANH_TOAN) {
            System.out.println("[xacNhanThanhToan] Trạng thái không hợp lệ: " + giaoDich.getTrangThai());
            return false;
        }

        NguoiDung nguoiThang = giaoDich.getPhienDauGia().getNguoiThangCuoc(); // người mua
        NguoiDung nguoiBan   = giaoDich.getPhienDauGia().getNguoiBan();        // người bán
        double giaChot       = giaoDich.getPhienDauGia().getGiaHienTai();      // số tiền

        // Kiểm tra số dư người thắng có đủ không
        if (nguoiThang.getSoDuKhaDung() < giaChot) {
            System.out.printf("[xacNhanThanhToan] Số dư không đủ. Cần: %,.0f | Có: %,.0f VNĐ%n",
                    giaChot, nguoiThang.getSoDuKhaDung());
            return false;
        }

        // Bước 2: Người thắng chuyển tiền — trừ khỏi số dư người mua
        nguoiThang.setSoDuKhaDung(nguoiThang.getSoDuKhaDung() - giaChot);

        // Bước 3: Người bán nhận tiền — cộng vào số dư người bán
        nguoiBan.setSoDuKhaDung(nguoiBan.getSoDuKhaDung() + giaChot);

        // Bước 4: Cập nhật trạng thái → ĐÃ THANH TOÁN
        giaoDich.setTrangThai(TrangThaiGiaoDich.DA_THANH_TOAN);

        // Bước 5: Lưu cập nhật vào kho
        kho.capNhat(giaoDich);

        // Bước 6: Thông báo cho người bán
        System.out.println("========================================");
        System.out.println("[xacNhanThanhToan] Thanh toán thành công!");
        System.out.println("  Giao dịch : " + giaoDich.getId());
        System.out.printf ("  Số tiền   : %,.0f VNĐ%n", giaChot);
        System.out.println("  Người mua : " + nguoiThang.layHoTen()
                + String.format(" | Số dư còn: %,.0f VNĐ", nguoiThang.getSoDuKhaDung()));
        System.out.println("  [Thông báo → " + nguoiBan.layHoTen() + "]: "
                + "Bạn vừa nhận thanh toán cho giao dịch " + giaoDich.getId());
        System.out.printf ("  Số dư người bán: %,.0f VNĐ%n", nguoiBan.getSoDuKhaDung());
        System.out.println("========================================");

        return true;
    }

    // =========================================================
    // CHỨC NĂNG 3: HOÀN TIỀN
    // =========================================================

    /**
     * Hoàn tiền cho người thắng cuộc khi phiên bị hủy hoặc có khiếu nại.
     *
     * Điều kiện hợp lệ:
     *  - Giao dịch không được null
     *  - Giao dịch không được ở trạng thái DA_HOAN_TIEN (tránh hoàn 2 lần)
     *
     * Quy trình:
     *  1. Kiểm tra điều kiện hợp lệ
     *  2. Nếu đã thanh toán rồi → thu lại tiền từ người bán trước
     *     (vì xacNhanThanhToan đã cộng cho người bán, giờ phải thu lại)
     *  3. Cộng tiền lại vào tài khoản người thắng (người mua được hoàn)
     *  4. Cập nhật trạng thái → DA_HOAN_TIEN
     *  5. Lưu lịch sử vào kho
     *
     * @param giaoDich giao dịch cần hoàn tiền
     * @param lyDo     lý do hoàn tiền (hủy phiên / khiếu nại / lý do khác)
     * @return true nếu hoàn tiền thành công, false nếu không hợp lệ
     */
    public boolean hoanTien(GiaoDich giaoDich, String lyDo) {

        // Kiểm tra giao dịch tồn tại
        if (giaoDich == null) {
            System.out.println("[hoanTien] Giao dịch không tồn tại.");
            return false;
        }

        // Không hoàn tiền lại nếu đã hoàn rồi
        if (giaoDich.getTrangThai() == TrangThaiGiaoDich.DA_HOAN_TIEN) {
            System.out.println("[hoanTien] Giao dịch đã được hoàn tiền trước đó.");
            return false;
        }

        NguoiDung nguoiThang = giaoDich.getPhienDauGia().getNguoiThangCuoc();
        NguoiDung nguoiBan   = giaoDich.getPhienDauGia().getNguoiBan();
        double soTienHoan    = giaoDich.getPhienDauGia().getGiaHienTai();

        if (nguoiThang == null) {
            System.out.println("[hoanTien] Không tìm thấy người cần hoàn tiền.");
            return false;
        }

        // Bước 2: Nếu đã thanh toán → thu lại tiền từ người bán
        if (giaoDich.getTrangThai() == TrangThaiGiaoDich.DA_THANH_TOAN) {
            nguoiBan.setSoDuKhaDung(nguoiBan.getSoDuKhaDung() - soTienHoan);
        }

        // Bước 3: Cộng tiền lại vào tài khoản người thắng
        nguoiThang.setSoDuKhaDung(nguoiThang.getSoDuKhaDung() + soTienHoan);

        // Bước 4: Cập nhật trạng thái → ĐÃ HOÀN TIỀN
        giaoDich.setTrangThai(TrangThaiGiaoDich.DA_HOAN_TIEN);

        // Bước 5: Lưu lịch sử vào kho
        kho.capNhat(giaoDich);

        System.out.println("========================================");
        System.out.println("[hoanTien] Hoàn tiền thành công!");
        System.out.println("  Giao dịch       : " + giaoDich.getId());
        System.out.println("  Người nhận hoàn : " + nguoiThang.layHoTen());
        System.out.printf ("  Số tiền hoàn    : %,.0f VNĐ%n", soTienHoan);
        System.out.printf ("  Số dư sau hoàn  : %,.0f VNĐ%n", nguoiThang.getSoDuKhaDung());
        System.out.println("  Lý do           : " + lyDo);
        System.out.println("========================================");

        return true;
    }

    // =========================================================
    // CHỨC NĂNG 4: XEM LỊCH SỬ GIAO DỊCH
    // =========================================================

    /**
     * Lấy danh sách giao dịch của một người dùng và hiển thị chi tiết có phân trang.
     *
     * Bao gồm cả giao dịch với tư cách người bán lẫn người mua.
     *
     * Hiển thị chi tiết mỗi giao dịch:
     *  - Mã giao dịch
     *  - Ngày giờ tạo giao dịch
     *  - Tên sản phẩm, người bán, người mua
     *  - Số tiền (giá chốt)
     *  - Trạng thái: CHO_THANH_TOAN / DA_THANH_TOAN / DA_HOAN_TIEN
     *
     * Phân trang:
     *  - trang   : trang muốn xem, bắt đầu từ 1
     *  - soLuong : số giao dịch tối đa hiển thị trên mỗi trang
     *
     * @param email    email người dùng cần xem lịch sử
     * @param trang    số trang muốn xem (bắt đầu từ 1)
     * @param soLuong  số giao dịch tối đa mỗi trang
     * @return danh sách giao dịch của trang hiện tại (rỗng nếu không có)
     */
    public List<GiaoDich> xemLichSuGiaoDich(String email, int trang, int soLuong) {

        // Lấy toàn bộ giao dịch của người dùng từ kho
        List<GiaoDich> tatCa = kho.layTheoEmail(email);

        // Không có giao dịch nào
        if (tatCa.isEmpty()) {
            System.out.println("[xemLichSu] Người dùng [" + email + "] chưa có giao dịch nào.");
            return tatCa;
        }

        // Tính toán phân trang
        int tongTrang = (int) Math.ceil((double) tatCa.size() / soLuong);
        int batDau    = (trang - 1) * soLuong; // chỉ số bắt đầu (0-based)

        if (batDau >= tatCa.size()) {
            System.out.println("[xemLichSu] Không có dữ liệu ở trang " + trang
                    + " (tổng " + tongTrang + " trang).");
            return List.of();
        }

        int ketThuc = Math.min(batDau + soLuong, tatCa.size());
        List<GiaoDich> trangHienTai = tatCa.subList(batDau, ketThuc);

        // In tiêu đề
        System.out.println("==========================================");
        System.out.println("         LỊCH SỬ GIAO DỊCH               ");
        System.out.println("==========================================");
        System.out.println("  Người dùng : " + email);
        System.out.printf ("  Trang %d / %d  |  Tổng: %d giao dịch%n",
                trang, tongTrang, tatCa.size());
        System.out.println("------------------------------------------");

        // In chi tiết từng giao dịch
        for (int i = 0; i < trangHienTai.size(); i++) {
            GiaoDich gd        = trangHienTai.get(i);
            PhienDauGia phien  = gd.getPhienDauGia();
            NguoiDung nguoiMua = phien.getNguoiThangCuoc();

            System.out.printf("  [%d] Mã GD     : %s%n", batDau + i + 1, gd.getId());
            System.out.println("      Ngày tạo  : " + gd.getThoiGianTao());                         // ngày giờ
            System.out.println("      Sản phẩm  : " + phien.getSanPham().layTenSanPham());
            System.out.println("      Người bán : " + phien.getNguoiBan().layHoTen());
            System.out.println("      Người mua : " + (nguoiMua != null ? nguoiMua.layHoTen() : "Không có"));
            System.out.printf ("      Số tiền   : %,.0f VNĐ%n", phien.getGiaHienTai());             // số tiền
            System.out.println("      Trạng thái: " + gd.getTrangThai().name());                     // trạng thái
            System.out.println("  ------------------------------------------");
        }

        return trangHienTai;
    }
}