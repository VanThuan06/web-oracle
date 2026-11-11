# REST API contract (v1)

Base path: `/api/v1`. JSON fields use camelCase. List endpoints return `{ data, page, size, totalItems, totalPages }`. Errors return `{ error: { code, message, details? } }`.

## Public storefront

- `GET /catalog/products?query=&categoryId=&color=&clothingSize=&sort=&page=0&pageSize=12` where sort is `newest`, `bestseller`, `price-asc`, or `price-desc`
- `GET /catalog/products/{slug}`
- `GET /catalog/categories`
- `GET /catalog/recommendations?productId=&limit=8`
- `GET /catalog/banners` (active marketing banners)
- `GET /catalog/attributes` (colors, sizes, materials, collars, sleeves, etc.)
- `POST /catalog/contacts` (customer inquiry / feedback submission)
- `GET /promotions/active`

## Session and customer

- `GET /auth/csrf`, `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`
- `GET /cart`, `POST /cart/items`, `PATCH /cart/items/{variantId}`, `DELETE /cart/items/{variantId}`, `DELETE /cart/items`
- `POST /checkout` with an `Idempotency-Key`; accepts shipping address, optional account/customer details for guest checkout, and `paymentMethod` (`COD`, `BANK_TRANSFER_SAMPLE`)
- `GET /orders/mine`, `GET /orders/{id}`, `POST /orders/{id}/reviews`, `POST /orders/{id}/returns` (reason required)
- `GET /profile`, `PATCH /profile`, `GET /profile/addresses`, `POST /profile/addresses`, `PATCH /profile/addresses/{id}`, `DELETE /profile/addresses/{id}`

## Staff/admin

### Products & Variants (Figures 3.1 & 3.2)
- `GET|POST /admin/products`, `GET|PATCH|DELETE /admin/products/{id}`
- `POST /admin/products/generate-variants` (matrix generator from colors & sizes)
- `GET /admin/products/export` (CSV)
- `POST /admin/products/{id}/images` (multipart), `DELETE /admin/products/{id}/images/{imageId}`
- `POST /admin/products/{id}/variants`, `PATCH|DELETE /admin/variants/{id}`

### Product Attributes (9 Entities: Table 2.16 - 2.24)
- `GET|POST /admin/attributes/{type}` (types: `origin-categories`, `categories`, `colors`, `sizes`, `materials`, `collars`, `designs`, `forms`, `sleeves`)
- `PATCH|DELETE /admin/attributes/{type}/{id}`

### Inventory & Fault Products (Table 2.26)
- `GET|POST /admin/fault-products`, `PATCH /admin/fault-products/{id}/status`

### Discounts & Vouchers (Tables 2.33 - 2.36)
- `GET|POST|PATCH /admin/discounts`, `GET|POST|PATCH /admin/vouchers`

### Orders, Invoices, Returns & Exchanges (Tables 2.15, 2.37 - 2.39, ERD 2.31)
- `GET /admin/orders`, `GET /admin/orders/{id}`, `PATCH /admin/orders/{id}/status`
- `GET /admin/orders/{id}/invoice` (printable invoice PDF)
- `GET /admin/returns`, `PATCH /admin/returns/{id}/status`
- `GET|POST /admin/exchanges`, `PATCH /admin/exchanges/{id}/status`

### POS Counter Sales (Figures 3.3 & 3.4)
- `GET|POST /admin/pos/carts`, `DELETE /admin/pos/carts/{cartId}`, `POST /admin/pos/carts/{cartId}/items`, `PATCH|DELETE /admin/pos/carts/{cartId}/items/{variantId}`, `POST /admin/pos/orders` (up to 5 draft bills)
- `GET /admin/pos/barcodes/{barcode}` (instant barcode lookup)

### Customers, Employees & Roles (Tables 2.12 - 2.14, 2.31)
- `GET /admin/customers?query=&page=0&pageSize=20`, `GET /admin/customers/{id}` (includes address book), `POST /admin/customers`
- `GET /admin/customers/export` (CSV)
- `GET|POST|PATCH /admin/staff`, `GET /admin/roles`

### Banners, Contacts & Reviews Moderation (Tables 2.25, 2.27, 2.28, 2.32)
- `GET|POST|PATCH|DELETE /admin/banners[/{id}]`
- `GET /admin/contacts`, `PATCH /admin/contacts/{id}/status`
- `GET /admin/reviews`, `PATCH /admin/reviews/{id}/status`

### Reports & Analytics (Figure 3.7)
- `GET /admin/reports/dashboard?range=day|week|month|year&from=&to=`
- `GET /admin/reports/summary?from=&to=`, `GET /admin/reports/top-products?from=&to=`
- `GET /admin/reports/summary/export?from=&to=` (CSV)
