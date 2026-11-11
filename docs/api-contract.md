# Hợp Đồng Giao Tiếp Dữ Liệu REST API (Phiên bản v1)

Đường dẫn gốc: `/api/v1`.  
Các trường JSON sử dụng quy ước đặt tên `camelCase`.  
Các API danh sách phân trang trả về cấu trúc: `{ data, page, size, totalItems, totalPages }`.  
Cấu trúc phản hồi lỗi chuẩn: `{ error: { code, message, details? } }`.

---

## 1. Phân hệ Bán hàng Trực tuyến (Public Storefront)

- `GET /catalog/products?query=&categoryId=&color=&clothingSize=&sort=&page=0&pageSize=12`: Lấy danh sách sản phẩm với bộ lọc từ khóa, danh mục, màu sắc, size, và sắp xếp (`newest`, `bestseller`, `price-asc`, `price-desc`).
- `GET /catalog/products/{slug}`: Chi tiết sản phẩm kèm danh sách biến thể màu/size và hình ảnh.
- `GET /catalog/categories`: Danh mục sản phẩm công khai.
- `GET /catalog/recommendations?productId=&limit=8`: Gợi ý sản phẩm liên quan.
- `GET /catalog/banners`: Danh sách banner quảng cáo đang hiển thị.
- `GET /catalog/attributes`: Bảng tra cứu các thuộc tính thời trang (màu sắc, size, chất liệu, cổ áo, tay áo,...).
- `POST /catalog/contacts`: Tiếp nhận ý kiến phản hồi và liên hệ từ khách hàng.
- `GET /promotions/active`: Danh sách các chương trình khuyến mãi và voucher đang có hiệu lực.

---

## 2. Phiên làm việc, Giỏ hàng & Khách hàng (Session & Customer)

- `GET /auth/csrf`: Lấy mã token bảo mật CSRF cho Angular.
- `POST /auth/register`: Đăng ký tài khoản khách hàng mới.
- `POST /auth/login`: Đăng nhập hệ thống (Quản trị viên, Nhân viên, Khách hàng).
- `POST /auth/logout`: Đăng xuất và hủy phiên làm việc.
- `GET /auth/me`: Kiểm tra thông tin người dùng đang đăng nhập trong phiên hiện tại.
- `GET /cart`: Lấy thông tin giỏ hàng hiện tại của khách hàng.
- `POST /cart/items`: Thêm sản phẩm biến thể vào giỏ hàng.
- `PATCH /cart/items/{variantId}`: Cập nhật số lượng của một biến thể trong giỏ hàng.
- `DELETE /cart/items/{variantId}`: Xóa một biến thể khỏi giỏ hàng.
- `DELETE /cart/items`: Xóa sạch toàn bộ giỏ hàng.
- `POST /checkout`: Đặt hàng và thanh toán. Yêu cầu kèm header `Idempotency-Key` để chống trùng đơn; tiếp nhận địa chỉ giao hàng, thông tin khách vãng lai hoặc tài khoản thành viên, và phương thức thanh toán (`COD`, `BANK_TRANSFER_SAMPLE`).
- `GET /orders/mine`: Danh sách lịch sử các đơn hàng của khách hàng hiện tại.
- `GET /orders/{id}`: Chi tiết một đơn hàng cụ thể kèm dòng thời gian trạng thái.
- `POST /orders/{id}/reviews`: Gửi đánh giá sao và nhận xét cho các sản phẩm đã mua trong đơn.
- `POST /orders/{id}/returns`: Gửi yêu cầu đổi trả hàng (bắt buộc nhập lý do).
- `GET /profile`: Lấy thông tin tài khoản cá nhân.
- `PATCH /profile`: Cập nhật thông tin tài khoản cá nhân.
- `GET /profile/addresses`: Danh sách sổ địa chỉ nhận hàng của khách hàng.
- `POST /profile/addresses`: Thêm mới địa chỉ nhận hàng.
- `PATCH /profile/addresses/{id}`: Cập nhật địa chỉ nhận hàng.
- `DELETE /profile/addresses/{id}`: Xóa địa chỉ nhận hàng.

---

## 3. Phân hệ Quản trị Vận hành (Staff & Admin)

