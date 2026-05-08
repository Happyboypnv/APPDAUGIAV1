# 📡 HIỂU PHẦN SERVER TỪ GỐC ĐẾN NGỌN

---

## 🧠 PHẦN 1: TƯ DUY — TẠI SAO CẦN SERVER?

### Vấn đề của app hiện tại

Trước khi có server, app hoạt động như sau:

```
Máy A:  [JavaFX] → [SQLite trên máy A]
Máy B:  [JavaFX] → [SQLite trên máy B]  ← Dữ liệu hoàn toàn khác nhau!
```

**Hậu quả:** Người A và người B không thể đấu giá với nhau vì dữ liệu nằm
trên 2 máy khác nhau, không có kết nối.

### Giải pháp: Tách Database ra một máy trung tâm

```
Máy A:  [JavaFX] ──┐
                   ├──► [SERVER + SQLite]  ← Dữ liệu dùng chung
Máy B:  [JavaFX] ──┘
```

**Kết quả:** Tất cả mọi người cùng nhìn vào 1 nguồn dữ liệu duy nhất.
Đây là lý do phải làm server.

---

## 🧠 PHẦN 2: TƯ DUY — CLIENT VÀ SERVER NÓI CHUYỆN VỚI NHAU BẰNG GÌ?

### Vấn đề: 2 chương trình khác nhau, chạy trên 2 máy khác nhau

Máy A chạy JavaFX (Java). Máy B chạy server (Java). Làm sao chúng giao tiếp?

**Giải pháp được chọn: HTTP** — giao thức mà browser dùng để nói chuyện với web.

```
Bạn dùng Chrome truy cập Google:
  Chrome ──── HTTP Request ────► Google Server
  Chrome ◄─── HTTP Response ─── Google Server

App JavaFX gọi server của mình:
  JavaFX ──── HTTP Request ────► Server của mình
  JavaFX ◄─── HTTP Response ─── Server của mình
```

Tư duy ở đây là: **dùng lại cách internet đã hoạt động sẵn**, không phải
phát minh lại từ đầu.

---

## 🧠 PHẦN 3: TƯ DUY — HTTP REQUEST LÀ GÌ?

Một HTTP Request gồm 3 thành phần chính:

```
┌─────────────────────────────────────────────┐
│  METHOD + URL                               │
│  POST http://localhost:8080/api/users/login │
├─────────────────────────────────────────────┤
│  HEADERS (thông tin meta)                   │
│  Content-Type: application/json             │
├─────────────────────────────────────────────┤
│  BODY (dữ liệu gửi đi)                      │
│  {"email": "a@gmail.com", "matKhau": "123"} │
└─────────────────────────────────────────────┘
```

**METHOD** = động từ, nói lên ý định:
- `GET`    = tôi muốn **lấy** dữ liệu
- `POST`   = tôi muốn **gửi/tạo mới** dữ liệu
- `PUT`    = tôi muốn **cập nhật** dữ liệu
- `DELETE` = tôi muốn **xóa** dữ liệu

**URL** = địa chỉ, nói rõ muốn làm gì:
- `/api/users/login`    = đăng nhập
- `/api/users/register` = đăng ký
- `/api/users/abc@gmail.com` = lấy thông tin user này

---

## 🧠 PHẦN 4: TƯ DUY — DỮ LIỆU ĐƯỢC GỬI DƯỚI DẠNG GÌ?

Máy A gửi **object Java** → không thể gửi thẳng qua mạng được vì máy B
không hiểu bộ nhớ RAM của máy A.

**Giải pháp: Chuyển object thành TEXT (JSON), gửi text qua mạng, bên kia chuyển ngược lại**

```
Máy A (gửi):
  LoginRequest object  →  gson.toJson()  →  '{"email":"a@b.com","matKhau":"123"}'
                                                        │
                                               Gửi qua mạng (text)
                                                        │
Máy B (nhận):                                          ▼
  LoginRequest object  ←  gson.fromJson()  ←  '{"email":"a@b.com","matKhau":"123"}'
```

Thư viện **Gson** làm việc chuyển đổi 2 chiều này. Đây gọi là
**Serialization** (object → text) và **Deserialization** (text → object).

---

## 🧠 PHẦN 5: TƯ DUY — CẤU TRÚC CODE ĐƯỢC TỔ CHỨC NHƯ THẾ NÀO?

Người làm server tổ chức code theo **3 tầng tư duy**:

