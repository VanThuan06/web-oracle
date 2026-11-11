# NewMen Store requirements and assumptions

The source is the supplied graduation report for a men's shirt store. The implementation follows its commerce workflows and ignores copied passages that describe jewelry or unrelated systems.

## Customer

- Browse, search, filter, sort, and paginate shirts; inspect color/size variants and availability.
- Add variants to a cart, change quantities, apply a voucher, provide delivery details, and place an order.
- Register/sign in, maintain profile and delivery addresses, view order history and status timeline, and leave a product rating/review.
- Receive basic rule-based product suggestions.

## Staff and administrator

- Maintain products, categories, catalog attributes, color/size variants, prices, stock, and images.
- Upload product images to local server storage.
- Create and process in-store POS orders, including customer lookup, barcode/product search, and multiple draft bills.
- Search customers and orders, move orders through a tracked lifecycle, and view order details.
- Manage staff access, promotions/vouchers, and review revenue/order/product summaries with spreadsheet-friendly export.
- View/print an order invoice and process basic return requests with a recorded status.

## Simplifications where the report is incomplete

- Use a normalized schema with product, variant, category, image, customer/account, address, cart, order, order-line, inventory-movement, promotion, voucher, review, and order-history records.
- A variant is the stock-keeping unit for a product/color/size combination; order lines retain an immutable unit-price snapshot.
- Cart and checkout calculations are server-authoritative. Orders are placed with cash on delivery or a clearly marked sample payment option; no real payment call is made.
- Report exports use CSV, which opens in Excel; PDF invoice output is printable and contains order snapshots. Returns support requested/approved/rejected/received states; the report does not specify a detailed refund or carrier workflow.
- The local profile supplies sample products and a demo administrator. Production passwords and Oracle credentials come only from environment variables.
- UI text uses Vietnamese and VND formatting. The thesis's references to products as jewelry are treated as copy errors.
