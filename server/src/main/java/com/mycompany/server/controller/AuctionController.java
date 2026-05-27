package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.action.AuctionScheduler;
import com.mycompany.action.AuctionSessionService;
import com.mycompany.action.AuctionSessionRegistry;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.ItemFactory;
import com.mycompany.models.Items;
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
  private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionController.class);
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

  /** Scheduler để lên lịch tự động mở phiên khi đến startTime*/
  private final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();

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

    if (method.equals("PUT") && path.endsWith("/approve")) {
      String segment = path.replace("/api/auctions/", "").replace("/approve", "");
      handleApproveAuction(exchange, segment);
      return;
    }

    if (method.equals("PUT") && path.endsWith("/reject")) {
      String segment = path.replace("/api/auctions/", "").replace("/reject", "");
      handleRejectAuction(exchange, segment);
      return;
    }

    // PUT /api/auctions/{id}/start  → bắt đầu phiên
    if (method.equals("PUT") && path.endsWith("/start")) {
      // Trích xuất id từ /api/auctions/{id}/start
      String segment = path.replace("/api/auctions/", "").replace("/start", "");
      handleStartAuction(exchange, segment);
      return;
    }

    // PUT /api/auctions/{id}/cancel  → hủy phiên (chỉ người tạo hoặc admin)
    if (method.equals("PUT") && path.endsWith("/cancel")) {
      String segment = path.replace("/api/auctions/", "").replace("/cancel", "");
      handleCancelAuction(exchange, segment);
      return;
    }

    // DELETE /api/auctions/{id}  → xóa phiên đã kết thúc/hủy
    if (method.equals("DELETE") && path.startsWith("/api/auctions/")) {
      handleDeleteAuction(exchange, getIDByPath(path, "/api/auctions/"));
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
    User seller = checkToken(exchange);
    if (seller == null) return;

    String body = readBody(exchange);
    TaoPhienRequest req = gson.fromJson(body, TaoPhienRequest.class);

    if (req == null || req.tenPhien == null || req.tenSanPham == null
        || req.maSanPham == null || req.giaKhoiDiem <= 0 || req.thoiGianGiay <= 0) {
      guiPhanHoi(exchange, 400, sendBug("Thiếu hoặc sai thông tin bắt buộc"));
      return;
    }

    Product sanPham;
    ItemFactory itemFactory = ItemFactory.getFactory(req.danhMuc);
    if (itemFactory != null) {
      Items item = itemFactory.createItem(req.tenSanPham, req.maSanPham, req.moTa);
      sanPham = item;
    } else {
      sanPham = new Product(req.tenSanPham, req.maSanPham, req.moTa, req.danhMuc);
    }
     AuctionSession phienMoi = new AuctionSession(
         null, req.tenPhien, sanPham, req.giaKhoiDiem, seller, req.thoiGianGiay);

     // Tính startTime và endTime
     java.time.LocalDateTime thoiGianBD;
     if (req.thoiGianBatDau != null && !req.thoiGianBatDau.isBlank()) {
       try {
         thoiGianBD = java.time.LocalDateTime.parse(req.thoiGianBatDau);
       } catch (Exception e) {
         guiPhanHoi(exchange, 400, sendBug("thoiGianBatDau không đúng định dạng ISO-8601 (vd: 2026-05-20T09:00:00)"));
         return;
       }
     } else {
       // Client không truyền thoiGianBatDau → bắt đầu ngay bây giờ
       thoiGianBD = java.time.LocalDateTime.now();
     }
     phienMoi.setStartTime(thoiGianBD);
     phienMoi.setEndTime(thoiGianBD.plusSeconds(req.thoiGianGiay)); // FIX: endTime không còn null

     // FIX: Set productImgPath từ request vào AuctionSession trước khi save
     if (req.productImgPath != null && !req.productImgPath.isBlank()) {
       phienMoi.setProductImgPath(req.productImgPath);
       logger.info("✅ Đặt productImgPath: " + req.productImgPath);
     }

     auctionRepository.save(phienMoi);

    // FIX: Đồng bộ vào in-memory store để WebSocket tìm thấy
    AuctionSessionRegistry.getInstance().add(phienMoi);

    // Lên lịch tự động chuyển WAITING → IN_PROGRESS khi đến startTime
    // Dùng setASAuction (Auction Start), không phải setACAuction (Auction Close)
    auctionScheduler.setASAuction(phienMoi);

    String maPhienDaSinh = phienMoi.getSessionId();
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

    if (phien.isAccepted() != 1) {
      guiPhanHoi(exchange, 403, sendBug("PhiÃªn chÆ°a Ä‘Æ°á»£c admin duyá»‡t"));
      return;
    }

    auctionSessionService.startAuction(phien);
    auctionRepository.update(phien);

    // FIX: Đảm bảo phiên đang hoạt động được đăng ký để WebSocket tìm thấy
    AuctionSessionRegistry.getInstance().add(phien);

    guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("Phiên " + maPhien + " đã bắt đầu")));
  }

  // =========================================================
