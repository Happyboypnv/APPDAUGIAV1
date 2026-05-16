package com.mycompany.action;

import com.mycompany.models.GiaoDich;
import com.mycompany.models.TrangThaiGiaoDich;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.TrangThaiPhien;
import com.mycompany.utils.ITransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GiaoDichService {
    private static final Logger logger = LoggerFactory.getLogger(GiaoDichService.class);
    private final ITransactionRepository kho;

    public GiaoDichService(ITransactionRepository kho) {
        this.kho = kho;
    }

    // =========================================================
    // CHỨC NĂNG 1: TẠO GIAO DỊCH MỚI
    // =========================================================
    public GiaoDich taoGiaoDich(PhienDauGia phien) {
        if (phien.getTrangThai() != TrangThaiPhien.DA_THANH_TOAN) {
            logger.info("[taoGiaoDich] Phiên chưa kết thúc hoặc bị hủy. Trạng thái: {}", phien.getTrangThai());
            return null;
        }

        NguoiDung nguoiThang = phien.getNguoiThangCuoc();
        if (nguoiThang == null) {
            logger.info("[taoGiaoDich] Không thể tạo: không có người đặt giá.");
            return null;
        }

        GiaoDich giaoDichMoi = new GiaoDich("TEMP", phien);
        nguoiThang.themGiaoDich(giaoDichMoi);
        phien.getNguoiBan().themGiaoDich(giaoDichMoi);
        kho.luuGiaoDich(giaoDichMoi);

        logger.info("========================================");
        logger.info("[taoGiaoDich] Giao dịch tạo thành công!");
        logger.info("  Sản phẩm  : {}", phien.getSanPham().layTenSanPham());
        logger.info("  Người bán : {}", phien.getNguoiBan().layHoTen());
        logger.info("  Người mua : {}", nguoiThang.layHoTen());
        // FIX: Sử dụng String.format để định dạng số tiền
        logger.info("  Giá chốt  : {} VNĐ", String.format("%,.0f", phien.getGiaHienTai()));
        logger.info("  Trạng thái: CHỜ THANH TOÁN");
        logger.info("========================================");

        return giaoDichMoi;
    }

    // =========================================================
    // CHỨC NĂNG 2: XÁC NHẬN THANH TOÁN
    // =========================================================
    public boolean xacNhanThanhToan(GiaoDich giaoDich) {
        if (giaoDich == null) {
            logger.info("[xacNhanThanhToan] Giao dịch không tồn tại.");
            return false;
        }

        if (giaoDich.getTrangThai() != TrangThaiGiaoDich.CHO_THANH_TOAN) {
            logger.info("[xacNhanThanhToan] Trạng thái không hợp lệ: {}", giaoDich.getTrangThai());
            return false;
        }

        NguoiDung nguoiThang = giaoDich.getPhienDauGia().getNguoiThangCuoc();
        NguoiDung nguoiBan   = giaoDich.getPhienDauGia().getNguoiBan();
        double giaChot       = giaoDich.getPhienDauGia().getGiaHienTai();

        if (nguoiThang.getSoDuKhaDung() < giaChot) {
            // FIX: Sử dụng String.format
            logger.info("[xacNhanThanhToan] Số dư không đủ. Cần: {} | Có: {} VNĐ",
                    String.format("%,.0f", giaChot),
                    String.format("%,.0f", nguoiThang.getSoDuKhaDung()));
            return false;
        }

        nguoiThang.setSoDuKhaDung(nguoiThang.getSoDuKhaDung() - giaChot);
        nguoiBan.setSoDuKhaDung(nguoiBan.getSoDuKhaDung() + giaChot);
        giaoDich.setTrangThai(TrangThaiGiaoDich.DA_THANH_TOAN);
        kho.luuGiaoDich(giaoDich);

        logger.info("========================================");
        logger.info("[xacNhanThanhToan] Thanh toán thành công!");
        logger.info("  Giao dịch : {}", giaoDich.getId());
        // FIX: Định dạng số tiền
        logger.info("  Số tiền   : {} VNĐ", String.format("%,.0f", giaChot));
        logger.info("  Người mua : {} | Số dư còn: {} VNĐ",
                nguoiThang.layHoTen(),
                String.format("%,.0f", nguoiThang.getSoDuKhaDung()));
        logger.info("  [Thông báo -> {}]: Bạn vừa nhận thanh toán cho giao dịch {}",
                nguoiBan.layHoTen(), giaoDich.getId());
        logger.info("  Số dư người bán: {} VNĐ", String.format("%,.0f", nguoiBan.getSoDuKhaDung()));
        logger.info("========================================");

        return true;
    }

    // =========================================================
    // CHỨC NĂNG 3: HOÀN TIỀN
    // =========================================================
    public boolean hoanTien(GiaoDich giaoDich, String lyDo) {
        if (giaoDich == null) {
            logger.info("[hoanTien] Giao dịch không tồn tại.");
            return false;
        }

        if (giaoDich.getTrangThai() == TrangThaiGiaoDich.DA_HOAN_TIEN) {
            logger.info("[hoanTien] Giao dịch đã được hoàn tiền trước đó.");
            return false;
        }

        NguoiDung nguoiThang = giaoDich.getPhienDauGia().getNguoiThangCuoc();
        NguoiDung nguoiBan   = giaoDich.getPhienDauGia().getNguoiBan();
        double soTienHoan    = giaoDich.getPhienDauGia().getGiaHienTai();

        if (nguoiThang == null) {
            logger.info("[hoanTien] Không tìm thấy người cần hoàn tiền.");
            return false;
        }

        if (giaoDich.getTrangThai() == TrangThaiGiaoDich.DA_THANH_TOAN) {
            nguoiBan.setSoDuKhaDung(nguoiBan.getSoDuKhaDung() - soTienHoan);
        }

        nguoiThang.setSoDuKhaDung(nguoiThang.getSoDuKhaDung() + soTienHoan);
        giaoDich.setTrangThai(TrangThaiGiaoDich.DA_HOAN_TIEN);
        kho.capNhatGiaoDich(giaoDich);

        logger.info("========================================");
        logger.info("[hoanTien] Hoàn tiền thành công!");
        logger.info("  Giao dịch       : {}", giaoDich.getId());
        logger.info("  Người nhận hoàn : {}", nguoiThang.layHoTen());
        // FIX: Định dạng số tiền
        logger.info("  Số tiền hoàn    : {} VNĐ", String.format("%,.0f", soTienHoan));
        logger.info("  Số dư sau hoàn  : {} VNĐ", String.format("%,.0f", nguoiThang.getSoDuKhaDung()));
        logger.info("  Lý do           : {}", lyDo);
        logger.info("========================================");

        return true;
    }

    // =========================================================
    // CHỨC NĂNG 4: XEM LỊCH SỬ GIAO DỊCH
    // =========================================================
    public List<GiaoDich> xemLichSuGiaoDich(String maNguoiDung, int trang, int soLuong) {
        List<GiaoDich> tatCa = kho.layGiaoDichTheoNguoiDung(maNguoiDung);

        if (tatCa.isEmpty()) {
            logger.info("[xemLichSu] Người dùng [{}] chưa có giao dịch nào.", maNguoiDung);
            return tatCa;
        }

        int tongTrang = (int) Math.ceil((double) tatCa.size() / soLuong);
        int batDau    = (trang - 1) * soLuong;

        if (batDau >= tatCa.size()) {
            logger.info("[xemLichSu] Không có dữ liệu ở trang {} (tổng {} trang).", trang, tongTrang);
            return List.of();
        }

        int ketThuc = Math.min(batDau + soLuong, tatCa.size());
        List<GiaoDich> trangHienTai = tatCa.subList(batDau, ketThuc);

        logger.info("==========================================");
        logger.info("         LỊCH SỬ GIAO DỊCH               ");
        logger.info("==========================================");
        logger.info("  Người dùng : {}", maNguoiDung);
        // FIX: Định dạng dòng tiêu đề trang
        logger.info("  Trang {} / {}  |  Tổng: {} giao dịch", trang, tongTrang, tatCa.size());
        logger.info("------------------------------------------");

        for (int i = 0; i < trangHienTai.size(); i++) {
            GiaoDich gd        = trangHienTai.get(i);
            PhienDauGia phien  = gd.getPhienDauGia();
            NguoiDung nguoiMua = phien.getNguoiThangCuoc();

            // FIX: Sử dụng placeholder {} thay vì %d, %s và định dạng tiền
            logger.info("  [{}] Mã GD     : {}", batDau + i + 1, gd.getId());
            logger.info("      Ngày tạo  : {}", gd.getThoiGianTao());
            logger.info("      Sản phẩm  : {}", phien.getSanPham().layTenSanPham());
            logger.info("      Người bán : {}", phien.getNguoiBan().layHoTen());
            logger.info("      Người mua : {}", (nguoiMua != null ? nguoiMua.layHoTen() : "Không có"));
            logger.info("      Số tiền   : {} VNĐ", String.format("%,.0f", phien.getGiaHienTai()));
            logger.info("      Trạng thái: {}", gd.getTrangThai().name());
            logger.info("  ------------------------------------------");
        }

        return trangHienTai;
    }
}