package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.Product;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.utils.IUserRepository;
import com.mycompany.utils.IAuctionRepository;
import com.mycompany.utils.UserRepositorySQLite;
import com.mycompany.utils.AuctionRepositorySQLite;
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
  private final IAuctionRepository auctionRepository = new AuctionRepositorySQLite();

  /** Kho lưu trữ người dùng — để tra cứu người bán theo email từ token */
  private final IUserRepository userRepository = new UserRepositorySQLite();

  /** Service xử lý logic đấu giá — tái sử dụng singleton đã có */
  private final AuctionSessionService auctionSessionService = AuctionSessionService.getInstance();

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
      handleFindAll(exchange);
      return;
    }

    // GET /api/auctions/{id}  → chi tiết một phiên
    if (method.equals("GET") && path.startsWith("/api/auctions/") && !path.endsWith("/start")) {
      handleFindOne(exchange, getIDByPath(path, "/api/auctions/"));
      return;
    }

    // POST /api/auctions  → tạo phiên mới
    if (method.equals("POST") && path.equals("/api/auctions")) {
      handleCreateAuction(exchange);
      return;
    }

    // PUT /api/auctions/{id}/start  → bắt đầu phiên
    if (method.equals("PUT") && path.endsWith("/start")) {
      // Trích xuất id từ /api/auctions/{id}/start
      String segment = path.replace("/api/auctions/", "").replace("/start", "");
      handleStartAuction(exchange, segment);
      return;
    }

    guiPhanHoi(exchange, 404, sendBug("Endpoint không tồn tại: " + method + " " + path));
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
  private void handleFindAll(HttpExchange exchange) throws IOException {
    try{
      Map<String, AuctionSession> tatCaPhien = auctionRepository.findAll();

      List<TomTatPhien> danhSach = new ArrayList<>();
      for (AuctionSession phien : tatCaPhien.values()) {
        danhSach.add(new TomTatPhien(phien));
      }

      guiPhanHoi(exchange, 200, gson.toJson(danhSach));
    }
    catch (Exception e){
      guiPhanHoi(exchange, 500, sendBug("Lỗi server: " + e.getMessage()));
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
   * Response 404: { "sendBug": "Không tìm thấy phiên: PH999999" }
   */
  private void handleFindOne(HttpExchange exchange, String maPhien) throws IOException {
    if (maPhien == null || maPhien.isBlank()) {
      guiPhanHoi(exchange, 400, sendBug("Thiếu mã phiên trong URL"));
      return;
    }

    AuctionSession phien = auctionRepository.findById(maPhien);

    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("Không tìm thấy phiên: " + maPhien));
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
   * Response 400: { "sendBug": "Thiếu thông tin bắt buộc" }
   * Response 401: { "sendBug": "Cần đăng nhập trước" }
   */
  private void handleCreateAuction(HttpExchange exchange) throws IOException {
    User nguoiBan = checkToken(exchange);
    if (nguoiBan == null) return;

    String body = readBody(exchange);
    TaoPhienRequest req = gson.fromJson(body, TaoPhienRequest.class);

    if (req == null || req.tenPhien == null || req.tenSanPham == null
            || req.maSanPham == null || req.giaKhoiDiem <= 0 || req.thoiGianGiay <= 0) {
      guiPhanHoi(exchange, 400, sendBug("Thiếu hoặc sai thông tin bắt buộc"));
      return;
    }

    Product sanPham = new Product(req.tenSanPham, req.maSanPham);
    AuctionSession phienMoi = new AuctionSession(
            null, req.tenPhien, sanPham, req.giaKhoiDiem, nguoiBan, req.thoiGianGiay);

    auctionRepository.save(phienMoi);

    // FIX: Đồng bộ vào in-memory store để WebSocket tìm thấy
    AuctionSessionRegistry.getInstance().add(phienMoi);

    String maPhienDaSinh = phienMoi.getAuctionSessionId();
    guiPhanHoi(exchange, 201,
            gson.toJson(new TaoPhienResponse(maPhienDaSinh, "Tạo phiên thành công")));
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
   * Response 400: { "sendBug": "Phiên không ở trạng thái DANG_CHO" }
   * Response 403: { "sendBug": "Bạn không phải người tạo phiên này" }
   * Response 404: { "sendBug": "Không tìm thấy phiên: PH999999" }
   */
  private void handleStartAuction(HttpExchange exchange, String maPhien) throws IOException {
    User nguoiYeuCau = checkToken(exchange);
    if (nguoiYeuCau == null) return;

    AuctionSession phien = auctionRepository.findById(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("Không tìm thấy phiên: " + maPhien));
      return;
    }

    if (!phien.getSeller().getUserId().equals(nguoiYeuCau.getUserId())) {
      guiPhanHoi(exchange, 403, sendBug("Bạn không phải người tạo phiên này"));
      return;
    }

    if (phien.getStatus() != SessionStatus.WAITING) {
      guiPhanHoi(exchange, 400, sendBug("Phiên không ở trạng thái DANG_CHO"));
      return;
    }

    auctionSessionService.start(phien);
    auctionRepository.update(phien);

    // FIX: Đảm bảo phiên đang hoạt động được đăng ký để WebSocket tìm thấy
    AuctionSessionRegistry.getInstance().add(phien);

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
  private User checkToken(HttpExchange exchange) throws IOException {
    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      guiPhanHoi(exchange, 401, sendBug("Cần đăng nhập trước (Authorization: Bearer <token>)"));
      return null;
    }

    String token = authHeader.substring("Bearer ".length()).trim();

    // Token format: USER_<email>_<timestamp>
    // Trích xuất email: bỏ "USER_" ở đầu và "_<timestamp>" ở cuối
    if (!token.startsWith("USER_")) {
      guiPhanHoi(exchange, 401, sendBug("Token không hợp lệ"));
      return null;
    }

    String phần = token.substring("USER_".length()); // "abc@gmail.com_1714123456"
    int viTriDauGachDuoiCuoi = phần.lastIndexOf('_');

    if (viTriDauGachDuoiCuoi < 0) {
      guiPhanHoi(exchange, 401, sendBug("Token không hợp lệ"));
      return null;
    }

    String email = phần.substring(0, viTriDauGachDuoiCuoi);

    // Tra cứu người dùng trong DB
    User nguoiDung = userRepository.findByEmail(email);

    if (nguoiDung == null) {
      guiPhanHoi(exchange, 401, sendBug("Token không hợp lệ hoặc tài khoản không tồn tại"));
      return null;
    }

    return nguoiDung;
  }

  /** Trích xuất đoạn sau prefix từ URL path */
  private String getIDByPath(String path, String prefix) {
    if (!path.startsWith(prefix) || path.length() <= prefix.length()) return null;
    return path.substring(prefix.length());
  }

  /** Đọc toàn bộ body từ HTTP request */
  private String readBody(HttpExchange exchange) throws IOException {
    InputStream is = exchange.getRequestBody();
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
  }

  /** Tạo JSON lỗi đơn giản */
  private String sendBug(String thongBao) {
    return gson.toJson(new ThongBaosendBug(thongBao));
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
  public static class TaoPhienRequest {
    String tenPhien;
    String tenSanPham;
    String maSanPham;
    String danhMuc;
    String moTa;
    double giaKhoiDiem;
    int    thoiGianGiay;

    public TaoPhienRequest(String tenPhien, String tenSanPham, String maSanPham, String danhMuc, String moTa, double giaKhoiDiem, int thoiGianGiay) {
      this.tenPhien = tenPhien;
      this.tenSanPham = tenSanPham;
      this.maSanPham = maSanPham;
      this.giaKhoiDiem = giaKhoiDiem;
      this.thoiGianGiay = thoiGianGiay;
    }
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

    TomTatPhien(AuctionSession p) {
      this.maPhien         = p.getSessionId();
      this.tenPhien        = p.getSessionName();
      this.giaHienTai      = p.getCurrentPrice();
      this.trangThai       = p.getStatus().name();
      this.tenNguoiBan     = p.getSeller() != null ? p.getSeller().getFullName() : null;
      this.tenSanPham      = p.getProduct()  != null ? p.getProduct().getProductName() : null;
      this.thoiGianBatDau  = p.getStartTime()  != null ? p.getStartTime().toString()  : null;
      this.thoiGianKetThuc = p.getEndTime() != null ? p.getEndTime().toString() : null;
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

    ChiTietPhien(AuctionSession p) {
      this.maPhien           = p.getSessionId();
      this.tenPhien          = p.getSessionName();
      this.giaHienTai        = p.getCurrentPrice();
      this.buocGia           = p.getPriceStep();
      this.trangThai         = p.getStatus().name();
      this.tenNguoiBan       = p.getSeller()       != null ? p.getSeller().getFullName()          : null;
      this.maNguoiBan        = p.getSeller()       != null ? p.getSeller().getUserId()     : null;
      this.tenSanPham        = p.getProduct()        != null ? p.getProduct().getProductName()       : null;
      this.maSanPham         = p.getProduct()        != null ? p.getProduct().getProductCode()        : null;
      this.tenNguoiThangCuoc = p.getWinner() != null ? p.getWinner().getFullName()    : null;
      this.thoiGianBatDau    = p.getStartTime()  != null ? p.getStartTime().toString()   : null;
      this.thoiGianKetThuc   = p.getEndTime() != null ? p.getEndTime().toString()  : null;
      this.soNguoiTraGia     = p.getBidderList() != null ? p.getBidderList().size() : 0;
    }
  }

  /** Wrapper thông báo thành công */
  private static class ThongBao {
    String thongBao;
    ThongBao(String thongBao) { this.thongBao = thongBao; }
  }

  /** Wrapper thông báo lỗi */
  private static class ThongBaosendBug {
    String sendBug;
    ThongBaosendBug(String sendBug) { this.sendBug = sendBug; }
  }
}