```
┌──────────────────────────────────────────────────┐
│  TẦNG 1: ServerApp.java                          │
│  "Mở cửa hàng, treo biển, phân luồng khách"     │
│  → Khởi động server, đăng ký các URL endpoint   │
└──────────────────────┬───────────────────────────┘
                       │ giao việc cho
┌──────────────────────▼───────────────────────────┐
│  TẦNG 2: UserController.java                     │
│  "Nhân viên tiếp khách, xử lý yêu cầu"          │
│  → Đọc request, validate, trả response          │
└──────────────────────┬───────────────────────────┘
                       │ dùng lại
┌──────────────────────▼───────────────────────────┐
│  TẦNG 3: DTO (LoginRequest, LoginResponse...)    │
│  "Tờ đơn điền thông tin"                        │
│  → Định nghĩa hình dạng của dữ liệu vào/ra      │
└──────────────────────────────────────────────────┘
```

Tư duy ở đây là **tách trách nhiệm** — mỗi tầng chỉ lo 1 việc, không ai
làm việc của người khác.

---

## 🧠 PHẦN 6: TƯ DUY — DTO LÀ GÌ VÀ TẠI SAO CẦN?

**DTO = Data Transfer Object** = "tờ đơn" mẫu để điền thông tin.

**Câu hỏi:** Tại sao không dùng thẳng class `NguoiDung` sẵn có?

```java
// ❌ Nếu dùng NguoiDung thẳng và serialize ra JSON:
{
  "maNguoiDung": "PPTT000001",
  "hoTen": "Nguyen Van A",
  "email": "a@b.com",
  "matKhau": "3rUu+LPzYC...",   ← LỘ MẬT KHẨU!
  "salt": "lkm8+q1ndl...",      ← LỘ SALT!
  "soDuKhaDung": 500.0,
  "cacGiaoDich": [...]           ← DỮ LIỆU THỪA
}

// ✅ Dùng DTO LoginResponse chỉ trả về những gì cần:
{
  "token": "USER_a@b.com_17141...",
  "email": "a@b.com",
  "hoTen": "Nguyen Van A",
  "thongBao": "Đăng nhập thành công"
}
```

**Tư duy:** DTO là bộ lọc — chỉ cho ra ngoài những thứ client cần,
giữ lại những thứ nhạy cảm.

---

## 📄 PHẦN 7: GIẢI THÍCH TỪNG FILE (SAU KHI ĐÃ HIỂU TƯ DUY)

---

### ServerApp.java — "Người mở cửa hàng"

**Nhiệm vụ tổng quát:** Khởi động server, nói với Java rằng
"khi có người gọi đến URL này thì làm việc này".

```java
HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
```
> Tạo server, bảo nó lắng nghe ở cổng 8080.
> Giống như mở điện thoại và chờ cuộc gọi đến số 8080.

```java
server.createContext("/api/users/login", exchange -> {
    userController.handleLogin(exchange);
});
```
> Đăng ký rule: "Ai gọi đến địa chỉ /api/users/login thì chuyển cho
> handleLogin() xử lý".
> Giống như lễ tân khách sạn: "Anh cần check-in? Tôi dẫn anh đến quầy số 1".

```java
server.setExecutor(Executors.newFixedThreadPool(10));
```
> Thuê 10 nhân viên làm việc song song.
> Không có dòng này → chỉ có 1 nhân viên → 10 người gọi cùng lúc thì 9 người phải chờ.

```java
if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
    xuLyCors(exchange);
    return;
}
```
> Browser có thói quen hỏi thăm dò trước ("Tôi có được phép gọi không?")
> bằng method OPTIONS. Phải trả lời "được" thì browser mới gửi request thật.
> Đây gọi là CORS preflight — đặc điểm của browser, không phải bug.

---

### UserController.java — "Nhân viên tiếp khách"

**Nhiệm vụ tổng quát:** Nhận yêu cầu, kiểm tra hợp lệ, xử lý, trả kết quả.

#### handleLogin() — Xử lý đăng nhập

**Tư duy xử lý đăng nhập:**
```
Nhận request
    │
    ▼
Đúng method POST không? ──No──► Trả lỗi 405
    │ Yes
    ▼
Đọc body, parse JSON thành LoginRequest
    │
    ▼
Email và matKhau có bị null không? ──Yes──► Trả lỗi 400
    │ No
    ▼
Email + matKhau có khớp DB không? ──No──► Trả lỗi 401
    │ Yes
    ▼
Tạo token, lấy họ tên
    │
    ▼
Trả về 200 + JSON chứa token
```

```java
String body = docBody(exchange);
LoginRequest req = gson.fromJson(body, LoginRequest.class);
```
> Đọc cái "tờ đơn" client gửi lên (dạng text JSON),
> rồi chuyển thành object Java để dễ dùng.

```java
boolean hopLe = khoNguoiDung.kiemTraNguoiDung(req.getEmail(), req.getMatKhau());
```
> Tái sử dụng code đã viết sẵn ở phần JavaFX. Không viết lại.
> Bên trong: hash matKhau với salt lấy từ DB → so sánh → đúng/sai.

