# NewMen Store REST API — Phân Hệ Backend (Spring Boot 4 / Java 21)

Phân hệ dịch vụ REST API phía máy chủ cho hệ thống bán lẻ và quản lý chuỗi cửa hàng thời trang nam **NewMen Store**. Được phát triển trên nền tảng **Java 21 LTS** và **Spring Boot 4.0.6**.

Mặc định khi chạy thử nghiệm, profile `dev` được kích hoạt và sử dụng cơ sở dữ liệu in-memory H2 với đầy đủ dữ liệu hạt giống (Seed data) cho 32 bảng, đi kèm tài khoản quản trị viên và nhân viên quầy có sẵn.

---

## 🚀 Khởi chạy cục bộ trên Windows

Mở PowerShell tại thư mục `backend/` và thực thi:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Maven Wrapper sẽ tự động sử dụng Maven cài sẵn trên máy hoặc bộ nhớ đệm Maven 3.9.16 cục bộ. Nếu chưa có, wrapper sẽ tự động tải phiên bản Maven chính thức.

- **Đường dẫn gốc REST API:** `http://localhost:8188/api/v1`
- **Kiểm tra trạng thái sức khỏe (Health Check):** `http://localhost:8188/actuator/health`
- **Tài khoản Quản trị viên mẫu:** `admin@newmen.local` / `ChangeMe123!`
- **Bảng điều khiển cơ sở dữ liệu H2 Console:** `http://localhost:8188/h2-console`
  - JDBC URL: `jdbc:h2:mem:newmen`
  - User Name: `sa`
  - Password: *(để trống)*
- **Thư mục lưu trữ hình ảnh tải lên:** Mặc định được lưu vào thư mục `./uploads` nằm cùng cấp với thư mục chạy của backend. Có thể tùy chỉnh đường dẫn bằng biến môi trường `NEWMEN_UPLOAD_DIR`.

> [!NOTE]
> Bảng điều khiển H2 Console chỉ hoạt động khi thuộc tính `spring.h2.console.enabled=true` (được bật ở profile `dev` và bị vô hiệu hóa khi chuyển sang profile `oracle`). Mật khẩu tài khoản admin mẫu chỉ dành riêng cho môi trường kiểm thử cục bộ; hãy thiết lập biến `NEWMEN_DEMO_ADMIN_PASSWORD` khi cần thay đổi.

---

## 🗄 Kết nối Cơ sở dữ liệu Oracle Database

Dự án đã tích hợp sẵn thư viện driver Oracle JDBC chính thức (`ojdbc11`) cùng tệp cấu hình mẫu `application-oracle.yml`.

Để kết nối với cơ sở dữ liệu Oracle thực tế (Oracle Database 19c, 21c hoặc 23c):

1. **Khởi tạo cấu trúc bảng:**
   Đăng nhập vào Oracle với quyền chủ sở hữu schema (schema owner) và chạy script khởi tạo:
   ```text
   backend/db/oracle-schema.sql
   ```
