# ỨNG DỤNG ĐẤU GIÁ TRỰC TUYẾN (HiPiTi)

## 1. Mô tả bài toán và phạm vi hệ thống
**HiPiTi** là hệ thống đấu giá trực tuyến thời gian thực được xây dựng theo mô hình Client-Server Desktop thuần Java. Dự án mô phỏng một nền tảng đấu giá đầy đủ, cho phép nhiều người dùng đồng thời tham gia đặt giá, theo dõi biến động giá thầu và thực hiện các giao dịch tài chính một cách an toàn thông qua kiến trúc phân tầng rõ ràng.
Hệ thống phục vụ 3 vai trò chính:
- **Người mua (Bidder):** Tham gia đấu giá và quản lý ví điện tử.
- **Người bán (Seller):** Đăng tải sản phẩm và tạo phiên đấu giá.
- **Quản trị viên (Admin):** Giám sát toàn hệ thống, duyệt phiên đấu giá và quản lý tài khoản.

## 2. Công nghệ, môi trường chạy và yêu cầu cài đặt
**Công nghệ sử dụng:**
- **Ngôn ngữ:** Java 17+ (LTS)
- **Build System:** Maven Multi-Module (3.9+)
- **Giao diện:** JavaFX 17 + FXML, CSS
- **HTTP Server:** `com.sun.net.httpserver` (JDK built-in, port 8080)
- **WebSocket:** Java-WebSocket (port 8081)
- **Database:** SQLite (JDBC) với WAL mode
- **Bảo mật:** SHA-256 + Salt, Token Bearer

**Yêu cầu cài đặt:**
- Java Development Kit (JDK) phiên bản 17 trở lên.
- Apache Maven phiên bản 3.9+ (hoặc dùng Maven Wrapper).
- Hệ điều hành: Windows, macOS hoặc Linux.

## 3. Cấu trúc module chính
Dự án được tổ chức theo Maven Multi-Module gồm 3 phần:
- `shared`: Chứa các Model, Interface, DTOs, Exception dùng chung.
- `server`: Chứa HTTP Server, WebSocket Server, Controller, Service, Repository, Scheduler.
- `client`: Chứa giao diện JavaFX Controller, FXML view, ApiClient, WebSocket Client, Local cache.

## 4. Vị trí các file .jar
Sau khi build bằng lệnh `mvn clean package -DskipTests`, các file thực thi (fat JAR) sẽ nằm tại:
- Server: `server/target/server.jar`
- Client: `client/target/client.jar`

## 5. Hướng dẫn chạy chương trình
Cần phải chạy Server trước, sau đó mới chạy Client.

**Bước 1: Chạy Server**
Mở terminal tại thư mục gốc của project và chạy lệnh:
```bash
java -jar server/target/server.jar
```
*(Server sẽ lắng nghe tại `http://localhost:8080` và `ws://localhost:8081`. File database `hipiti.db` sẽ được tự động tạo)*

**Bước 2: Chạy Client**
Mở thêm một terminal khác và chạy lệnh:
```bash
java -jar client/target/client.jar
```

**Tài khoản đăng nhập mặc định:**
- **Admin:** Được cài mặc định tài khoản ứng với các thành viên trong nhóm.
- **Người dùng:** Có thể tự đăng ký tài khoản mới trực tiếp từ giao diện Client.

## 6. Danh sách chức năng đã hoàn thành
- [x] Đăng ký / Đăng nhập (Xác thực SHA-256+Salt, quản lý session).
- [x] Quản lý ví điện tử (Nạp/rút tiền, cơ chế phong tỏa số dư - frozen balance).
- [x] Quản lý người dùng (Admin ban/unban tài khoản, đổi mật khẩu).
- [x] Đa danh mục sản phẩm (Nghệ thuật, Xe cộ, Điện tử, Đá quý).
- [x] Quản lý phiên đấu giá (Seller đăng sản phẩm, Admin duyệt/từ chối phiên).
- [x] Đặt giá realtime (WebSocket broadcast kết quả đặt giá tức thì).
- [x] Chống bắn tỉa (Anti-snipe) (Gia hạn thêm 30s nếu có giá đặt trong phút cuối).
- [x] Thanh toán tự động (Tự động trừ tiền người thắng và cộng tiền người bán).
- [x] Lịch sử đấu giá & Lịch sử giao dịch.
- [x] Cập nhật profile cá nhân.


## 7. Tài liệu và Demo
- **Báo cáo PDF:** https://drive.google.com/file/d/1qa989jYLkOt0ytU17Q8ggOYkALnGABnY/view?usp=drive_link
- **Video Demo:** https://drive.google.com/file/d/11rb2BbCG5NOVUVBfaUZDZOmXIaYlihHI/view?usp=sharing