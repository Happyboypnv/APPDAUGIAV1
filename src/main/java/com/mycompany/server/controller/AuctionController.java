package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.action.PhienDauGiaService;
import com.mycompany.models.NguoiDung;
import com.mycompany.models.PhienDauGia;
import com.mycompany.models.SanPham;
import com.mycompany.models.TrangThaiPhien;
import com.mycompany.utils.IKhoLuuTruNguoiDung;
import com.mycompany.utils.IKhoLuuTruPhienDauGia;
import com.mycompany.utils.KhoLuuTruNguoiDungSQLite;
import com.mycompany.utils.KhoLuuTruPhienDauGiaSQLite;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AuctionController — Xử lý các HTTP request liên quan đến phiên đấu giá.
 *
 * Bốn API endpoint:
 *  GET  /api/auctions          → Danh sách tất cả phiên đấu giá
 *  GET  /api/auctions/{id}     → Chi tiết một phiên theo mã
 *  POST /api/auctions          → Tạo phiên mới (chỉ Seller)
 *  PUT  /api/auctions/{id}/start → Bắt đầu phiên (chuyển DANG_CHO → DANG_DIEN_RA)
 *
 * Tái sử dụng:
 *  - PhienDauGiaService   (singleton, có sẵn) → batDauPhien()
 *  - KhoLuuTruPhienDauGiaSQLite (có sẵn)      → CRUD phiên
 *  - KhoLuuTruNguoiDungSQLite (có sẵn)        → tra cứu người bán
 *
 * Authentication:
 *  - POST và PUT yêu cầu header: Authorization: Bearer <token>
 *  - GET không yêu cầu token (public)
 */
public class AuctionController {

  // ===== PHỤ THUỘC =====

  private final Gson gson = new GsonBuilder()
      .setPrettyPrinting()
      .serializeNulls()
      .create();

  /** Kho lưu trữ phiên đấu giá — tái sử dụng KhoLuuTruPhienDauGiaSQLite đã có */
  private final IKhoLuuTruPhienDauGia khoPhien = new KhoLuuTruPhienDauGiaSQLite();

  /** Kho lưu trữ người dùng — để tra cứu người bán theo email từ token */
  private final IKhoLuuTruNguoiDung khoNguoiDung = new KhoLuuTruNguoiDungSQLite();

  /** Service xử lý logic đấu giá — tái sử dụng singleton đã có */
  private final PhienDauGiaService phienDauGiaService = PhienDauGiaService.getInstance();

  // =========================================================
  // ROUTING — phân phối request đến handler phù hợp
  // =========================================================

  /**
   * Điểm vào duy nhất cho mọi request đến /api/auctions
   *
   * Routing theo method + path:
   *   GET  /api/auctions          → handleLayTatCa()
   *   GET  /api/auctions/{id}     → handleLayMotPhien()
   *   POST /api/auctions          → handleTaoPhien()
   *   PUT  /api/auctions/{id}/start → handleBatDauPhien()
   */
  public void route(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod().toUpperCase();
    String path   = exchange.getRequestURI().getPath(); // vd: /api/auctions/PH000001/start

    // Chuẩn hóa: loại bỏ trailing slash
    if (path.endsWith("/") && path.length() > 1) {
      path = path.substring(0, path.length() - 1);
    }

    // GET /api/auctions  → danh sách tất cả
    if (method.equals("GET") && path.equals("/api/auctions")) {
      handleLayTatCa(exchange);
      return;
    }

    // GET /api/auctions/{id}  → chi tiết một phiên
    if (method.equals("GET") && path.startsWith("/api/auctions/") && !path.endsWith("/start")) {
      handleLayMotPhien(exchange, layIdTuPath(path, "/api/auctions/"));
      return;
    }

    // POST /api/auctions  → tạo phiên mới
    if (method.equals("POST") && path.equals("/api/auctions")) {
      handleTaoPhien(exchange);
      return;
    }

    // PUT /api/auctions/{id}/start  → bắt đầu phiên
    if (method.equals("PUT") && path.endsWith("/start")) {
      // Trích xuất id từ /api/auctions/{id}/start
      String segment = path.replace("/api/auctions/", "").replace("/start", "");
      handleBatDauPhien(exchange, segment);
      return;
    }

    guiPhanHoi(exchange, 404, loi("Endpoint không tồn tại: " + method + " " + path));
  }

  // =========================================================
  // API 1: GET /api/auctions  →  danh sách tất cả phiên
  // =========================================================

