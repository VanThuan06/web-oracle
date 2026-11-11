# Đặc Tả Yêu Cầu Nghiệp Vụ & Giả Định Hệ Thống NewMen Store

Hệ thống được xây dựng bám sát quy trình nghiệp vụ bán hàng thời trang nam dựa trên tài liệu thiết kế đồ án tốt nghiệp, chuẩn hóa thành mô hình quản lý bán lẻ đa kênh (Online Storefront & Offline POS Counter).

---

## 1. Phân hệ Khách hàng (Customer Storefront)

- **Xem và tìm kiếm sản phẩm:** Khách hàng có thể duyệt danh mục, tìm kiếm theo tên áo hoặc mã, lọc theo màu sắc, kích cỡ, chất liệu, khoảng giá và phân trang linh hoạt; kiểm tra tức thời số lượng tồn kho theo từng biến thể màu/size.
- **Giỏ hàng & Đặt hàng:** Thêm sản phẩm biến thể vào giỏ hàng, cập nhật số lượng, áp dụng mã giảm giá (Voucher), nhập thông tin người nhận và địa chỉ giao hàng, lựa chọn hình thức thanh toán (COD hoặc Chuyển khoản mẫu).
- **Quản lý tài khoản cá nhân:** Đăng ký, đăng nhập tài khoản, quản lý thông tin cá nhân và sổ địa chỉ giao hàng nhận hàng; theo dõi lịch sử đơn hàng và dòng thời gian trạng thái giao hàng; gửi đánh giá và nhận xét sản phẩm.
- **Tính năng thông minh bổ sung:** Trợ lý AI gợi ý trang phục theo phong thủy và dịp sử dụng; bảng tính kích cỡ áo chuẩn (Size Calculator) theo chiều cao và cân nặng.

---

## 2. Phân hệ Nhân viên & Quản trị viên (Staff & Administrator)

- **Quản lý danh mục & Ma trận sản phẩm:** Thêm mới, chỉnh sửa sản phẩm, tự động tạo ma trận biến thể từ bảng Màu sắc và Kích cỡ, thiết lập mã vạch barcode, giá vốn, giá bán, trọng lượng và trạng thái kinh doanh; tải lên ảnh đại diện sản phẩm trực tiếp từ máy tính.
- **Bán hàng tại quầy (POS - Point of Sale):**
  - Mở đồng thời tối đa 5 hóa đơn chờ (draft bills) giúp phục vụ nhiều khách hàng cùng lúc mà không bị mất dữ liệu.
  - Tra cứu khách hàng theo số điện thoại hoặc thêm nhanh khách hàng mới ngay tại quầy.
  - Tìm kiếm sản phẩm thông minh không dấu tiếng Việt; quét mã vạch bằng đầu đọc hoặc camera.
  - Tích hợp danh mục chọn nhanh sản phẩm trực quan, thu ngân có thể bấm trực tiếp vào biến thể để thêm ngay vào giỏ hàng.
- **Xử lý đơn hàng & Vòng đời đơn:**
  - Tra cứu và lọc đơn hàng theo trạng thái, mã đơn, khách hàng, số điện thoại, kênh bán (Online/POS) và khoảng thời gian.
  - Cập nhật trạng thái đơn qua từng giai đoạn và lưu vết lịch sử (Audit trail).
  - Xuất hóa đơn bán lẻ PDF chuyên nghiệp có thể in ngay cho khách hàng.
- **Quản lý 9 bảng thuộc tính sản phẩm:**
  - Danh mục gốc, Danh mục con, Màu sắc, Kích cỡ, Chất liệu, Cổ áo, Kiểu thiết kế, Phom dáng, Tay áo.
- **Quản lý khuyến mãi, nhân viên & Khách hàng:**
  - Thiết lập chiến dịch giảm giá theo thời gian và tạo mã voucher chiết khấu.
  - Phân quyền nhân viên (`ADMIN`, `STAFF`); quản lý danh sách khách hàng thân thiết.
- **Báo cáo kinh doanh & Xuất dữ liệu:**
  - Biểu đồ thống kê doanh thu theo ngày, tuần, tháng, năm; tỷ lệ đơn thành công, sản phẩm bán chạy.
  - Xuất danh sách sản phẩm và đơn hàng định dạng CSV tương thích 100% với Microsoft Excel.

---

## 3. Các điểm chuẩn hóa so với tài liệu gốc

- **Cơ sở dữ liệu 32 bảng chuẩn hóa:** Mô hình dữ liệu được tổ chức chuẩn hóa gồm các bảng: tài khoản, phân quyền, hồ sơ nhân viên, khách hàng, địa chỉ, liên hệ, sản phẩm, biến thể, hình ảnh sản phẩm, danh mục gốc, danh mục con, 7 bảng thuộc tính thời trang, giỏ hàng, dòng giỏ hàng, đơn hàng, dòng đơn hàng, lịch sử trạng thái, sản phẩm lỗi/hủy, biến động kho, khuyến mãi, giảm giá, voucher, đánh giá, ảnh đánh giá, đổi hàng, trả hàng,...
- **Biến thể (ProductVariant) là đơn vị quản lý tồn kho (SKU):** Mỗi sự kết hợp giữa Màu sắc và Kích cỡ của một sản phẩm là một đơn vị lưu kho riêng biệt có barcode và số lượng tồn độc lập.
- **Đóng băng giá bán tại thời điểm đặt hàng:** Giá bán trên từng dòng hóa đơn (`order_lines`) là giá chốt tại thời điểm giao dịch, không bị ảnh hưởng khi giá sản phẩm gốc thay đổi trong tương lai.
- **Thanh toán an toàn & Khóa tồn kho:** Áp dụng khóa bi quan (Pessimistic Locking) trong giao dịch cơ sở dữ liệu để loại bỏ hoàn toàn nguy cơ âm kho khi nhiều người cùng đặt mua 1 chiếc áo cuối cùng.
- **Ngôn ngữ và tiền tệ:** Toàn bộ giao diện người dùng sử dụng Tiếng Việt chuẩn mực, đơn vị tiền tệ Việt Nam Đồng (VNĐ) và định dạng ngày giờ chuẩn Việt Nam.
