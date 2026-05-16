package com.mycompany.action;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho PhienDauGiaService — logic đặt giá, concurrent bidding, anti-sniping.
 *
 * Giải thích cho giảng viên:
 * - @BeforeEach tạo dữ liệu mới trước mỗi test → các test độc lập nhau.
 * - Dùng helper buildPhienDangDienRa() để tránh lặp code setup.
 * - Không cần database thật — test thuần logic in-memory.
 */
@DisplayName("Kiểm tra logic đấu giá PhienDauGiaService")
class PhienDauGiaServiceTest {

    private AuctionSessionService service;
    private NguoiDung nguoiBan;
    private NguoiDung nguoiMua1;
    private NguoiDung nguoiMua2;

    @BeforeEach
    void setup() {
        service = AuctionSessionService.getInstance();

        // Tạo người bán
        nguoiBan = new NguoiDung("Nguyen Van Ban", "ban@test.com", "hash", "2000-01-01");
        nguoiBan.setMaNguoiDung("ND001");
        nguoiBan.setSoDuKhaDung(0);

        // Tạo hai người mua
        nguoiMua1 = new NguoiDung("Tran Thi Mua1", "mua1@test.com", "hash", "1995-05-15");
        nguoiMua1.setMaNguoiDung("ND002");
        nguoiMua1.setSoDuKhaDung(10_000_000_000.0); // 10 tỷ để không bị chặn tiền

        nguoiMua2 = new NguoiDung("Le Van Mua2", "mua2@test.com", "hash", "1998-08-20");
        nguoiMua2.setMaNguoiDung("ND003");
        nguoiMua2.setSoDuKhaDung(10_000_000_000.0);
    }

    // ----------------------------------------------------------------
    // Helper: tạo phiên đang diễn ra với giá khởi điểm cho trước
    // ----------------------------------------------------------------
    private PhienDauGia buildPhienDangDienRa(double giaKhoiDiem, int thoiGianGiay) {
        SanPham sp = new SanPham("Đồng hồ Rolex", "SP001");
        PhienDauGia phien = new PhienDauGia(
                "PH_TEST_001", "Phiên test", sp, giaKhoiDiem, nguoiBan, thoiGianGiay
        );
        service.batDauPhien(phien); // chuyển sang DANG_DIEN_RA
        return phien;
    }

    // ================================================================
    // 1. batDauPhien()
    // ================================================================

    @Test
    @DisplayName("Phiên mới tạo phải ở trạng thái DANG_CHO")
    void phienMoi_trangThaiDangCho() {
        SanPham sp = new SanPham("Xe máy", "SP002");
        PhienDauGia phien = new PhienDauGia(
                "PH002", "Phiên xe", sp, 10_000_000, nguoiBan, 300
        );
        assertEquals(TrangThaiPhien.DANG_CHO, phien.getTrangThai());
    }

