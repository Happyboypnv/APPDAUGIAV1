package com.mycompany.service;

import com.mycompany.action.GiaoDichService;
import com.mycompany.models.*;
import com.mycompany.utils.ITransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GiaoDichServiceTest — Kiểm tra toàn bộ logic nghiệp vụ của GiaoDichService.
 *
 * Dùng Mockito để mock IKhoLuuTruGiaoDich:
 *   - Không cần DB thật, test chạy offline hoàn toàn.
 *   - Kiểm tra đúng hành vi (behavior) thay vì trạng thái lưu trữ.
 *
 * Cấu trúc 4 nhóm test chính:
 *   1. taoGiaoDich()        — Tạo giao dịch từ phiên đấu giá kết thúc
 *   2. xacNhanThanhToan()   — Trừ tiền người mua, cộng tiền người bán
 *   3. hoanTien()           — Hoàn tiền khi hủy giao dịch
 *   4. xemLichSuGiaoDich()  — Phân trang lịch sử giao dịch
 */
@DisplayName("GiaoDichService — Kiểm tra logic nghiệp vụ giao dịch")
@ExtendWith(MockitoExtension.class)
class GiaoDichServiceTest {

    // ===== MOCK & SUT =====

    @Mock
    private ITransactionRepository mockKho;

    private GiaoDichService service;

    // ===== DỮ LIỆU DÙNG CHUNG =====

    private NguoiDung nguoiBan;
    private NguoiDung nguoiMua;
    private SanPham sanPham;
    private PhienDauGia phienDaKetThuc;
    private GiaoDich giaoDich;

    // ===== SETUP =====

    @BeforeEach
    void setup() {
        service = new GiaoDichService(mockKho);

        // Người bán — số dư ban đầu 0
        nguoiBan = new NguoiDung("Nguyen Van Ban", "ban@test.com", "hash", "2000-01-01");
        nguoiBan.setMaNguoiDung("ND001");
        nguoiBan.setSoDuKhaDung(0);

        // Người mua — số dư 500 triệu
        nguoiMua = new NguoiDung("Tran Thi Mua", "mua@test.com", "hash", "1995-05-15");
        nguoiMua.setMaNguoiDung("ND002");
        nguoiMua.setSoDuKhaDung(500_000_000);

        sanPham = new SanPham("Đồng hồ Rolex", "SP001");

        // Phiên đã thanh toán — điều kiện để taoGiaoDich() chấp nhận
        phienDaKetThuc = new PhienDauGia(
                "PH001", "Phiên test", sanPham, 100_000_000, nguoiBan, 300
        );
        phienDaKetThuc.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
        phienDaKetThuc.addNguoiTraGia(nguoiMua);
        phienDaKetThuc.setNguoiThangCuoc(); // set nguoiMua làm người thắng
        phienDaKetThuc.setGiaHienTai(150_000_000); // giá chốt

        // Giao dịch mẫu để dùng trong các test xacNhanThanhToan / hoanTien
        giaoDich = new GiaoDich("GD000001", phienDaKetThuc);
    }

    // ================================================================
    // 1. taoGiaoDich()
    // ================================================================

    @Nested
    @DisplayName("1. taoGiaoDich()")
    class TaoGiaoDichTests {

        @Test
        @DisplayName("Phiên DA_THANH_TOAN có người thắng → tạo giao dịch thành công")
        void taoGiaoDich_hopLe_traVeGiaoDich() {
            GiaoDich result = service.taoGiaoDich(phienDaKetThuc);

            assertNotNull(result, "Giao dịch không được null khi phiên hợp lệ");
            assertEquals(TrangThaiGiaoDich.CHO_THANH_TOAN, result.getTrangThai(),
                    "Giao dịch mới tạo phải ở trạng thái CHO_THANH_TOAN");
            assertEquals(phienDaKetThuc, result.getPhienDauGia(),
                    "Giao dịch phải tham chiếu đúng phiên");
        }

        @Test
        @DisplayName("Phiên DA_THANH_TOAN có người thắng → luuGiaoDich() được gọi đúng 1 lần")
        void taoGiaoDich_hopLe_goiLuuMotLan() {
            service.taoGiaoDich(phienDaKetThuc);

            // Verify mock: đảm bảo service thật sự gọi kho.luuGiaoDich()
            verify(mockKho, times(1)).luuGiaoDich(any(GiaoDich.class));
        }

        @Test
        @DisplayName("Phiên DANG_DIEN_RA (chưa kết thúc) → trả về null, không lưu")
        void taoGiaoDich_phienChuaKetThuc_traVeNull() {
            phienDaKetThuc.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);

            GiaoDich result = service.taoGiaoDich(phienDaKetThuc);

