# NewMen Store API

Spring Boot 4.0.6 / Java 21 backend for the NewMen men's apparel storefront and store operations. The `dev` profile is selected by default and uses an in-memory H2 database with sample products and a local demo administrator.

## Run locally (Windows)

From the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The wrapper uses installed Maven or the local Maven 3.9.16 cache and downloads the official Maven distribution when neither is available. A dependency download may still be required the first time if Maven Central is reachable.

- API: `http://localhost:8188/api/v1`
- Health: `http://localhost:8188/actuator/health`
- Demo administrator: `admin@newmen.local` / `ChangeMe123!`
- H2 console: `http://localhost:8188/h2-console` (JDBC URL `jdbc:h2:mem:newmen`, user `sa`, blank password)
- Uploaded images are stored in `./uploads` relative to the backend process directory; override with `NEWMEN_UPLOAD_DIR`.

The H2 console servlet and its security exceptions exist only while `spring.h2.console.enabled=true`; the `dev` profile enables it, and the Oracle profile leaves it denied. The demo administrator password is only for local development. Set `NEWMEN_DEMO_ADMIN_PASSWORD` before startup to replace it. Seed data is disabled outside `dev`.

## Oracle profile

The project includes the Oracle `ojdbc11` driver and an example profile. Set a real Oracle service name, user, and password in the environment, apply the initial schema script, and start with the `oracle` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'oracle'
$env:ORACLE_JDBC_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:ORACLE_DB_USER = 'NEWMEN'
$env:ORACLE_DB_PASSWORD = '<your-local-password>'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

`db/oracle-schema.sql` is a starting schema for Oracle 12c or newer. The Oracle profile uses Hibernate `validate`; it does not create tables automatically. Review and version a migration strategy before using a shared or production database. The H2 profile uses Hibernate schema update for local trials.

## Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `PORT` | HTTP port | `8188` |
| `APP_CORS_ORIGINS` | Explicit comma-separated browser origins | `http://localhost:4220,http://127.0.0.1:4220` |
| `NEWMEN_UPLOAD_DIR` | Local image directory | `./uploads` |
| `NEWMEN_DEMO_ADMIN_EMAIL` | Seed administrator email (`dev` only) | `admin@newmen.local` |
| `NEWMEN_DEMO_ADMIN_PASSWORD` | Seed administrator password (`dev` only) | `ChangeMe123!` |
| `ORACLE_JDBC_URL` | Oracle JDBC URL (`oracle` profile) | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `ORACLE_DB_USER` | Oracle schema user | `NEWMEN` |
| `ORACLE_DB_PASSWORD` | Oracle password | empty; provide it locally |
| `SESSION_COOKIE_SECURE` | Mark session cookie secure behind HTTPS | `false` |
| `FREE_SHIPPING_THRESHOLD` | Minimum merchandise subtotal for free delivery | `899000` VND |

Session cookies are HttpOnly and SameSite=Lax; state-changing requests require the Angular-compatible CSRF cookie/header. Refresh the token through `GET /api/v1/auth/csrf` after login or logout and before state-changing requests. The Angular development setup should use relative `/api/v1` requests through its proxy.

## Implemented areas

- Public catalog search, category and variant filtering, sorting, pagination, product detail, recommendations, and active voucher list.
- Customer registration/login/logout, profile and address book, session cart, voucher application, guest or member checkout, order history/detail, review and return request.
- Admin products, variants, categories, local image upload/delete, customers, staff, promotions, POS draft bills, order status history, returns, reports, CSV export, and printable PDF invoices.
- Server-side validation, hashed passwords, role checks, CSRF, structured API errors, transactional checkout with locked stock rows and idempotency keys.

Payment (`BANK_TRANSFER_SAMPLE`) and shipping settings are placeholders. No real payment, carrier, mail/SMS, Firebase, or other third-party service is called. POS draft carts live in the authenticated HTTP session (up to five per session); completed orders are persisted in the database.

The source report has incomplete and contradictory details. This implementation uses product variants as the stock unit and stores immutable order-line price snapshots. Advanced return/refund processing, product import/export, barcode label printing, and the thesis's recommendation profile logic require follow-up specifications.
