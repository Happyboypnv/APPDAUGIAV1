# GIẢI THÍCH TOÀN BỘ PHẦN SERVER — TỪ ĐẦU ĐẾN CUỐI

---

## PHẦN 1: TẠI SAO CẦN SERVER?

Trước khi có server, app hoạt động như này:

```
Máy A:  [JavaFX] → [SQLite trên máy A]
Máy B:  [JavaFX] → [SQLite trên máy B]
```

Hai người không thể đấu giá với nhau vì dữ liệu nằm trên 2 máy khác nhau,
không có kết nối.

Giải pháp là tách database ra một máy trung tâm:

```
Máy A: [JavaFX] ──┐
                  ├──► [SERVER + SQLite]   ← tất cả dùng chung 1 database
Máy B: [JavaFX] ──┘
```

Đây là lý do phải làm server.

---

## PHẦN 2: HAI MÁY NÓI CHUYỆN VỚI NHAU BẰNG GÌ?

Chọn **HTTP** — đúng giao thức mà browser dùng để vào Google, Facebook.

```
Chrome vào Google:
  Chrome ──── HTTP Request ────► Google Server
  Chrome ◄─── HTTP Response ─── Google Server

JavaFX gọi server của mình:
  JavaFX ──── HTTP Request ────► Server của mình
  JavaFX ◄─── HTTP Response ─── Server của mình
```

Tư duy ở đây là dùng lại cách internet đã hoạt động sẵn.

---

## PHẦN 3: DỮ LIỆU ĐƯỢC GỬI DƯỚI DẠNG GÌ?

Máy A không thể gửi object Java thẳng qua mạng cho máy B được.
Vì máy B không hiểu bộ nhớ RAM của máy A.

Giải pháp: chuyển object thành TEXT (JSON), gửi text qua mạng,
bên kia chuyển ngược lại.

```
Máy A gửi:
  LoginRequest object  →  gson.toJson()  →  '{"email":"a@b.com","matKhau":"123"}'
                                                       │
                                              gửi qua mạng (text)
                                                       │
Máy B nhận:                                           ▼
  LoginRequest object  ←  gson.fromJson()  ←  '{"email":"a@b.com","matKhau":"123"}'
```

Thư viện Gson làm việc chuyển đổi 2 chiều này.

---

## PHẦN 4: CẤU TRÚC CODE GỒM 3 TẦNG

```
ServerApp.java         → Mở server, đăng ký URL, phân luồng request
      ↓
UserController.java    → Nhận request, kiểm tra, xử lý, trả kết quả
      ↓
DTO (LoginRequest...)  → Định nghĩa hình dạng dữ liệu vào/ra
```

---

## PHẦN 5: OPTIONS LÀ GÌ — GIẢI THÍCH TỪ GỐC

### 5.1 — Tại sao browser không gửi POST thẳng mà phải gửi OPTIONS trước?

Đây là câu hỏi quan trọng nhất.

Browser là công cụ hàng tỷ người dùng để vào mọi trang web.
Nếu browser cho phép gửi POST thẳng đến bất kỳ địa chỉ nào,
chuyện này có thể xảy ra:

```
1. Bạn đang đăng nhập ngân hàng tại bank.com
2. Bạn vô tình click vào link evil.com
3. evil.com âm thầm chạy code ngầm:
       fetch("http://bank.com/chuyen-tien", {
           method: "POST",
           body: { soTien: 10000000, nguoiNhan: "hacker" }
       })
4. Browser gửi POST thẳng đến bank.com
5. Bank nhận POST → chuyển tiền → xong
```

Bạn không làm gì cả nhưng mất tiền vì evil.com gửi POST thay bạn.

Để ngăn điều này, tổ chức W3C (tổ chức đặt ra tiêu chuẩn web toàn cầu)
quy định: trước khi gửi POST đến domain khác, browser BẮT BUỘC phải
hỏi server đích trước.

Câu hỏi đó chính là request OPTIONS.

```
Browser hỏi bank.com:
   "Này bank.com, evil.com có được phép gửi POST đến mày không?"

bank.com trả lời: "KHÔNG"

Browser: "Vậy tao từ chối gửi POST cho evil.com."
```

Tiền an toàn. evil.com bị chặn.

Mọi browser Chrome, Firefox, Safari đều phải tuân theo luật này.
Đây không phải lựa chọn của lập trình viên — đây là tiêu chuẩn bắt buộc.

---

### 5.2 — Quy trình 2 bước

```
BƯỚC 1 — Browser tự gửi OPTIONS (bạn không gọi cái này):
   Browser ──► OPTIONS /api/users/login
               "Server ơi, tôi có được phép gửi POST không?"

   Server ──► 200 OK
              Header: Access-Control-Allow-Origin: *
              "Được, mày vào đi"

BƯỚC 2 — Bây giờ browser mới gửi POST thật:
   Browser ──► POST /api/users/login
               Body: {email, matKhau}

   Server ──► 200 OK
              Body: {token, hoTen...}
```

