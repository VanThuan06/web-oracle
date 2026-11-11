# Delivery checklist

- [x] Record requirements and API contract; initialize the project repository.
- [x] Scaffold Spring Boot and Angular projects with local run profiles.
- [x] Implement catalog and storefront.
- [x] Implement customer cart, checkout, account, and order history.
- [x] Implement admin catalog, inventory/image upload, orders, POS, promotions, customers, and reports.
- [x] Verify H2 startup, API health, and Angular build; document environment limits.

Manual H2 smoke checks covered auth/CSRF, catalog sorting and recommendations, customer search pagination, checkout/shipping thresholds, admin POS, return/order lifecycle, invoice/report routes, and exactly-once stock restoration on concurrent cancellation. No automated test suite was added or run.