```java
String token = "USER_" + req.getEmail() + "_" + System.currentTimeMillis();
```
> Tạo "thẻ ra vào" cho client. Lần sau client dùng thẻ này thay vì
> gửi email/mật khẩu mỗi lần.
> `currentTimeMillis()` để thẻ của mỗi lần login khác nhau.

---

#### handleRegister() — Xử lý đăng ký

**Tư duy:** Giống `LoginAction.dangKy()` bên JavaFX, nhưng qua HTTP.

```java
if (!khoNguoiDung.kiemTraEmail(req.getEmail())) {
    guiPhanHoi(exchange, 400, ...);
    return;
}
```
> `kiemTraEmail()` trả `true` = email chưa có → được đăng ký.
> Nên `!kiemTraEmail()` = email đã có → báo lỗi.

```java
String salt          = BoMaHoaMatKhau.taoSalt();
String matKhauDaHash = BoMaHoaMatKhau.maHoaMatKhau(req.getMatKhau(), salt);
nguoiDungMoi.setSalt(salt);
khoNguoiDung.luu(nguoiDungMoi);
```
> Quy trình bảo mật: không bao giờ lưu mật khẩu gốc.
> Tạo salt ngẫu nhiên → trộn vào mật khẩu → hash → lưu kết quả.
> Kể cả admin xem DB cũng không biết mật khẩu thật.

---

#### handleGetUser() — Lấy thông tin user

```java
String path  = exchange.getRequestURI().getPath();
String email = path.substring("/api/users/".length());
```
> URL là `/api/users/abc@gmail.com` → cắt bỏ phần đầu → còn `abc@gmail.com`.
> Dùng email này để tra DB.

```java
private static class ThongTinNguoiDung {
    // Chỉ có: maNguoiDung, hoTen, email, ngaySinh, diaChi, soDienThoai, soDuKhaDung
    // KHÔNG có: matKhau, salt
}
```
> Lớp bọc bên ngoài — chỉ để lọc thông tin.
> Ngay cả khi DB lưu 10 field, client chỉ nhận được 7 field an toàn.

---

#### guiPhanHoi() — Gửi response về client

```java
byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
exchange.sendResponseHeaders(statusCode, bytes.length);
try (OutputStream os = exchange.getResponseBody()) {
    os.write(bytes);
}
```
> **Tư duy:** Giao tiếp mạng là gửi/nhận bytes, không phải String.
> Phải chuyển String → bytes trước khi gửi.
> `sendResponseHeaders` phải gọi TRƯỚC khi gửi body — đây là quy tắc HTTP.

---

### DTO Files — "Những tờ đơn mẫu"

**Tư duy chung:** Mỗi chiều giao tiếp cần 1 tờ đơn riêng.

```
Client → Server:  LoginRequest    (email + matKhau)
Client → Server:  RegisterRequest (hoTen + email + matKhau + ngaySinh)
Server → Client:  LoginResponse   (token + email + hoTen + thongBao)
```

```java
public LoginRequest() {}  // Constructor rỗng — bắt buộc cho Gson
```
> Gson hoạt động bằng cách: tạo object rỗng trước → điền field sau.
> Không có constructor rỗng → Gson không tạo được object → crash.

---

## 🔴 2 LỖI CẦN SỬA

### Lỗi 1: Quên khởi tạo Database

Server và JavaFX app dùng chung database. Khi chạy `ServerApp.main()` độc lập,
chưa có gì gọi `KetNoiCSDL.khoiTao()` → bảng chưa được tạo → query crash.

```java
// Thêm vào đầu main() của ServerApp:
KetNoiCSDL.khoiTao();
new KhoLuuTruNguoiDungSQLite().migratePlainTextPasswords();
```

### Lỗi 2: GET user không cần đăng nhập

Ai cũng có thể gọi `GET /api/users/abc@gmail.com` và xem thông tin.
Cần kiểm tra token trong header `Authorization` trước khi trả dữ liệu.

---

## 📊 TỔNG KẾT TƯ DUY

| Câu hỏi | Trả lời |
|---------|---------|
| Tại sao cần server? | Để nhiều máy dùng chung 1 database |
| Giao tiếp bằng gì? | HTTP — giao thức sẵn có của internet |
| Dữ liệu gửi dạng gì? | JSON text — Gson chuyển đổi 2 chiều |
| Code tổ chức thế nào? | 3 tầng: ServerApp → Controller → DTO |
| DTO để làm gì? | Lọc dữ liệu — chỉ cho ra thứ cần thiết |
| Bảo mật password? | Tái dùng BoMaHoaMatKhau đã có sẵn |
