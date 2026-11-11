# Admin and data-model gap plan

Rechecked against the supplied report: data dictionary tables 2.12–2.41 (printed pages 74–88), ERD figure 2.31 (printed page 89), user requirements tables 1.8 and 2.1, use cases 2.5, and admin screens 3.1 (printed pages 90–95).

## Document-to-schema mapping

The thesis has 30 data-dictionary tables and two additional ERD-only tables. The existing system has 13 domain entities. Keep authentication and commerce normalized while making every thesis concept explicit in the schema or documenting a deliberate mapping:

| Thesis table / concept | Target model | Work |
| --- | --- | --- |
| Employee, Customer | `accounts`, `roles`, `employee_profiles`, `customer_profiles` | Add role and profile records; accounts continue to own credentials. |
| Customer Address, Contact | `customer_addresses`, `customer_contacts` | Keep saved addresses separate from contact records/snapshots. |
| Invoice/Order, OrderDetail | `customer_orders`, `order_lines`, `order_status_history` | Existing order snapshot and timeline; add employee assignment and fulfillment filter fields. |
| Color, Design, Collar, Form, Material, Sleeve, Size | `product_colors`, `product_designs`, `product_collars`, `product_fits`, `product_materials`, `product_sleeves`, `product_sizes` | Add normalized lookup entities and reference them from products/variants. |
| Origin Category, Category | `origin_categories`, `categories` | Add the missing parent-category table and link child categories. |
| Product, ProductDetail, Product Image | `products`, `product_variants`, `product_images` | Existing concepts; expose all attributes, weight, purchase price, barcode, status, and color/size links in admin and API. |
| Banner | `banners` | Add CRUD and connect active banners to the store home page. |
| Fault Product | `fault_products`, `inventory_movements` | Add a stock write-off workflow and movement history. |
| Rating, Rating Image | `product_reviews`, `review_images` | Existing rating entity; add attached images and admin moderation. |
| Role | `roles` | Add a role dictionary and relate accounts to it. |
| Discount, ProductDiscount | `discounts`, `product_discounts` | Separate time-based campaigns and product assignments from voucher codes. |
| Voucher | `vouchers` | Separate customer-entered codes from discount campaigns. |
| Return, ReturnDetail | `order_returns`, `return_lines` | Extend return record and add return-line quantities/status. |
| Checkout, CheckoutDetail | `checkouts`, `checkout_details` | Persist the checkout/payment handoff and its item snapshot. |
| Exchange, ExchangeDetail (ERD only) | `exchanges`, `exchange_lines` | Implement exchange request and item records; document fields absent from the thesis dictionary. |

`Address` and the legacy `Contact` both describe shipping/contact data. The target keeps saved customer addresses and contact snapshots in separate tables because the ERD shows both. `ProductDetail` maps to a sellable SKU (`product_variants`), and `OrderDetail` maps to immutable order-line snapshots. `Invoice` maps to `customer_orders`.

The report contains misspellings (`Wieght`, `Frieght`, `NameColar`) and inconsistent meanings for payment/status fields. Use corrected names and typed fields in code. Do not implement copied jewelry text; the application remains a men's clothing store.

## Dedicated admin surface

The `/admin` route must not render the retail announcement, store header/navigation, or retail footer. Keep the NewMen green/cream palette, but use a separate operations shell with its own topbar, sidebar, workspace, search/filter bars, data tables, and detail views.

Admin modules to cover the report:

- Dashboard and date-filtered daily/weekly/monthly/yearly statistics, with chart and spreadsheet export.
- Products with an expandable SKU table, variant generation from selected colors/sizes, barcode, weight, cost/sale price, image and status; paginated product import/export.
- Categories and all product-attribute dictionaries.
- Inventory movements and fault/write-off records.
- Orders/invoices with status filters, code/customer/phone/channel/date filters, detail, timeline/history and printable PDF.
- POS with five draft bills, barcode lookup, customer/contact, voucher/points area, delivery details, print and checkout.
- Customers, addresses/contacts and export; employees and roles.
- Discount campaigns, product assignments and vouchers.
- Banners, ratings/images and moderation, returns/return lines, and exchanges/exchange lines.

## Delivery slices

1. Add the missing normalized tables, relationships, repositories, Oracle DDL, seed lookups and admin APIs.
2. Add/extend admin API contracts and customer/store API projections where the normalized lookups are visible.
3. Create a dedicated admin shell and navigation, then fill the modules above using the matching API contracts.
4. Reconcile schema counts against the ERD and exercise the H2 admin screens/API flows; preserve Oracle initial-schema validation.