  /**
   * Trả về danh sách tất cả phiên đấu giá (không cần authentication).
   *
   * Response 200:
   * [
   *   {
   *     "maPhien": "PH000001",
   *     "tenPhien": "Đấu giá xe đạp",
   *     "giaHienTai": 500000,
   *     "trangThai": "DANG_CHO",
   *     "tenNguoiBan": "Nguyen Van A",
   *     "tenSanPham": "Xe đạp Giant"
   *   }, ...
   * ]
   */
  private void handleLayTatCa(HttpExchange exchange) throws IOException {
    try{
      Map<String, PhienDauGia> tatCaPhien = khoPhien.layTatCaPhienDauGia();

      List<TomTatPhien> danhSach = new ArrayList<>();
      for (PhienDauGia phien : tatCaPhien.values()) {
        danhSach.add(new TomTatPhien(phien));
      }

      guiPhanHoi(exchange, 200, gson.toJson(danhSach));
    }
    catch (Exception e){
      guiPhanHoi(exchange, 500, loi("Lỗi server: " + e.getMessage()));
    }
  }

  // =========================================================
  // API 2: GET /api/auctions/{id}  →  chi tiết một phiên
  // =========================================================

  /**
   * Trả về chi tiết đầy đủ của một phiên đấu giá theo mã phiên.
   *
   * Response 200: { maPhien, tenPhien, giaHienTai, buocGia, trangThai,
   *                 thoiGianBatDau, thoiGianKetThuc, tenNguoiBan,
   *                 tenSanPham, tenNguoiThangCuoc }
   * Response 404: { "loi": "Không tìm thấy phiên: PH999999" }
   */
  private void handleLayMotPhien(HttpExchange exchange, String maPhien) throws IOException {
    if (maPhien == null || maPhien.isBlank()) {
      guiPhanHoi(exchange, 400, loi("Thiếu mã phiên trong URL"));
      return;
    }

    PhienDauGia phien = khoPhien.layPhienDauGia(maPhien);

    if (phien == null) {
      guiPhanHoi(exchange, 404, loi("Không tìm thấy phiên: " + maPhien));
      return;
    }

    guiPhanHoi(exchange, 200, gson.toJson(new ChiTietPhien(phien)));
  }

  // =========================================================
  // API 3: POST /api/auctions  →  tạo phiên mới (Seller)
  // =========================================================

  /**
   * Tạo một phiên đấu giá mới.
   *
   * Yêu cầu: Authorization: Bearer <token>
   *
   * Request body:
   * {
   *   "tenPhien": "Đấu giá xe đạp",
   *   "tenSanPham": "Xe đạp Giant",
   *   "maSanPham": "SP001",
   *   "giaKhoiDiem": 500000,
   *   "thoiGianGiay": 300
   * }
   *
   * Response 201: { "maPhien": "PH000001", "thongBao": "Tạo phiên thành công" }
   * Response 400: { "loi": "Thiếu thông tin bắt buộc" }
   * Response 401: { "loi": "Cần đăng nhập trước" }
   */
  private void handleTaoPhien(HttpExchange exchange) throws IOException {
    // Xác thực token
    NguoiDung nguoiBan = xacThucToken(exchange);
    if (nguoiBan == null) return; // xacThucToken đã gửi response lỗi

    // Đọc và parse body
    String body = docBody(exchange);
    TaoPhienRequest req = gson.fromJson(body, TaoPhienRequest.class);

    // Validate dữ liệu đầu vào
    if (req == null || req.tenPhien == null || req.tenSanPham == null
        || req.maSanPham == null || req.giaKhoiDiem <= 0 || req.thoiGianGiay <= 0) {
      guiPhanHoi(exchange, 400, loi(
          "Thiếu hoặc sai thông tin bắt buộc: tenPhien, tenSanPham, maSanPham, giaKhoiDiem (>0), thoiGianGiay (>0)"));
      return;
    }

    // Tạo SanPham và PhienDauGia — tái sử dụng model đã có
    SanPham sanPham = new SanPham(req.tenSanPham, req.maSanPham);
    PhienDauGia phienMoi = new PhienDauGia(
        null,           // maPhien = null → KhoLuuTru sẽ tự sinh
        req.tenPhien,
        sanPham,
        req.giaKhoiDiem,
        nguoiBan,
        req.thoiGianGiay
    );

    // Lưu vào SQLite — KhoLuuTruPhienDauGiaSQLite tự sinh mã phiên
    khoPhien.luuPhienDauGia(phienMoi);

    // Lấy lại mã phiên vừa được sinh (đã setMaPhien bên trong luuPhienDauGia)
    String maPhienDaSinh = phienMoi.getMaPhienDauGia();

    guiPhanHoi(exchange, 201, gson.toJson(new TaoPhienResponse(maPhienDaSinh, "Tạo phiên thành công")));
  }