Quan trọng: OPTIONS và POST là **2 request riêng biệt**.
Không phải 1 request gồm 2 phần.

---

### 5.3 — Tại sao có `return` rồi mà POST vẫn xuống được?

Đây là chỗ hay nhầm nhất.

```java
server.createContext("/api/users/login", exchange -> {
    if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
        xuLyCors(exchange);
        return;
    }
    userController.handleLogin(exchange);
});
```

Vì OPTIONS và POST là 2 request riêng biệt, hàm lambda trên
được gọi 2 lần riêng biệt:

```
Lần 1 — Browser gửi OPTIONS:
  → Hàm được gọi lần 1
  → getRequestMethod() = "OPTIONS" → điều kiện true
  → xuLyCors() → return
  → Hàm kết thúc lần 1. Server vẫn đứng đó chờ tiếp.

Lần 2 — Browser gửi POST:
  → Hàm được gọi lần 2, hoàn toàn mới từ đầu
  → getRequestMethod() = "POST" → điều kiện false
  → bỏ qua if
  → chạy xuống handleLogin()
```

`return` chỉ dừng lần xử lý đó lại.
Không dừng server, không ngăn request tiếp theo.

---

### 5.4 — Nếu không có cái `if OPTIONS` thì sao?

```java
// Giả sử xóa if đi:
server.createContext("/api/users/login", exchange -> {
    userController.handleLogin(exchange);   // chạy thẳng vào đây
});
```

Khi browser gửi OPTIONS vào, server chạy thẳng vào `handleLogin()`.
Bên trong `handleLogin()` có:

```java
if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
    guiPhanHoi(exchange, 405, "Chỉ chấp nhận POST");
    return;
}
```

OPTIONS không phải POST → server trả lỗi **405 Method Not Allowed**.

Browser nhận 405 → hiểu là "không được phép" → tự chặn POST thật,
không gửi nữa → login không hoạt động dù code đúng hoàn toàn.

---

### 5.5 — Tóm lại OPTIONS trong 4 câu

```
1. Browser không cho phép gửi POST thẳng đến domain khác.
2. Browser tự gửi OPTIONS trước để hỏi server có đồng ý không.
3. Server phải trả lời "đồng ý" thì browser mới gửi POST thật.
4. Cái if OPTIONS trong code là để bắt câu hỏi đó và trả lời "đồng ý".
```

---

## PHẦN 6: GIẢI THÍCH TỪNG FILE

---

### ServerApp.java

```java
KetNoiCSDL.khoiTao();
new KhoLuuTruNguoiDungSQLite().migratePlainTextPasswords();
```
Khởi tạo database trước khi nhận bất kỳ request nào.
Nếu thiếu 2 dòng này, server chạy nhưng mọi query đều báo lỗi
"no such table" vì bảng chưa được tạo.

---

```java
HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
```
Tạo server, bảo nó lắng nghe ở cổng 8080.
`0.0.0.0:8080` nghĩa là nhận kết nối từ mọi địa chỉ.

---

```java
server.createContext("/api/users/login", exchange -> { ... });
```
Đăng ký rule: ai gọi đến địa chỉ này thì chạy đoạn code trong đó.
Giống như lễ tân: "Anh cần check-in? Tôi dẫn anh đến quầy số 1."

---

```java
server.setExecutor(Executors.newFixedThreadPool(10));
```
Thuê 10 nhân viên làm việc song song.
Không có dòng này → chỉ có 1 nhân viên → 10 người gọi cùng lúc
thì 9 người phải ngồi chờ.

---

```java
private static void xuLyCors(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    exchange.sendResponseHeaders(200, -1);
    exchange.close();
}
```
Trả lời "được phép" cho browser khi nó hỏi thăm dò bằng OPTIONS.

`"*"` = cho phép mọi domain gọi đến.
`-1` = response không có body, chỉ có header.

---

### UserController.java — handleLogin()

