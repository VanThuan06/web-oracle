# Kế Hoạch Chuẩn Hóa Cơ Sở Dữ Liệu & Phân Hệ Quản Trị Theo Đồ Án Tốt Nghiệp

Tài liệu đối soát và hoàn thiện hệ thống NewMen Store dựa trên tập tài liệu tốt nghiệp được cung cấp: từ điển dữ liệu các bảng 2.12 – 2.41 (trang 74 – 88), lược đồ quan hệ thực thể ERD Hình 2.31 (trang 89), yêu cầu người dùng bảng 1.8 & 2.1, biểu đồ use case 2.5 và giao diện quản trị Hình 3.1 (trang 90 – 95).

---

## 1. Ánh xạ từ điển dữ liệu sang mô hình thực thể chuẩn

Tài liệu đồ án gốc định nghĩa 30 bảng trong từ điển dữ liệu và 2 bảng bổ sung trên lược đồ ERD (tổng cộng 32 bảng). Toàn bộ các khái niệm này được chuẩn hóa nhất quán thành các thực thể JPA trong mã nguồn và schema Oracle:

| Bảng / Khái niệm trong đồ án | Thực thể đích trong hệ thống | Diễn giải & Công việc hiện thực |
| :--- | :--- | :--- |
| **Employee, Customer** | `accounts`, `roles`, `employees`, `customers` | Tách biệt thông tin tài khoản đăng nhập (`accounts`) và thông tin hồ sơ nhân viên (`employees`), khách hàng (`customers`). |
| **Customer Address, Contact** | `addresses`, `contacts` | Quản lý sổ địa chỉ giao hàng và thông tin liên hệ phản hồi của khách hàng. |
| **Invoice/Order, OrderDetail** | `customer_orders`, `order_lines`, `order_status_history` | Đơn hàng, dòng chi tiết sản phẩm và theo dõi lịch sử luân chuyển trạng thái đơn hàng. |
| **Color, Design, Collar, Form, Material, Sleeve, Size** | `colors`, `designs`, `collars`, `forms`, `materials`, `sleeves`, `sizes` | 7 bảng danh mục thuộc tính thời trang chuẩn hóa độc lập, liên kết với sản phẩm và biến thể. |
| **Origin Category, Category** | `origin_categories`, `categories` | Mô hình phân cấp 2 tầng: Danh mục gốc (Cha) và Danh mục chi tiết (Con). |
| **Product, ProductDetail, Product Image** | `products`, `product_variants`, `product_images` | Quản lý thông tin áo, ma trận biến thể SKU (Màu sắc × Kích cỡ), barcode và thư viện ảnh sản phẩm. |
| **Banner** | `banners` | Quản lý hình ảnh và liên kết banner quảng cáo trên trang chủ storefront. |
| **Fault Product** | `fault_products`, `inventory_movements` | Ghi nhận biên bản sản phẩm lỗi/hỏng/hủy và theo dõi dòng luân chuyển xuất nhập kho. |
| **Rating, Rating Image** | `reviews`, `rating_images` | Tiếp nhận và kiểm duyệt đánh giá, nhận xét của khách hàng kèm hình ảnh thực tế. |
| **Role** | `roles` | Bảng phân quyền hệ thống (`ADMIN`, `STAFF`, `USER`). |
| **Discount, ProductDiscount** | `discounts`, `product_discounts` | Chiến dịch giảm giá trực tiếp theo thời gian và danh mục sản phẩm áp dụng. |
| **Voucher** | `vouchers` | Quản lý mã giảm giá khách hàng nhập tại bước thanh toán (độc lập với chiến dịch giảm giá). |
| **Return, ReturnDetail** | `order_returns`, `return_details` | Quy trình tiếp nhận và xử lý yêu cầu trả hàng từ phía người mua. |
| **Checkout, CheckoutDetail** | `checkouts`, `checkout_details` | Lưu vết phiên thanh toán và chi tiết giỏ hàng tại thời điểm chuyển đổi sang đơn hàng. |
| **Exchange, ExchangeDetail** | `exchanges`, `exchange_details` | Tiếp nhận và xử lý quy trình đổi hàng (kích cỡ, màu sắc) phát sinh tại quầy hoặc trực tuyến. |

---

## 2. Thiết kế phân hệ Quản trị (Admin Console) độc lập

Tuyến đường `/admin` hoạt động như một ứng dụng quản trị doanh nghiệp chuyên biệt (Single Page Application Console):
- Sử dụng App Shell riêng biệt: Thanh đầu trang (Topbar) gọn gàng, thanh điều hướng chính (Navigation Bar) phẳng không dùng emoji rườm rà, vùng làm việc rộng rãi.
- Không kế thừa phần banner thông báo bán lẻ, menu cửa hàng hay chân trang (footer) của giao diện khách hàng.
- Bảng màu thống nhất: Xanh hải quân đậm (`#0f2652`), xanh điểm nhấn (`#38bdf8`) và nền xám trung tính hiện đại (`#f8fafc`).

### Các phân hệ quản trị chính:
1. **Bảng điều khiển (Overview Dashboard):** 4 thẻ chỉ số KPI doanh thu, số đơn, sản lượng, khách hàng mới; biểu đồ cột trực quan theo Ngày/Tuần/Tháng/Năm.
2. **Quản lý Sản phẩm & Biến thể:** Bảng danh sách sản phẩm có thể mở rộng xem từng biến thể, tự động tạo ma trận biến thể, tải ảnh trực tiếp, xuất dữ liệu CSV.
3. **Bán hàng tại quầy (POS):** Quản lý 5 hóa đơn chờ, tích hợp catalog chọn sản phẩm trực quan, tìm kiếm tiếng Việt không dấu, responsive trên mọi kích thước màn hình.
4. **Quản lý Đơn hàng & Xuất hóa đơn:** Vòng đời 5 bước, xem chi tiết giao dịch, xuất hóa đơn PDF in ấn trực tiếp.
5. **Quản lý Thuộc tính:** CRUD đầy đủ cho 9 danh mục thuộc tính sản phẩm.
6. **Khuyến mãi & Vouchers:** Quản lý giảm giá và mã voucher.
7. **Khách hàng, Nhân viên & Banners:** Quản lý danh sách thành viên, nhân sự và hình ảnh quảng bá.