  // =========================================================
  // API 4: PUT /api/auctions/{id}/start  →  bắt đầu phiên
  // =========================================================

  /**
   * Bắt đầu một phiên đấu giá (chuyển từ DANG_CHO sang DANG_DIEN_RA).
   *
   * Yêu cầu: Authorization: Bearer <token>
   *          Người gọi phải là người tạo phiên (nguoiBan)
   *
   * Response 200: { "thongBao": "Phiên PH000001 đã bắt đầu" }
   * Response 400: { "loi": "Phiên không ở trạng thái DANG_CHO" }
   * Response 403: { "loi": "Bạn không phải người tạo phiên này" }
   * Response 404: { "loi": "Không tìm thấy phiên: PH999999" }
   */
  private void handleBatDauPhien(HttpExchange exchange, String maPhien) throws IOException {
    // Xác thực token
    NguoiDung nguoiYeuCau = xacThucToken(exchange);
    if (nguoiYeuCau == null) return;

    if (maPhien == null || maPhien.isBlank()) {
      guiPhanHoi(exchange, 400, loi("Thiếu mã phiên trong URL"));
      return;
    }

    // Lấy phiên từ DB
    PhienDauGia phien = khoPhien.layPhienDauGia(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, loi("Không tìm thấy phiên: " + maPhien));
      return;
    }

    // Kiểm tra quyền: chỉ người tạo phiên mới được bắt đầu
    if (!phien.getNguoiBan().layMaNguoiDung().equals(nguoiYeuCau.layMaNguoiDung())) {
      guiPhanHoi(exchange, 403, loi("Bạn không phải người tạo phiên này"));
      return;
    }

    // Kiểm tra trạng thái
    if (phien.getTrangThai() != TrangThaiPhien.DANG_CHO) {
      guiPhanHoi(exchange, 400, loi(
          "Phiên không ở trạng thái DANG_CHO. Trạng thái hiện tại: " + phien.getTrangThai().name()));
      return;
    }

    // Tái sử dụng PhienDauGiaService.batDauPhien() — đã có sẵn
    phienDauGiaService.batDauPhien(phien);

    // Cập nhật lại vào SQLite (batDauPhien chỉ cập nhật object trong memory)
    khoPhien.capNhatPhienDauGia(phien);

    guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("Phiên " + maPhien + " đã bắt đầu")));
  }

  // =========================================================
  // PHƯƠNG THỨC HỖ TRỢ
  // =========================================================

  /**
   * Xác thực Bearer token từ Authorization header.
   * Token có dạng: "USER_<email>_<timestamp>"
   *
   * @return NguoiDung nếu token hợp lệ, null nếu không hợp lệ (đã gửi response lỗi)
   */
  private NguoiDung xacThucToken(HttpExchange exchange) throws IOException {
    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      guiPhanHoi(exchange, 401, loi("Cần đăng nhập trước (Authorization: Bearer <token>)"));
      return null;
    }

    String token = authHeader.substring("Bearer ".length()).trim();

    // Token format: USER_<email>_<timestamp>
    // Trích xuất email: bỏ "USER_" ở đầu và "_<timestamp>" ở cuối
    if (!token.startsWith("USER_")) {
      guiPhanHoi(exchange, 401, loi("Token không hợp lệ"));
      return null;
    }

    String phần = token.substring("USER_".length()); // "abc@gmail.com_1714123456"
    int viTriDauGachDuoiCuoi = phần.lastIndexOf('_');

    if (viTriDauGachDuoiCuoi < 0) {
      guiPhanHoi(exchange, 401, loi("Token không hợp lệ"));
      return null;
    }

    String email = phần.substring(0, viTriDauGachDuoiCuoi);

    // Tra cứu người dùng trong DB
    Map<String, NguoiDung> danhSach = khoNguoiDung.layTatCa();
    NguoiDung nguoiDung = danhSach.get(email);

    if (nguoiDung == null) {
      guiPhanHoi(exchange, 401, loi("Token không hợp lệ hoặc tài khoản không tồn tại"));
      return null;
    }

    return nguoiDung;
  }

  /** Trích xuất đoạn sau prefix từ URL path */
  private String layIdTuPath(String path, String prefix) {
    if (!path.startsWith(prefix) || path.length() <= prefix.length()) return null;
    return path.substring(prefix.length());
  }

  /** Đọc toàn bộ body từ HTTP request */
  private String docBody(HttpExchange exchange) throws IOException {
    InputStream is = exchange.getRequestBody();
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
  }

  /** Tạo JSON lỗi đơn giản */
  private String loi(String thongBao) {
    return gson.toJson(new ThongBaoLoi(thongBao));
  }

  /** Ghi HTTP response với Content-Type: application/json và CORS headers */
  private void guiPhanHoi(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  // =========================================================
  // INNER CLASSES: DTO (Data Transfer Objects)
  // =========================================================

  /** Request body cho POST /api/auctions */
  private static class TaoPhienRequest {
    String tenPhien;
    String tenSanPham;
    String maSanPham;
    double giaKhoiDiem;
    int    thoiGianGiay;
  }

  /** Response cho POST /api/auctions thành công */
  private static class TaoPhienResponse {
    String maPhien;
    String thongBao;
    TaoPhienResponse(String maPhien, String thongBao) {
      this.maPhien  = maPhien;
      this.thongBao = thongBao;
    }
  }

  /** Tóm tắt phiên đấu giá dùng trong GET /api/auctions */
  private static class TomTatPhien {
    String maPhien;
    String tenPhien;
    double giaHienTai;
    String trangThai;
    String tenNguoiBan;
    String tenSanPham;
    String thoiGianBatDau;
    String thoiGianKetThuc;

    TomTatPhien(PhienDauGia p) {
      this.maPhien         = p.getMaPhienDauGia();
      this.tenPhien        = p.getTenPhienDauGia();
      this.giaHienTai      = p.getGiaHienTai();
      this.trangThai       = p.getTrangThai().name();
      this.tenNguoiBan     = p.getNguoiBan() != null ? p.getNguoiBan().layHoTen() : null;
      this.tenSanPham      = p.getSanPham()  != null ? p.getSanPham().layTenSanPham() : null;
      this.thoiGianBatDau  = p.getThoiGianBatDau()  != null ? p.getThoiGianBatDau().toString()  : null;
      this.thoiGianKetThuc = p.getThoiGianKetThuc() != null ? p.getThoiGianKetThuc().toString() : null;
    }
  }

  /** Chi tiết đầy đủ phiên đấu giá dùng trong GET /api/auctions/{id} */
  private static class ChiTietPhien {
    String maPhien;
    String tenPhien;
    double giaHienTai;
    double buocGia;
    String trangThai;
    String tenNguoiBan;
    String maNguoiBan;
    String tenSanPham;
    String maSanPham;
    String tenNguoiThangCuoc;
    String thoiGianBatDau;
    String thoiGianKetThuc;
    int    soNguoiTraGia;

    ChiTietPhien(PhienDauGia p) {
      this.maPhien           = p.getMaPhienDauGia();
      this.tenPhien          = p.getTenPhienDauGia();
      this.giaHienTai        = p.getGiaHienTai();
      this.buocGia           = p.getBuocGia();
      this.trangThai         = p.getTrangThai().name();
      this.tenNguoiBan       = p.getNguoiBan()       != null ? p.getNguoiBan().layHoTen()          : null;
      this.maNguoiBan        = p.getNguoiBan()       != null ? p.getNguoiBan().layMaNguoiDung()     : null;
      this.tenSanPham        = p.getSanPham()        != null ? p.getSanPham().layTenSanPham()       : null;
      this.maSanPham         = p.getSanPham()        != null ? p.getSanPham().layMaSanPham()        : null;
      this.tenNguoiThangCuoc = p.getNguoiThangCuoc() != null ? p.getNguoiThangCuoc().layHoTen()    : null;
      this.thoiGianBatDau    = p.getThoiGianBatDau()  != null ? p.getThoiGianBatDau().toString()   : null;
      this.thoiGianKetThuc   = p.getThoiGianKetThuc() != null ? p.getThoiGianKetThuc().toString()  : null;
      this.soNguoiTraGia     = p.getDanhSachNguoiTraGia() != null ? p.getDanhSachNguoiTraGia().size() : 0;
    }
  }

  /** Wrapper thông báo thành công */
  private static class ThongBao {
    String thongBao;
    ThongBao(String thongBao) { this.thongBao = thongBao; }
  }

  /** Wrapper thông báo lỗi */
  private static class ThongBaoLoi {
    String loi;
    ThongBaoLoi(String loi) { this.loi = loi; }
  }
}