```
Nhận request
    │
    ▼
Đúng method POST không? ──Không──► Trả lỗi 405
    │ Đúng
    ▼
Đọc body, parse JSON thành LoginRequest
    │
    ▼
Email và matKhau có null không? ──Có──► Trả lỗi 400
    │ Không
    ▼
Kiểm tra email + matKhau với DB ──Sai──► Trả lỗi 401
    │ Đúng
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
Đọc text JSON từ request, chuyển thành object Java để dễ dùng.

```java
boolean hopLe = khoNguoiDung.kiemTraNguoiDung(req.getEmail(), req.getMatKhau());
```
Tái sử dụng code đã có ở phần JavaFX. Không viết lại.
Bên trong: hash matKhau với salt → so sánh với DB → trả true/false.

```java
String token = "USER_" + req.getEmail() + "_" + System.currentTimeMillis();
```
Tạo thẻ ra vào cho client. Lần sau client dùng thẻ này thay vì
gửi email/mật khẩu mỗi lần.
`currentTimeMillis()` = số mili giây từ 01/01/1970, đảm bảo unique.

---

### UserController.java — handleRegister()

```java
if (!khoNguoiDung.kiemTraEmail(req.getEmail())) { ... }
```
`kiemTraEmail()` trả `true` nếu email chưa có → được đăng ký.
Nên `!kiemTraEmail()` = email đã có → báo lỗi.

```java
String salt          = BoMaHoaMatKhau.taoSalt();
String matKhauDaHash = BoMaHoaMatKhau.maHoaMatKhau(req.getMatKhau(), salt);
nguoiDungMoi.setSalt(salt);
```
Không bao giờ lưu mật khẩu gốc.
Tạo salt ngẫu nhiên → trộn vào mật khẩu → hash → lưu kết quả.
Kể cả admin xem database cũng không biết mật khẩu thật.

---

### UserController.java — handleGetUser()

```java
String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    guiPhanHoi(exchange, 401, "Cần đăng nhập trước");
    return;
}
```
Đây là lỗi đã được sửa: trước đây ai cũng lấy được thông tin user
mà không cần đăng nhập. Bây giờ phải có token trong header thì mới trả.

```java
String email = path.substring("/api/users/".length());
```
URL là `/api/users/abc@gmail.com` → cắt bỏ phần đầu → còn `abc@gmail.com`.

```java
private static class ThongTinNguoiDung {
    // Có: maNguoiDung, hoTen, email, ngaySinh, diaChi, soDienThoai, soDuKhaDung
    // KHÔNG có: matKhau, salt
}
```
Lớp bọc bên ngoài chỉ để lọc thông tin.
Dù database lưu 10 field, client chỉ nhận 7 field an toàn.

---

### guiPhanHoi() — Gửi response

```java
byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
exchange.sendResponseHeaders(statusCode, bytes.length);
try (OutputStream os = exchange.getResponseBody()) {
    os.write(bytes);
}
```
Giao tiếp mạng là gửi/nhận bytes, không phải String.
Phải chuyển String → bytes trước khi gửi.
`sendResponseHeaders` phải gọi trước khi gửi body — đây là quy tắc HTTP.

---

### DTO — Tại sao cần?

Nếu dùng thẳng class NguoiDung và serialize ra JSON:

```json
{
  "hoTen": "Nguyen Van A",
  "email": "a@b.com",
  "matKhau": "3rUu+LPzYC...",   ← LỘ MẬT KHẨU!
  "salt": "lkm8+q1ndl...",      ← LỘ SALT!
}
```

Dùng DTO LoginResponse chỉ trả về đúng thứ cần:

```json
{
  "token": "USER_a@b.com_17141...",
  "email": "a@b.com",
  "hoTen": "Nguyen Van A",
  "thongBao": "Đăng nhập thành công"
}
```

DTO là bộ lọc — chỉ cho ra ngoài những thứ client cần.

Constructor rỗng trong DTO là bắt buộc vì Gson tạo object bằng cách
gọi constructor rỗng trước, sau đó mới điền field vào.
Không có constructor rỗng → Gson không tạo được object → crash.

---

## PHẦN 7: BẢNG HTTP STATUS CODE

| Code | Tên | Dùng khi |
|------|-----|----------|
| 200 | OK | Thành công |
| 201 | Created | Tạo mới thành công (đăng ký) |
| 400 | Bad Request | Thiếu field, email đã tồn tại |
| 401 | Unauthorized | Sai mật khẩu, thiếu token |
| 404 | Not Found | Không tìm thấy user |
| 405 | Method Not Allowed | Gọi sai HTTP method |

---

## PHẦN 8: LUỒNG ĐẦY ĐỦ KHI ĐĂNG NHẬP

```
Browser                              Server
  │                                    │
  │  Bước 1: OPTIONS /api/users/login  │
  │ ──────────────────────────────────►│
  │                                    │  if OPTIONS → xuLyCors() → return
  │◄──────────────────────────────────│
  │  200 OK, Allow: *                  │
  │                                    │
  │  Bước 2: POST /api/users/login     │
  │  Body: {email, matKhau}            │
  │ ──────────────────────────────────►│
  │                                    │  docBody() → gson.fromJson()
  │                                    │  kiemTraNguoiDung()
  │                                    │  tạo token
  │                                    │  gson.toJson()
  │◄──────────────────────────────────│
  │  200 OK                            │
  │  Body: {token, email, hoTen}       │
```
