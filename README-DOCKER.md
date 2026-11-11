# Hướng dẫn Chạy Toàn Bộ Dự Án NewMen Store Bằng Docker

Dự án đã được cấu hình **Multi-stage Docker Build** hoàn chỉnh. Khi chuyển mã nguồn sang bất kỳ máy tính nào khác (Windows, macOS, Ubuntu/Linux), bạn **KHÔNG CẦN** cài đặt:
- ❌ Không cần cài Java / JDK
- ❌ Không cần cài Maven
- ❌ Không cần cài Node.js / npm

👉 **Yêu cầu duy nhất:** Máy tính có cài đặt **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** (hoặc Docker Engine + Docker Compose).

---

## 1. Khởi động nhanh (Chỉ 1 lệnh duy nhất)

Mở terminal/PowerShell tại thư mục gốc của dự án (`d-a-v-o-t-i-3`) và chạy:

```bash
docker compose up --build
```

*(Hoặc chạy ngầm dưới nền với cờ `-d`:)*
```bash
docker compose up --build -d
```

Docker sẽ tự động:
1. **Build Backend:** Tải JDK 21 & Maven trong container, biên dịch mã nguồn Spring Boot, đóng gói JAR và chuyển sang môi trường runtime JRE 21 siêu nhẹ.
2. **Build Frontend:** Tải Node 20 trong container, cài đặt thư viện npm, biên dịch Angular 22 và chuyển toàn bộ file tĩnh sang web server Nginx.
3. **Cấu hình mạng nội bộ:** Kết nối Frontend Nginx tới Backend API và kiểm tra trạng thái sức khỏe (healthcheck) tự động.

---

## 2. Đường dẫn truy cập các phân hệ

| Phân hệ | Đường dẫn truy cập | Ghi chú |
| :--- | :--- | :--- |
| **Giao diện Bán hàng (Storefront)** | **`http://localhost:4220`** | Đầy đủ ảnh sản phẩm, Gợi ý AI, Bảng tính size, Xem nhanh modal |
| **Giao diện Quản trị (Admin Console)** | **`http://localhost:4220/admin`** | Tách riêng App Shell, Quản lý sản phẩm, POS, Hóa đơn, 9 thuộc tính |
| **Backend REST API** | **`http://localhost:8188`** | Spring Boot 4.0.6 (Java 21) |
| **API Health Check** | **`http://localhost:8188/actuator/health`** | Trạng thái UP của hệ thống |
| **H2 In-Memory Database Console** | **`http://localhost:8188/h2-console`** | JDBC URL: `jdbc:h2:mem:newmen`, User: `sa`, Pass: *(để trống)* |

---

## 3. Tài khoản Quản trị & Nhân viên mẫu

Hệ thống đã nạp sẵn toàn bộ 32 bảng cơ sở dữ liệu mẫu:
- **Admin toàn hệ thống:** `admin@newmen.local` / `ChangeMe123!`
- **Nhân viên thu ngân POS:** `staff@newmen.local` / `ChangeMe123!`
- **Khách hàng thân thiết:** `long.tran@gmail.com` / `ChangeMe123!`

---

## 4. Các lệnh quản lý Docker thông dụng

- **Xem log thời gian thực của cả 2 dịch vụ:**
  ```bash
  docker compose logs -f
  ```

- **Xem riêng log Backend:**
  ```bash
  docker compose logs -f backend
  ```

- **Xem riêng log Frontend:**
  ```bash
  docker compose logs -f frontend
  ```

- **Dừng và tắt hệ thống:**
  ```bash
  docker compose down
  ```

- **Dừng và xóa sạch toàn bộ volume dữ liệu cũ để chạy lại từ đầu:**
  ```bash
  docker compose down -v
  docker compose up --build
  ```