### 3.1. Quản lý Sản phẩm & Biến thể (Hình 3.1 & 3.2 trong đồ án)
- `GET /admin/products`: Danh sách sản phẩm quản trị (kèm biến thể, tồn kho và bộ lọc).
- `POST /admin/products`: Thêm mới sản phẩm.
- `GET /admin/products/{id}`: Chi tiết thông tin sản phẩm và danh sách SKU biến thể.
- `PATCH /admin/products/{id}`: Cập nhật thông tin sản phẩm.
- `DELETE /admin/products/{id}`: Xóa hoặc ngừng kinh doanh sản phẩm.
- `POST /admin/products/generate-variants`: Tạo tự động ma trận biến thể từ danh sách màu sắc và kích cỡ được chọn.
- `GET /admin/products/export`: Xuất dữ liệu toàn bộ sản phẩm và biến thể ra tệp CSV (Excel).
- `POST /admin/products/{id}/images`: Tải lên hình ảnh đại diện sản phẩm (Multipart form data).
- `DELETE /admin/products/{id}/images/{imageId}`: Xóa hình ảnh sản phẩm.
- `POST /admin/products/{id}/variants`: Thêm một biến thể thủ công cho sản phẩm.
- `PATCH /admin/variants/{id}`: Cập nhật mã vạch, giá bán, số lượng tồn kho của biến thể.
- `DELETE /admin/variants/{id}`: Xóa một biến thể sản phẩm.

### 3.2. Quản lý 9 Danh mục Thuộc tính Sản phẩm (Bảng 2.16 - 2.24)
- `GET /admin/attributes/{type}`: Lấy danh sách thuộc tính theo loại (`origin-categories`, `categories`, `colors`, `sizes`, `materials`, `collars`, `designs`, `forms`, `sleeves`).
- `POST /admin/attributes/{type}`: Thêm mới một giá trị thuộc tính.
- `PATCH /admin/attributes/{type}/{id}`: Chỉnh sửa giá trị thuộc tính.
- `DELETE /admin/attributes/{type}/{id}`: Xóa giá trị thuộc tính.

### 3.3. Kho hàng & Xử lý Sản phẩm lỗi/hủy (Bảng 2.26)
- `GET /admin/fault-products`: Danh sách ghi nhận các sản phẩm bị lỗi, hỏng hoặc hủy bỏ.
- `POST /admin/fault-products`: Lập biên bản xuất hủy sản phẩm lỗi kèm số lượng và lý do.
- `PATCH /admin/fault-products/{id}/status`: Cập nhật trạng thái xử lý biên bản lỗi/hủy.

### 3.4. Chiến dịch Khuyến mãi & Mã giảm giá Voucher (Bảng 2.33 - 2.36)
- `GET /admin/discounts`: Danh sách các chương trình giảm giá theo thời gian.
- `POST /admin/discounts`: Tạo chiến dịch giảm giá mới.
- `PATCH /admin/discounts/{id}`: Cập nhật thông tin chiến dịch giảm giá.
- `GET /admin/vouchers`: Danh sách mã giảm giá (Vouchers).
- `POST /admin/vouchers`: Tạo mã voucher mới kèm điều kiện áp dụng.
- `PATCH /admin/vouchers/{id}`: Bật/tắt trạng thái hoặc cập nhật mã voucher.

### 3.5. Đơn hàng, Hóa đơn & Đổi trả (Bảng 2.15, 2.37 - 2.39, ERD 2.31)
- `GET /admin/orders`: Danh sách tất cả đơn hàng (Online và POS) kèm bộ lọc đa tiêu chí.
- `GET /admin/orders/{id}`: Chi tiết đơn hàng, người mua, danh sách sản phẩm và lịch sử chuyển trạng thái.
- `PATCH /admin/orders/{id}/status`: Cập nhật trạng thái đơn hàng (Chờ xác nhận, Đã duyệt, Đang giao, Đã hoàn tất, Đã hủy).
- `GET /admin/orders/{id}/invoice`: Xuất hóa đơn bán lẻ định dạng PDF sẵn sàng in ấn.
- `GET /admin/returns`: Danh sách yêu cầu trả hàng từ khách hàng.
- `PATCH /admin/returns/{id}/status`: Duyệt hoặc từ chối yêu cầu trả hàng.
- `GET /admin/exchanges`: Danh sách yêu cầu đổi hàng.
- `POST /admin/exchanges`: Tiếp nhận xử lý yêu cầu đổi hàng tại quầy.
- `PATCH /admin/exchanges/{id}/status`: Cập nhật trạng thái quy trình đổi hàng.

### 3.6. Bán hàng tại quầy (POS Counter Sales - Hình 3.3 & 3.4)
- `GET /admin/pos/carts`: Lấy danh sách tối đa 5 hóa đơn chờ của thu ngân.
- `POST /admin/pos/carts`: Mở thêm một hóa đơn chờ mới.
- `DELETE /admin/pos/carts/{cartId}`: Đóng một hóa đơn chờ.
- `POST /admin/pos/carts/{cartId}/items`: Thêm sản phẩm vào hóa đơn (qua quét mã barcode hoặc click chọn biến thể).
- `PATCH /admin/pos/carts/{cartId}/items/{variantId}`: Thay đổi số lượng mua của sản phẩm trong hóa đơn.
- `DELETE /admin/pos/carts/{cartId}/items/{variantId}`: Xóa sản phẩm khỏi hóa đơn chờ.
- `POST /admin/pos/orders`: Hoàn tất thanh toán hóa đơn tại quầy, trừ kho tức thời và sinh mã đơn hàng.
