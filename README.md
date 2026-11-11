# NewMen Store — Hệ Thống Quản Lý & Bán Hàng Thời Trang Nam (web-oracle)

Dự án phát triển nền tảng thương mại điện tử và quản lý bán hàng tại quầy (POS) cho thương hiệu thời trang nam cao cấp **NewMen Store**, xây dựng chuẩn hóa theo tài liệu thiết kế đồ án tốt nghiệp với đầy đủ **32 bảng cơ sở dữ liệu**.

Hệ thống được phát triển theo kiến trúc hiện đại, tách biệt hoàn toàn giữa **Frontend (Angular 22)** và **Backend (Spring Boot 4 / Java 21)**. Hỗ trợ chạy thử nghiệm nhanh với cơ sở dữ liệu in-memory H2 hoặc kết nối trực tiếp với hệ quản trị cơ sở dữ liệu doanh nghiệp **Oracle Database (19c/21c/23c)**.

---

## 📑 Mục lục

1. [Kiến trúc & Công nghệ sử dụng](#-kiến-trúc--công-nghệ-sử-dụng)
2. [Cấu trúc thư mục dự án](#-cấu-trúc-thư-mục-dự-án)
3. [Cách 1: Khởi động 1-Click bằng Docker (Khuyên dùng)](#-cách-1-khởi-động-1-click-bằng-docker-khuyên-dùng)
4. [Cách 2: Chạy cục bộ bằng mã nguồn (Local Development)](#-cách-2-chạy-cục-bộ-bằng-mã-nguồn-local-development)
5. [Cấu hình kết nối Cơ sở dữ liệu Oracle](#-cấu-hình-kết-nối-cơ-sở-dữ-liệu-oracle)
6. [Tài khoản demo & Dữ liệu mẫu (Seed Data)](#-tài-khoản-demo--dữ-liệu-mẫu-seed-data)
7. [Các phân hệ và chức năng chính](#-các-phân-hệ-và-chức-năng-chính)
8. [Bảng biến môi trường hệ thống](#-bảng-biến-môi-trường-hệ-thống)
9. [Tài liệu kỹ thuật bổ sung](#-tài-liệu-kỹ-thuật-bổ-sung)

---

## 🏗 Kiến trúc & Công nghệ sử dụng

- **Frontend:**
  - Framework: [Angular 22](https://angular.dev/) (Standalone Components, Signals, Zoneless reactivity)
  - Ngôn ngữ: TypeScript 5.8
  - Web Server production: Nginx Alpine (Reverse proxy, Gzip compression)
  - Giao diện: CSS thuần tùy biến cao cấp, Responsive chuẩn mực từ màn hình điện thoại, máy tính bảng đến laptop (1366×768) và màn hình lớn.

- **Backend:**
  - Framework: [Spring Boot 4.0.6](https://spring.io/projects/spring-boot) / [Spring Framework 7](https://spring.io/)
  - Nền tảng: Java 21 LTS
  - Quản lý phụ thuộc: Apache Maven (đi kèm Maven Wrapper `mvnw.cmd` / `mvnw`)
  - Bảo mật: Spring Security (Phiên làm việc Stateful Session qua HttpOnly Cookie, CSRF Token đồng bộ Angular, mã hóa mật khẩu BCrypt, phân quyền Role-based: `ADMIN`, `STAFF`, `USER`)
  - Báo cáo & Hóa đơn: Xuất hóa đơn bán hàng chuẩn PDF (OpenPDF) và xuất danh sách sản phẩm/đơn hàng định dạng CSV (mở trực tiếp bằng Microsoft Excel)

- **Cơ sở dữ liệu:**
  - Môi trường phát triển (`dev`): H2 In-Memory Database (tự động nạp dữ liệu mẫu 32 bảng, đi kèm bảng điều khiển H2 Console).
  - Môi trường doanh nghiệp (`oracle`): Oracle Database (driver `ojdbc11`, xác thực schema qua `oracle-schema.sql`).

---

## 📁 Cấu trúc thư mục dự án

```text
├── backend/                        # Mã nguồn Spring Boot REST API
│   ├── db/
│   │   └── oracle-schema.sql       # Script DDL khởi tạo 32 bảng cho Oracle Database
│   ├── media/                      # Thư viện ảnh sản phẩm ban đầu
│   ├── src/main/java/vn/newmen/    # Mã nguồn Java (Domain, API Controllers, Repositories, Services)
│   ├── src/main/resources/         # Cấu hình Spring Boot (application.yml, application-oracle.yml,...)
│   ├── Dockerfile                  # Multi-stage Dockerfile cho Backend
│   ├── pom.xml                     # Cấu hình Maven dependencies
│   └── README.md                   # Hướng dẫn chi tiết phân hệ Backend
│
├── frontend/                       # Mã nguồn Angular Storefront & Admin
│   ├── src/app/
│   │   ├── core/                   # Models, Enums, StoreApiService kết nối REST API
│   │   ├── pages/                  # Các màn hình chính (Admin, POS, Storefront, Cart, Account, Product)
│   │   └── app.routes.ts           # Cấu hình định tuyến Angular
│   ├── Dockerfile                  # Multi-stage Dockerfile cho Frontend (Nginx)
│   ├── nginx.conf                  # Cấu hình máy chủ web Nginx & reverse proxy API
│   └── package.json                # Thư viện và kịch bản npm
│
├── docs/                           # Tài liệu thiết kế & phân tích hệ thống
│   ├── requirements.md             # Đặc tả yêu cầu người dùng và nghiệp vụ chuẩn hóa
│   ├── api-contract.md             # Hợp đồng REST API v1 giữa Frontend và Backend
│   └── admin-gap-plan.md           # Kế hoạch ánh xạ dữ liệu theo tài liệu đồ án tốt nghiệp
│
├── docker-compose.yml              # Kịch bản khởi chạy đồng thời Frontend và Backend bằng Docker
├── README-DOCKER.md                # Cẩm nang chi tiết triển khai hệ thống bằng Docker
└── README.md                       # Tài liệu tổng quan dự án (File hiện tại)
```

---

## 🐳 Cách 1: Khởi động 1-Click bằng Docker (Khuyên dùng)

> [!TIP]
> Phương pháp này **không yêu cầu** máy tính của bạn phải cài Java, Maven hay Node.js. Chỉ cần cài sẵn [Docker Desktop](https://www.docker.com/products/docker-desktop/).

1. Mở PowerShell hoặc Terminal tại thư mục gốc của dự án:
   ```bash
   docker compose up --build
   ```
2. Sau khi build hoàn tất, mở trình duyệt để sử dụng:
   - **Giao diện Khách hàng (Storefront):** `http://localhost:4220`
   - **Giao diện Quản trị & Bán hàng tại quầy (Admin/POS):** `http://localhost:4220/admin`
   - **Backend REST API:** `http://localhost:8188/api/v1`
   - **Kiểm tra trạng thái hệ thống (Health Check):** `http://localhost:8188/actuator/health`

*(Xem chi tiết tại [README-DOCKER.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/README-DOCKER.md))*

---

## 💻 Cách 2: Chạy cục bộ bằng mã nguồn (Local Development)

### Yêu cầu môi trường
- **Java:** JDK 21 trở lên
- **Node.js:** Phiên bản 20.x hoặc 22.x
- **Trình quản lý gói:** npm 10.x trở lên

### Bước 1: Khởi động Backend Spring Boot
Từ thư mục gốc dự án:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Backend sẽ khởi chạy tại cổng `http://localhost:8188` với profile mặc định là `dev` (H2 Database in-memory, tự động nạp dữ liệu mẫu).

- Bảng điều khiển cơ sở dữ liệu H2: `http://localhost:8188/h2-console`
  - JDBC URL: `jdbc:h2:mem:newmen`
  - User Name: `sa`
  - Password: *(để trống)*

### Bước 2: Khởi động Frontend Angular
Mở một cửa sổ PowerShell mới tại thư mục gốc:
```powershell
cd frontend
npm install
npm run start
```
Frontend sẽ khởi chạy tại `http://localhost:4220`. Proxy phát triển đã được cấu hình tự động điều hướng tất cả các yêu cầu `/api` sang cổng backend `8188`.

---

## 🗄 Cấu hình kết nối Cơ sở dữ liệu Oracle

Để kết nối với một máy chủ Oracle Database thực tế thay vì dùng H2 in-memory:

1. **Khởi tạo bảng trên Oracle:**
   Sử dụng công cụ quản trị (SQL Developer, DBeaver, hoặc SQL*Plus), đăng nhập bằng tài khoản người dùng Oracle (schema owner) và chạy toàn bộ tệp lệnh:
   ```text
   backend/db/oracle-schema.sql
   ```
   Tệp script này sẽ tự động tạo đủ 32 bảng, các khóa ngoại (Foreign Keys), ràng buộc toàn vẹn và chỉ mục (Indexes).

2. **Khai báo biến môi trường và chạy Backend:**
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "oracle"
   $env:ORACLE_JDBC_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
   $env:ORACLE_DB_USER = "NEWMEN"
   $env:ORACLE_DB_PASSWORD = "mat-khau-oracle-cua-ban"

   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   > [!IMPORTANT]
   > Profile `oracle` sử dụng cơ chế `hibernate.ddl-auto: validate` để đảm bảo an toàn dữ liệu, không tự ý thay đổi cấu trúc bảng thực tế.

---

## 🔑 Tài khoản demo & Dữ liệu mẫu (Seed Data)

Khi khởi động ở profile `dev` hoặc qua Docker, hệ thống tự động cung cấp các tài khoản thử nghiệm:

| Vai trò | Email đăng nhập | Mật khẩu mặc định | Quyền hạn và phạm vi sử dụng |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@newmen.local` | `ChangeMe123!` | Toàn quyền quản trị hệ thống, quản lý sản phẩm, nhân viên, voucher, xuất báo cáo |
| **Nhân viên thu ngân (Staff)** | `staff@newmen.local` | `ChangeMe123!` | Bán hàng tại quầy (POS), tạo và xử lý đơn hàng, tra cứu tồn kho sản phẩm |
| **Khách hàng thân thiết** | `long.tran@gmail.com` | `ChangeMe123!` | Xem lịch sử mua sắm, quản lý sổ địa chỉ giao hàng, theo dõi đơn trực tuyến |

*(Lưu ý: Có thể thay đổi mật khẩu quản trị viên mặc định qua biến môi trường `NEWMEN_DEMO_ADMIN_PASSWORD`).*

---

## 🚀 Các phân hệ và chức năng chính

### 1. Phân hệ Bán hàng Trực tuyến (Storefront)
- **Danh mục thời trang nam:** Áo sơ mi công sở, Áo Polo, Áo vest & Blazer, Áo khoác gió, Áo len dệt kim, Áo thun cotton,...
- **Bộ lọc đa tiêu chí:** Lọc tức thì theo danh mục, màu sắc, kích cỡ, khoảng giá; sắp xếp theo mới nhất, bán chạy nhất, giá tăng/giảm.
- **Trợ lý AI Stylist:** Gợi ý trang phục thông minh theo mệnh phong thủy (Kim, Mộc, Thủy, Hỏa, Thổ) và dịp sử dụng (Công sở, Dạo phố, Tiệc tối, Thể thao).
- **Bảng tính chọn size chuẩn:** Tự động tính toán và khuyến nghị kích cỡ áo (S, M, L, XL, XXL) theo chiều cao và cân nặng của khách hàng.
- **Giỏ hàng & Đặt hàng linh hoạt:** Hỗ trợ mua hàng dạng khách vãng lai hoặc đăng ký thành viên; áp dụng mã giảm giá (Voucher); chọn hình thức thanh toán COD hoặc Chuyển khoản QR.

### 2. Phân hệ Quản trị Vận hành (Admin Console)
- **Bảng điều khiển & Thống kê kinh doanh:** Biểu đồ doanh thu trực quan theo Ngày, Tuần, Tháng, Năm; theo dõi tỉ lệ đơn hoàn tất, sản lượng xuất kho và lượng khách hàng mới.
- **Quản lý Sản phẩm & Ma trận Biến thể:**
  - Nhập thông tin áo, giá vốn, giá bán, trọng lượng, chất liệu, kiểu dáng.
  - Ma trận tự động tạo biến thể (Variants) tổ hợp nhanh chóng giữa bảng Màu sắc và Kích cỡ.
  - Tải lên ảnh sản phẩm trực tiếp từ máy tính lên máy chủ cục bộ.
  - Xuất danh sách toàn bộ sản phẩm ra file CSV chuẩn Excel.
- **Bán hàng tại quầy (POS - Counter Sales):**
  - Quản lý đồng thời tối đa 5 hóa đơn chờ (Multi-draft carts).
  - Tìm kiếm sản phẩm thông minh không dấu tiếng Việt; quét mã barcode bằng máy quét hoặc camera.
  - Bổ sung Catalog sản phẩm trực quan: cho phép bấm chọn nhanh biến thể để thêm ngay vào đơn mà không bắt buộc phải có máy quét mã.
  - Hỗ trợ chọn khách hàng thành viên có sẵn hoặc tạo nhanh khách hàng mới ngay tại quầy.
  - Thiết kế Responsive hoàn hảo trên mọi độ phân giải màn hình laptop (1366×768, 1280×800) không bị co ép hay xuất hiện thanh cuộn ngang.
- **Quản lý Đơn hàng & Xuất hóa đơn PDF:**
  - Quy trình vòng đời đơn hàng: Chờ xác nhận ➔ Đã xác nhận ➔ Đang giao ➔ Hoàn thành ➔ Đã hủy.
  - Xem chi tiết từng dòng đơn hàng, thông tin giao hàng, lịch sử thay đổi trạng thái.
  - Nút xuất và in **Hóa đơn bán hàng PDF (Printable PDF Invoice)** sắc nét, chuyên nghiệp.
- **Quản lý Thuộc tính Sản phẩm (9 Danh mục chuẩn):**
  - Danh mục gốc (Origin Categories), Danh mục con (Categories), Màu sắc (Colors), Kích cỡ (Sizes), Chất liệu (Materials), Cổ áo (Collars), Kiểu thiết kế (Designs), Phom dáng (Forms), Tay áo (Sleeves).
- **Quản lý Khuyến mãi, Voucher & Sản phẩm lỗi/hủy:**
  - Tạo chiến dịch giảm giá theo phần trăm hoặc số tiền cố định; đặt giới hạn lượt dùng và thời hạn hiệu lực.
  - Quy trình ghi nhận sản phẩm lỗi/hủy (Fault Products) và cập nhật biến động kho hàng (Inventory Movement).

---

## ⚙ Bảng biến môi trường hệ thống

| Biến môi trường | Ý nghĩa và mục đích | Giá trị mặc định |
| :--- | :--- | :--- |
| `PORT` | Cổng mạng của dịch vụ Backend | `8188` |
| `SPRING_PROFILES_ACTIVE` | Profile cấu hình Spring Boot (`dev` hoặc `oracle`) | `dev` |
| `APP_CORS_ORIGINS` | Danh sách tên miền trình duyệt được phép gọi API (CORS) | `http://localhost:4220,http://127.0.0.1:4220` |
| `NEWMEN_UPLOAD_DIR` | Thư mục lưu trữ hình ảnh sản phẩm tải lên trên đĩa cứng | `./uploads` |
| `NEWMEN_DEMO_ADMIN_EMAIL` | Email của tài khoản Admin mẫu | `admin@newmen.local` |
| `NEWMEN_DEMO_ADMIN_PASSWORD` | Mật khẩu tài khoản Admin mẫu | `ChangeMe123!` |
| `ORACLE_JDBC_URL` | Đường dẫn kết nối JDBC Oracle | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `ORACLE_DB_USER` | Tên người dùng / schema Oracle | `NEWMEN` |
| `ORACLE_DB_PASSWORD` | Mật khẩu tài khoản Oracle | *(để trống)* |
| `FREE_SHIPPING_THRESHOLD` | Hạn mức giá trị đơn hàng được miễn phí vận chuyển (VNĐ) | `899000` |

---

## 📚 Tài liệu kỹ thuật bổ sung

- [README-DOCKER.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/README-DOCKER.md): Hướng dẫn chi tiết vận hành hệ thống bằng Docker container.
- [backend/README.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/backend/README.md): Hướng dẫn chuyên sâu cấu trúc mã nguồn Backend Spring Boot.
- [docs/requirements.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/docs/requirements.md): Đặc tả yêu cầu người dùng và các nghiệp vụ bán hàng chuẩn hóa.
- [docs/api-contract.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/docs/api-contract.md): Hợp đồng giao tiếp dữ liệu REST API v1.
- [docs/admin-gap-plan.md](file:///c:/Users/ssvn2/Documents/Codex/2026-11-09/d-a-v-o-t-i-3/docs/admin-gap-plan.md): Bảng đối soát cấu trúc 32 bảng và lược đồ ERD theo đồ án tốt nghiệp.