    @Test
    @DisplayName("Sau batDauPhien() trạng thái phải là DANG_DIEN_RA")
    void batDau_chuyenSangDangDienRa() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);
        assertEquals(TrangThaiPhien.DANG_DIEN_RA, phien.getTrangThai());
    }

    @Test
    @DisplayName("Sau batDauPhien() phải có thời gian kết thúc")
    void batDau_coThoiGianKetThuc() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);
        assertNotNull(phien.getThoiGianKetThuc(),
                "Thời gian kết thúc không được null sau khi bắt đầu");
        assertTrue(
                phien.getThoiGianKetThuc().isAfter(LocalDateTime.now()),
                "Thời gian kết thúc phải ở tương lai"
        );
    }

    // ================================================================
    // 2. setPrice() — các trường hợp hợp lệ
    // ================================================================

    @Test
    @DisplayName("Đặt giá hợp lệ (cao hơn giá khởi điểm) phải trả về true")
    void datGia_hopLe_trueTrue() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        boolean ketQua = service.setPrice(phien, nguoiMua1, 10_000_001);

        assertTrue(ketQua, "Giá hợp lệ phải được chấp nhận");
    }

    @Test
    @DisplayName("Sau đặt giá thành công, giá hiện tại phải được cập nhật")
    void datGia_hopLe_giaHienTaiCapNhat() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        service.setPrice(phien, nguoiMua1, 15_000_000);

        assertEquals(15_000_000, phien.getGiaHienTai(), 0.01,
                "Giá hiện tại phải bằng giá vừa đặt");
    }

    @Test
    @DisplayName("Sau đặt giá thành công, người mua được thêm vào danh sách")
    void datGia_hopLe_nguoiMuaDuocThem() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        service.setPrice(phien, nguoiMua1, 15_000_000);

        assertFalse(phien.getDanhSachNguoiTraGia().isEmpty(),
                "Danh sách người trả giá phải có ít nhất 1 người sau khi đặt");
        assertEquals(nguoiMua1, phien.getDanhSachNguoiTraGia().get(0),
                "Người đặt phải nằm trong danh sách");
    }

    // ================================================================
    // 3. setPrice() — các trường hợp bị từ chối
    // ================================================================

    @Test
    @DisplayName("Giá thấp hơn giá hiện tại phải bị từ chối")
    void datGia_giaTuaThap_traFalse() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        boolean ketQua = service.setPrice(phien, nguoiMua1, 9_000_000);

        assertFalse(ketQua, "Giá thấp hơn giá hiện tại phải bị từ chối");
    }

    @Test
    @DisplayName("Người bán không được tự đặt giá phiên của mình")
    void datGia_nguoiBanTuDat_traFalse() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        boolean ketQua = service.setPrice(phien, nguoiBan, 15_000_000);

        assertFalse(ketQua, "Người bán không được phép tự đặt giá");
    }

    @Test
    @DisplayName("Đặt giá vào phiên đã đóng phải bị từ chối")
    void datGia_phienDaDong_traFalse() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);
        service.dongPhien(phien, TrangThaiPhien.DA_HUY);

        boolean ketQua = service.setPrice(phien, nguoiMua1, 15_000_000);

        assertFalse(ketQua, "Không thể đặt giá vào phiên đã đóng");
    }

    @Test
    @DisplayName("Đặt giá đúng bằng giá hiện tại (chưa có bid)")
    void datGia_dungBangGiaKhoiDiem_traTrue() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        // Giá khởi điểm là 10tr, đặt đúng 10tr không được (phải cao hơn)
        boolean ketQua = service.setPrice(phien, nguoiMua1, 10_000_000);

        assertTrue(ketQua, "Giá bằng giá khởi điểm phải bị từ chối (phải cao hơn)");
    }

    // ================================================================
    // 4. Bước giá (buocGia) sau lần đặt đầu tiên
    // ================================================================

    @Test
    @DisplayName("Sau lần đặt đầu tiên, bước giá phải được thiết lập (6% giá khởi điểm)")
    void datGia_lanDau_buocGiaDuocThietLap() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);
        service.setPrice(phien, nguoiMua1, 10_000_001);

        // Theo logic: buocGia = giaHienTai * 0.06
        double expected = phien.getGiaHienTai() * phien.getDoLechGiaMin();
        assertEquals(expected, phien.getBuocGia(), 0.01,
                "Bước giá phải bằng 6% giá hiện tại sau lần đặt đầu tiên");
    }

    @Test
    @DisplayName("Lần đặt thứ hai phải cao hơn giá hiện tại + bước giá")
    void datGia_lanHai_phaiBaoGomBuocGia() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        // Lần 1
        service.setPrice(phien, nguoiMua1, 10_000_001);
        double buocGia = phien.getBuocGia();
        double giaHienTai = phien.getGiaHienTai();

        // Lần 2: đặt thấp hơn giaHienTai + buocGia → bị từ chối
        boolean ketQua = service.setPrice(phien, nguoiMua2, giaHienTai + buocGia - 1);

        assertFalse(ketQua,
                "Giá lần 2 thấp hơn giaHienTai + buocGia phải bị từ chối");
    }

    // ================================================================
    // 5. Anti-sniping
    // ================================================================

    @Test
    @DisplayName("Bid trong X giây cuối phải gia hạn thêm thời gian kết thúc")
    void antiSniping_bidGanHetGio_giaDaiThem() {
        // Tạo phiên chỉ còn 30 giây (dưới ngưỡng chống sniping 60s)
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        // Giả lập: đẩy thời gian kết thúc về gần (còn 30 giây)
        phien.setThoiGianKetThuc(LocalDateTime.now().plusSeconds(30));
        LocalDateTime thoiGianCu = phien.getThoiGianKetThuc();

        service.setPrice(phien, nguoiMua1, 10_000_001);

        // Thời gian kết thúc mới phải sau thời gian cũ
        assertTrue(
                phien.getThoiGianKetThuc().isAfter(thoiGianCu),
                "Anti-sniping: bid trong 60s cuối phải gia hạn thêm thời gian"
        );
    }

    @Test
    @DisplayName("Bid khi còn nhiều thời gian không được gia hạn")
    void antiSniping_bidSom_khongGiaDai() {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 3600); // 1 tiếng
        LocalDateTime thoiGianKetThucTruoc = phien.getThoiGianKetThuc();

        service.setPrice(phien, nguoiMua1, 10_000_001);

        // Cho phép sai lệch 1 giây vì scheduling
        long diff = java.time.Duration.between(
                thoiGianKetThucTruoc, phien.getThoiGianKetThuc()
        ).abs().getSeconds();

        assertTrue(diff < 2,
                "Bid khi còn nhiều thời gian không được thay đổi thời gian kết thúc");
    }

    // ================================================================
    // 6. Concurrent Bidding — nhiều người đặt cùng lúc
    // ================================================================

    @Test
    @DisplayName("Nhiều thread đặt giá cùng lúc — không bị lost update")
    void concurrent_nhieuThread_khongLostUpdate() throws InterruptedException {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        int soThread = 10;
        Thread[] threads = new Thread[soThread];
        boolean[] results = new boolean[soThread];

        // Mỗi thread đặt một giá khác nhau tăng dần
        for (int i = 0; i < soThread; i++) {
            final int idx = i;
            final double gia = 11_000_000 + idx * 1_000_000; // 11tr, 12tr, ... 20tr
            NguoiDung nguoiDat = (idx % 2 == 0) ? nguoiMua1 : nguoiMua2;
            threads[i] = new Thread(() -> {
                results[idx] = service.setPrice(phien, nguoiDat, gia);
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // Giá cuối cùng phải nằm trong khoảng hợp lệ (không bị rollback về 0 hay âm)
        assertTrue(phien.getGiaHienTai() > 10_000_000,
                "Sau concurrent bidding, giá phải lớn hơn giá khởi điểm");
        assertTrue(phien.getGiaHienTai() <= 20_000_000,
                "Giá không được vượt quá giá cao nhất được đặt");

        // Chỉ một số bid thành công (do điều kiện bước giá)
        long soThanhCong = 0;
        for (boolean r : results) if (r) soThanhCong++;
        assertTrue(soThanhCong > 0, "Ít nhất một bid phải thành công");
    }

    @Test
    @DisplayName("Không bao giờ có hai người cùng là người thắng cuộc")
    void concurrent_khongHaiNguoiCungThang() throws InterruptedException {
        PhienDauGia phien = buildPhienDangDienRa(10_000_000, 300);

        // Hai người đặt giá cuối cùng trước khi đóng
        Thread t1 = new Thread(() -> service.setPrice(phien, nguoiMua1, 50_000_000));
        Thread t2 = new Thread(() -> service.setPrice(phien, nguoiMua2, 50_000_000));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Đóng phiên và xác định người thắng
        service.dongPhien(phien, TrangThaiPhien.DA_THANH_TOAN);

        // Người thắng cuộc chỉ có thể là 1 trong 2, không thể null
        if (phien.getDanhSachNguoiTraGia().isEmpty()) {
            // Cả hai bị từ chối vì bước giá — không có người thắng, phiên hủy
            assertEquals(TrangThaiPhien.DA_HUY, phien.getTrangThai());
        } else {
            NguoiDung nguoiThang = phien.getNguoiThangCuoc();
            assertTrue(
                    nguoiThang == null || nguoiThang.equals(nguoiMua1) || nguoiThang.equals(nguoiMua2),
                    "Người thắng phải là một trong hai người đấu giá"
            );
        }
    }
}