// API 5: DELETE /api/auctions/{id}  →  xóa phiên đã hủy/kết thúc
// =========================================================
  private void handleApproveAuction(HttpExchange exchange, String maPhien) throws IOException {
    User admin = checkToken(exchange);
    if (admin == null) return;
    if (!isAdmin(admin)) {
      guiPhanHoi(exchange, 403, sendBug("Chá»‰ admin má»›i Ä‘Æ°á»£c duyá»‡t phiÃªn"));
      return;
    }

    AuctionSession phien = auctionRepository.findById(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("KhÃ´ng tÃ¬m tháº¥y phiÃªn: " + maPhien));
      return;
    }
    if (phien.getStatus() != SessionStatus.PENDING) {
      guiPhanHoi(exchange, 400, sendBug("Chá»‰ cÃ³ thá»ƒ duyá»‡t phiÃªn Ä‘ang PENDING"));
      return;
    }

    auctionSessionService.acceptAuctionRequest(phien);
    guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("ÄÃ£ duyá»‡t phiÃªn " + maPhien)));
  }

  private void handleRejectAuction(HttpExchange exchange, String maPhien) throws IOException {
    User admin = checkToken(exchange);
    if (admin == null) return;
    if (!isAdmin(admin)) {
      guiPhanHoi(exchange, 403, sendBug("Chá»‰ admin má»›i Ä‘Æ°á»£c tá»« chá»‘i phiÃªn"));
      return;
    }

    AuctionSession phien = auctionRepository.findById(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("KhÃ´ng tÃ¬m tháº¥y phiÃªn: " + maPhien));
      return;
    }
    if (phien.getStatus() != SessionStatus.PENDING) {
      guiPhanHoi(exchange, 400, sendBug("Chá»‰ cÃ³ thá»ƒ tá»« chá»‘i phiÃªn Ä‘ang PENDING"));
      return;
    }

    auctionSessionService.denyAuctionRequest(phien);
    guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("ÄÃ£ tá»« chá»‘i phiÃªn " + maPhien)));
  }

  private void handleDeleteAuction(HttpExchange exchange, String maPhien) throws IOException {
    User nguoiYeuCau = checkToken(exchange);
    if (nguoiYeuCau == null) return;

    if (maPhien == null || maPhien.isBlank()) {
      guiPhanHoi(exchange, 400, sendBug("Thiếu mã phiên"));
      return;
    }

    AuctionSession phien = auctionRepository.findById(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("Không tìm thấy phiên: " + maPhien));
      return;
    }

    // Chỉ cho xóa phiên PAID hoặc CANCELLED
    SessionStatus status = phien.getStatus();
    if (status != SessionStatus.PAID && status != SessionStatus.CANCELLED) {
      guiPhanHoi(exchange, 400, sendBug("Chỉ có thể xóa phiên đã kết thúc hoặc đã hủy"));
      return;
    }

    // Bất kỳ user đăng nhập đều được xóa phiên đã kết thúc/hủy
    boolean deleted = auctionRepository.delete(maPhien);
    if (deleted) {
      guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("Đã xóa phiên " + maPhien)));
    } else {
      guiPhanHoi(exchange, 500, sendBug("Lỗi khi xóa phiên"));
    }
  }

  // =========================================================
  // API 6: PUT /api/auctions/{id}/cancel  →  hủy phiên (seller hoặc admin)
  // =========================================================
  /**
   * Hủy phiên đấu giá qua HTTP.
   * Chỉ người tạo phiên được hủy phiên WAITING.
   * Phiên đang IN_PROGRESS không cho hủy qua HTTP để bảo vệ người đang đặt giá.
   */
  private void handleCancelAuction(HttpExchange exchange, String maPhien) throws IOException {
    User nguoiYeuCau = checkToken(exchange);
    if (nguoiYeuCau == null) return;

    if (maPhien == null || maPhien.isBlank()) {
      guiPhanHoi(exchange, 400, sendBug("Thiếu mã phiên"));
      return;
    }

    AuctionSession phien = auctionRepository.findById(maPhien);
    if (phien == null) {
      guiPhanHoi(exchange, 404, sendBug("Không tìm thấy phiên: " + maPhien));
      return;
    }

    SessionStatus status = phien.getStatus();

    // Không hủy phiên đã kết thúc
    if (status == SessionStatus.PAID || status == SessionStatus.CANCELLED) {
      guiPhanHoi(exchange, 400, sendBug("Phiên đã kết thúc, không thể hủy"));
      return;
    }

    // Phiên đang diễn ra: không cho hủy qua HTTP
    if (status == SessionStatus.IN_PROGRESS) {
      guiPhanHoi(exchange, 403, sendBug(
          "Không thể hủy phiên đang diễn ra. Liên hệ admin hoặc để phiên tự kết thúc."));
      return;
    }

    // Phiên WAITING: chỉ người tạo phiên mới được hủy
    boolean isSeller = phien.getSeller() != null
        && phien.getSeller().getUserId().equals(nguoiYeuCau.getUserId());
    if (!isSeller && !isAdmin(nguoiYeuCau)) {
      guiPhanHoi(exchange, 403, sendBug("Chỉ người tạo phiên hoặc admin mới được hủy"));
      return;
    }

    // Gọi service để hủy đúng flow: giải phóng frozen, broadcast WebSocket, cập nhật DB
    AuctionSessionService.getInstance()
        .closeAuction(phien, SessionStatus.CANCELLED);

    logger.info("[AuctionController] Seller {} đã hủy phiên {}", nguoiYeuCau.getEmail(), maPhien);
    guiPhanHoi(exchange, 200, gson.toJson(new ThongBao("Đã hủy phiên " + maPhien)));
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

  private boolean isAdmin(User user) {
    return user != null && user.getRole() == 1;
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
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
    // FIX: Phải gọi exchange.close() tường minh để com.sun.net.httpserver
    // giải phóng kết nối đúng cách. Thiếu dòng này → server giữ socket lơ lửng
    // → client nhận EOF/Unexpected end of file khi đọc response.
    exchange.close();
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
    String thoiGianBatDau; // ISO-8601, vd: "2026-05-20T09:00:00" — tuỳ chọn, nếu có sẽ lên lịch tự động mở
    String productImgPath; // Đường dẫn ảnh sản phẩm trên local directory của seller

    public TaoPhienRequest(String tenPhien, String tenSanPham, String maSanPham, String danhMuc, String moTa, String thoiGianBatDau, double giaKhoiDiem, int thoiGianGiay) {
      this.tenPhien = tenPhien;
      this.tenSanPham = tenSanPham;
      this.maSanPham = maSanPham;
      this.danhMuc = danhMuc;
      this.moTa = moTa;
      this.thoiGianBatDau = thoiGianBatDau;
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
    String moTa;
    String danhMucSanPham;
    String thoiGianBatDau;
    String thoiGianKetThuc;
    String productImgPath;

    TomTatPhien(AuctionSession p) {
      this.maPhien         = p.getSessionId();
      this.tenPhien        = p.getSessionName();
      this.giaHienTai      = p.getCurrentPrice();
      this.trangThai       = p.getStatus().name();
      this.tenNguoiBan     = p.getSeller()    != null ? p.getSeller().getFullName()     : null;
      this.tenSanPham      = p.getProduct()   != null ? p.getProduct().getProductName() : null;
      this.moTa            = p.getProduct()   != null ? p.getProduct().getDescription() : null;
      this.danhMucSanPham  = p.getProduct()   != null ? p.getProduct().getCategory()    : null;
      this.thoiGianBatDau  = p.getStartTime() != null ? p.getStartTime().toString()     : null;
      this.thoiGianKetThuc = p.getEndTime()   != null ? p.getEndTime().toString()       : null;
      this.productImgPath  = p.getProductImgPath();
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
    String moTa;
    String danhMucSanPham;
    String tenNguoiThangCuoc;
    String thoiGianBatDau;
    String thoiGianKetThuc;
    int    soNguoiTraGia;
    String productImgPath;

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
      this.moTa              = p.getProduct()        != null ? p.getProduct().getDescription()        : null;
      this.danhMucSanPham    = p.getProduct()        != null ? p.getProduct().getCategory()           : null;
      this.tenNguoiThangCuoc = p.getWinner() != null ? p.getWinner().getFullName()    : null;
      this.thoiGianBatDau    = p.getStartTime()  != null ? p.getStartTime().toString()   : null;
      this.thoiGianKetThuc   = p.getEndTime() != null ? p.getEndTime().toString()  : null;
      this.soNguoiTraGia     = p.getBidderList() != null ? p.getBidderList().size() : 0;
      this.productImgPath    = p.getProductImgPath();
    }
  }

  private static class IsBankAccAvailable {
    boolean isAvailable;

    IsBankAccAvailable(boolean isAvailable) {
      this.isAvailable = isAvailable;
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