            assertNull(result, "Phiên chưa kết thúc không được tạo giao dịch");
            verify(mockKho, never()).luuGiaoDich(any());
        }

        @Test
        @DisplayName("Phiên DA_HUY → trả về null, không lưu")
        void taoGiaoDich_phienDaHuy_traVeNull() {
            phienDaKetThuc.setTrangThai(TrangThaiPhien.DA_HUY);

            GiaoDich result = service.taoGiaoDich(phienDaKetThuc);

            assertNull(result, "Phiên đã hủy không được tạo giao dịch");
            verify(mockKho, never()).luuGiaoDich(any());
        }

        @Test
        @DisplayName("Phiên DA_THANH_TOAN nhưng không có người thắng → trả về null")
        void taoGiaoDich_khongCoNguoiThang_traVeNull() {
            // Phiên mới hoàn toàn, không có người đặt giá
            PhienDauGia phienKhongCoNguoiMua = new PhienDauGia(
                    "PH002", "Phiên trống", sanPham, 100_000_000, nguoiBan, 300
            );
            phienKhongCoNguoiMua.setTrangThai(TrangThaiPhien.DA_THANH_TOAN);
            // KHÔNG addNguoiTraGia → getNguoiThangCuoc() = null

            GiaoDich result = service.taoGiaoDich(phienKhongCoNguoiMua);

            assertNull(result, "Không có người thắng thì không tạo được giao dịch");
            verify(mockKho, never()).luuGiaoDich(any());
        }

        @Test
        @DisplayName("Tạo thành công → giao dịch được thêm vào danh sách của người mua và người bán")
        void taoGiaoDich_hopLe_themVaoDanhSachHaiPhia() {
            service.taoGiaoDich(phienDaKetThuc);

            assertFalse(nguoiMua.layCacGiaoDich().isEmpty(),
                    "Người mua phải có giao dịch sau khi tạo");
            assertFalse(nguoiBan.layCacGiaoDich().isEmpty(),
                    "Người bán phải có giao dịch sau khi tạo");
        }