2. **Khai báo biến môi trường và khởi động:**
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'oracle'
   $env:ORACLE_JDBC_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
   $env:ORACLE_DB_USER = 'NEWMEN'
   $env:ORACLE_DB_PASSWORD = 'mat-khau-oracle-cua-ban'

   cd backend
   .\mvnw.cmd spring-boot:run
   ```

Tệp `db/oracle-schema.sql` định nghĩa chuẩn hóa cho Oracle 12c trở lên. Ở profile `oracle`, Hibernate sử dụng chế độ `ddl-auto: validate` để kiểm tra tính toàn vẹn của các bảng sẵn có mà không tự ý sửa đổi dữ liệu thực tế.

---

## ⚙ Bảng biến môi trường cấu hình

| Biến môi trường | Mục đích | Giá trị mặc định |
| :--- | :--- | :--- |
| `PORT` | Cổng HTTP lắng nghe của dịch vụ Backend | `8188` |
| `APP_CORS_ORIGINS` | Danh sách tên miền trình duyệt được phép truy cập (CORS) | `http://localhost:4220,http://127.0.0.1:4220` |
| `NEWMEN_UPLOAD_DIR` | Thư mục lưu trữ file ảnh sản phẩm trên ổ đĩa | `./uploads` |
| `NEWMEN_DEMO_ADMIN_EMAIL` | Email tài khoản quản trị mặc định (chỉ profile `dev`) | `admin@newmen.local` |
| `NEWMEN_DEMO_ADMIN_PASSWORD` | Mật khẩu tài khoản quản trị mặc định (chỉ profile `dev`) | `ChangeMe123!` |
| `ORACLE_JDBC_URL` | Chuỗi kết nối JDBC Oracle (profile `oracle`) | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `ORACLE_DB_USER` | Tên người dùng / schema Oracle | `NEWMEN` |
| `ORACLE_DB_PASSWORD` | Mật khẩu tài khoản Oracle | *(để trống, truyền qua biến môi trường)* |
| `SESSION_COOKIE_SECURE` | Đánh dấu cờ bảo mật Secure cho Cookie khi dùng HTTPS | `false` |
| `FREE_SHIPPING_THRESHOLD` | Giá trị đơn hàng tối thiểu để được miễn phí giao hàng (VNĐ) | `899000` |

Session cookie của hệ thống được bảo vệ với các cờ `HttpOnly` và `SameSite=Lax`. Các yêu cầu làm thay đổi trạng thái (POST, PUT, PATCH, DELETE) bắt buộc phải kèm theo mã token CSRF tương thích với Angular. Token được cập nhật tự động qua endpoint `GET /api/v1/auth/csrf`.

---

## 📦 Các phân hệ nghiệp vụ đã hoàn thiện

- **Tra cứu và danh mục sản phẩm (Public Catalog):** Tìm kiếm thông minh theo từ khóa không dấu, bộ lọc danh mục, màu sắc, kích cỡ, chất liệu, phân trang linh hoạt, sắp xếp đa chiều và danh sách khuyến mãi / voucher đang hoạt động.
- **Khách hàng & Tài khoản cá nhân:** Đăng ký thành viên, đăng nhập, đổi mật khẩu, quản lý hồ sơ và sổ địa chỉ giao hàng, lưu giỏ hàng theo phiên, đánh giá sản phẩm kèm ảnh và gửi yêu cầu đổi trả hàng.
- **Thanh toán an toàn (Checkout & Idempotency):** Khóa bản ghi tồn kho biến thể khi đặt hàng (Pessimistic Locking) để loại trừ hiện tượng bán vượt số lượng tồn kho (overselling); sử dụng khóa định danh duy nhất `Idempotency-Key` ngăn chặn tình trạng người dùng bấm thanh toán nhiều lần gây trùng đơn.
- **Bán hàng tại quầy (POS Counter Sales):** Quản lý đồng thời tối đa 5 giỏ hàng chờ trên cùng một phiên làm việc của nhân viên thu ngân; tra cứu khách hàng nhanh bằng số điện thoại; tìm kiếm sản phẩm và quét mã vạch barcode; tính toán chiết khấu tự động.
- **Quản trị vận hành (Admin Console):** Quản lý sản phẩm và ma trận biến thể, tải ảnh trực tiếp lên đĩa cứng máy chủ, quản lý 9 danh mục thuộc tính sản phẩm, nhân viên và phân quyền, chiến dịch khuyến mãi, duyệt đơn hàng và theo dõi lịch sử trạng thái đơn.
- **Báo cáo & Chứng từ:** Xuất danh sách sản phẩm ra tệp CSV (mở trực tiếp bằng Microsoft Excel) và tạo hóa đơn bán lẻ PDF chuyên nghiệp có thể in trực tiếp.