        @Test
        @DisplayName("Tạo thành công → thời gian tạo không được null")
        void taoGiaoDich_hopLe_thoiGianTaoKhongNull() {
            GiaoDich result = service.taoGiaoDich(phienDaKetThuc);

            assertNotNull(result);
            assertNotNull(result.getThoiGianTao(), "Thời gian tạo giao dịch không được null");
        }
    }

    // ================================================================
    // 2. xacNhanThanhToan()
    // ================================================================

    @Nested
    @DisplayName("2. xacNhanThanhToan()")
    class XacNhanThanhToanTests {

        @Test
        @DisplayName("Số dư đủ → thanh toán thành công, trả về true")
        void xacNhan_soDuDu_trueVe() {
            // nguoiMua có 500tr, giá chốt 150tr → đủ tiền
            boolean result = service.xacNhanThanhToan(giaoDich);

            assertTrue(result, "Số dư đủ phải thanh toán thành công");
        }

        @Test
        @DisplayName("Số dư đủ → trừ đúng số tiền của người mua")
        void xacNhan_soDuDu_truTienNguoiMua() {
            double soDuTruoc = nguoiMua.getSoDuKhaDung(); // 500tr
            double giaChot = phienDaKetThuc.getGiaHienTai(); // 150tr

            service.xacNhanThanhToan(giaoDich);

            assertEquals(soDuTruoc - giaChot, nguoiMua.getSoDuKhaDung(), 0.01,
                    "Phải trừ đúng số tiền của người mua");
        }

        @Test
        @DisplayName("Số dư đủ → cộng đúng số tiền cho người bán")
        void xacNhan_soDuDu_congTienNguoiBan() {
            double soDuTruoc = nguoiBan.getSoDuKhaDung(); // 0
            double giaChot = phienDaKetThuc.getGiaHienTai(); // 150tr

            service.xacNhanThanhToan(giaoDich);

            assertEquals(soDuTruoc + giaChot, nguoiBan.getSoDuKhaDung(), 0.01,
                    "Phải cộng đúng số tiền cho người bán");
        }

        @Test
        @DisplayName("Số dư đủ → trạng thái chuyển sang DA_THANH_TOAN")
        void xacNhan_soDuDu_trangThaiCapNhat() {
            service.xacNhanThanhToan(giaoDich);

            assertEquals(TrangThaiGiaoDich.DA_THANH_TOAN, giaoDich.getTrangThai(),
                    "Trạng thái giao dịch phải là DA_THANH_TOAN sau khi thanh toán");
        }

        @Test
        @DisplayName("Số dư không đủ → thanh toán thất bại, trả về false")
        void xacNhan_soDuKhongDu_falseVe() {
            nguoiMua.setSoDuKhaDung(10_000); // chỉ có 10k, giá chốt 150tr

            boolean result = service.xacNhanThanhToan(giaoDich);

            assertFalse(result, "Số dư không đủ phải trả về false");
        }

        @Test
        @DisplayName("Số dư không đủ → số dư người mua KHÔNG bị thay đổi")
        void xacNhan_soDuKhongDu_khongThayDoiSoDu() {
            nguoiMua.setSoDuKhaDung(10_000);
            double soDuGoc = nguoiMua.getSoDuKhaDung();

            service.xacNhanThanhToan(giaoDich);

            assertEquals(soDuGoc, nguoiMua.getSoDuKhaDung(), 0.01,
                    "Số dư không được thay đổi khi thanh toán thất bại");
        }

        @Test
        @DisplayName("Giao dịch null → trả về false, không crash")
        void xacNhan_null_falseVe() {
            boolean result = service.xacNhanThanhToan(null);

            assertFalse(result, "Giao dịch null phải trả về false");
        }

        @Test
        @DisplayName("Giao dịch đã DA_THANH_TOAN (thanh toán lại) → trả về false")
        void xacNhan_daThanhToanRoi_falseVe() {
            giaoDich.setTrangThai(TrangThaiGiaoDich.DA_THANH_TOAN);

            boolean result = service.xacNhanThanhToan(giaoDich);

            assertFalse(result, "Không được thanh toán 2 lần cho cùng 1 giao dịch");
        }

        @Test
        @DisplayName("Giao dịch DA_HOAN_TIEN → không thanh toán được")
        void xacNhan_daHoanTien_falseVe() {
            giaoDich.setTrangThai(TrangThaiGiaoDich.DA_HOAN_TIEN);

            boolean result = service.xacNhanThanhToan(giaoDich);

            assertFalse(result);
        }

        @Test
        @DisplayName("Thanh toán thành công → luuGiaoDich() được gọi")
        void xacNhan_thanhCong_goiLuu() {
            service.xacNhanThanhToan(giaoDich);

            verify(mockKho, atLeastOnce()).luuGiaoDich(any(GiaoDich.class));
        }
    }

    // ================================================================
    // 3. hoanTien()
    // ================================================================

    @Nested
    @DisplayName("3. hoanTien()")
    class HoanTienTests {

        @Test
        @DisplayName("Hoàn tiền giao dịch CHO_THANH_TOAN → thành công, trả về true")
        void hoanTien_choThanhToan_trueVe() {
            boolean result = service.hoanTien(giaoDich, "Người mua hủy đơn");

            assertTrue(result, "Hoàn tiền giao dịch hợp lệ phải trả về true");
        }

        @Test
        @DisplayName("Hoàn tiền → cộng lại đúng số tiền cho người mua")
        void hoanTien_hopLe_congLaiTienNguoiMua() {
            double soDuTruoc = nguoiMua.getSoDuKhaDung();
            double giaChot = phienDaKetThuc.getGiaHienTai();

            service.hoanTien(giaoDich, "Test hoàn tiền");

            assertEquals(soDuTruoc + giaChot, nguoiMua.getSoDuKhaDung(), 0.01,
                    "Phải cộng lại đúng số tiền cho người mua sau khi hoàn");
        }

        @Test
        @DisplayName("Hoàn tiền giao dịch DA_THANH_TOAN → trừ tiền người bán, cộng cho người mua")
        void hoanTien_daThanhToan_truNguoiBanCongNguoiMua() {
            giaoDich.setTrangThai(TrangThaiGiaoDich.DA_THANH_TOAN);
            nguoiBan.setSoDuKhaDung(150_000_000); // người bán đã nhận tiền
            double soDuNguoiMuaTruoc = nguoiMua.getSoDuKhaDung();
            double soDuNguoiBanTruoc = nguoiBan.getSoDuKhaDung();
            double giaChot = phienDaKetThuc.getGiaHienTai();

            service.hoanTien(giaoDich, "Sản phẩm lỗi");

            assertEquals(soDuNguoiMuaTruoc + giaChot, nguoiMua.getSoDuKhaDung(), 0.01,
                    "Người mua phải được hoàn tiền");
            assertEquals(soDuNguoiBanTruoc - giaChot, nguoiBan.getSoDuKhaDung(), 0.01,
                    "Người bán phải bị trừ tiền khi hoàn");
        }

        @Test
        @DisplayName("Hoàn tiền → trạng thái chuyển sang DA_HOAN_TIEN")
        void hoanTien_hopLe_trangThaiCapNhat() {
            service.hoanTien(giaoDich, "Lý do test");

            assertEquals(TrangThaiGiaoDich.DA_HOAN_TIEN, giaoDich.getTrangThai(),
                    "Trạng thái phải là DA_HOAN_TIEN sau khi hoàn");
        }

        @Test
        @DisplayName("Hoàn tiền giao dịch đã DA_HOAN_TIEN → trả về false, không hoàn 2 lần")
        void hoanTien_daHoanRoi_falseVe() {
            giaoDich.setTrangThai(TrangThaiGiaoDich.DA_HOAN_TIEN);

            boolean result = service.hoanTien(giaoDich, "Thử hoàn lần 2");

            assertFalse(result, "Không được hoàn tiền 2 lần");
        }

        @Test
        @DisplayName("Giao dịch null → trả về false, không crash")
        void hoanTien_null_falseVe() {
            boolean result = service.hoanTien(null, "Lý do");

            assertFalse(result);
        }

        @Test
        @DisplayName("Hoàn tiền thành công → capNhatGiaoDich() được gọi")
        void hoanTien_thanhCong_goiCapNhat() {
            service.hoanTien(giaoDich, "Lý do test");

            verify(mockKho, times(1)).capNhatGiaoDich(giaoDich);
        }
    }

    // ================================================================
    // 4. xemLichSuGiaoDich()
    // ================================================================

    @Nested
    @DisplayName("4. xemLichSuGiaoDich()")
    class XemLichSuTests {

        /** Tạo n giao dịch giả để test phân trang */
        private List<GiaoDich> taoNhieuGiaoDich(int n) {
            List<GiaoDich> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(new GiaoDich("GD" + String.format("%06d", i + 1), phienDaKetThuc));
            }
            return list;
        }

        @Test
        @DisplayName("Người dùng có 5 giao dịch, trang 1 lấy 3 → trả về đúng 3")
        void xemLichSu_phanTrang_trang1() {
            List<GiaoDich> tatCa = taoNhieuGiaoDich(5);
            when(mockKho.layGiaoDichTheoNguoiDung("ND002")).thenReturn(tatCa);

            List<GiaoDich> trang1 = service.xemLichSuGiaoDich("ND002", 1, 3);

            assertEquals(3, trang1.size(), "Trang 1 phải có đúng 3 giao dịch");
        }

        @Test
        @DisplayName("Người dùng có 5 giao dịch, trang 2 lấy 3 → trả về đúng 2 (còn lại)")
        void xemLichSu_phanTrang_trang2() {
            List<GiaoDich> tatCa = taoNhieuGiaoDich(5);
            when(mockKho.layGiaoDichTheoNguoiDung("ND002")).thenReturn(tatCa);

            List<GiaoDich> trang2 = service.xemLichSuGiaoDich("ND002", 2, 3);

            assertEquals(2, trang2.size(), "Trang 2 phải có 2 giao dịch còn lại");
        }

        @Test
        @DisplayName("Trang vượt quá số trang tồn tại → trả về danh sách rỗng")
        void xemLichSu_trangVuotQua_listRong() {
            List<GiaoDich> tatCa = taoNhieuGiaoDich(3);
            when(mockKho.layGiaoDichTheoNguoiDung("ND002")).thenReturn(tatCa);

            List<GiaoDich> result = service.xemLichSuGiaoDich("ND002", 99, 3);

            assertTrue(result.isEmpty(), "Trang vượt quá phải trả về danh sách rỗng");
        }

        @Test
        @DisplayName("Người dùng chưa có giao dịch → trả về danh sách rỗng")
        void xemLichSu_chuaCoGiaoDich_listRong() {
            when(mockKho.layGiaoDichTheoNguoiDung("ND999")).thenReturn(new ArrayList<>());

            List<GiaoDich> result = service.xemLichSuGiaoDich("ND999", 1, 10);

            assertTrue(result.isEmpty(), "Người dùng không có giao dịch phải trả về rỗng");
        }

        @Test
        @DisplayName("layGiaoDichTheoNguoiDung() được gọi đúng 1 lần với đúng mã")
        void xemLichSu_goiKhoMotLan() {
            when(mockKho.layGiaoDichTheoNguoiDung("ND002")).thenReturn(new ArrayList<>());

            service.xemLichSuGiaoDich("ND002", 1, 5);

            verify(mockKho, times(1)).layGiaoDichTheoNguoiDung("ND002");
        }

        @Test
        @DisplayName("Kích thước trang bằng đúng số giao dịch → trả về tất cả trong 1 trang")
        void xemLichSu_kichThuocTrangBangTatCa_traVeHet() {
            List<GiaoDich> tatCa = taoNhieuGiaoDich(4);
            when(mockKho.layGiaoDichTheoNguoiDung("ND002")).thenReturn(tatCa);

            List<GiaoDich> result = service.xemLichSuGiaoDich("ND002", 1, 4);

            assertEquals(4, result.size(), "Phải trả về đủ 4 giao dịch trong 1 trang");
        }
    }
}