# NewMen Store (web-oracle)

Starter commerce platform for a men's apparel store, implemented as an Angular storefront/operations console and a Spring Boot REST API. The local profile uses H2 for a quick run; an Oracle profile and JDBC driver are included for a real database connection. Uploaded product images are stored on the backend's local disk.

## Project layout

- `frontend/` — Angular client
- `backend/` — Spring Boot API and database profiles
- `docs/requirements.md` — extracted requirements and assumptions
- `docs/api-contract.md` — API contract shared by frontend and backend
- `docker-compose.yml` & `README-DOCKER.md` — 1-click Docker deployment without Java/Node/Maven

## Run locally

Requirements: Java 21, Node.js 22.22.3 or newer in the 22.x line, and npm.

Start the backend from the repository root:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The default profile is `dev` and uses H2. Then, in another terminal, start Angular:

```powershell
Set-Location frontend
npm ci
npm run start
```

The development proxy forwards `/api` calls to `http://localhost:8188`. Open `http://127.0.0.1:4220`.

The `dev` profile uses an in-memory H2 database and seeds sample apparel/catalog data. Demo administrator: `admin@newmen.local` / `ChangeMe123!` (local use only; override with `NEWMEN_DEMO_ADMIN_PASSWORD`). The backend README has the H2 console and Oracle setup details.

## Oracle connection example

Set the Oracle profile and provide credentials via environment variables; do not put real credentials in committed configuration files.

```powershell
$env:SPRING_PROFILES_ACTIVE = "oracle"
$env:ORACLE_JDBC_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
$env:ORACLE_DB_USER = "NEWMEN"
$env:ORACLE_DB_PASSWORD = "change-me"
```

Replace the sample service name and credentials with values from your Oracle instance. External payment and delivery providers are stubs and do not make live calls.
Before starting the Oracle profile, apply `backend/db/oracle-schema.sql` as the schema owner; the Oracle profile validates the existing schema.

## Configuration notes

- Product images default to a configurable local `uploads/` directory; ensure it is writable and back it up with the database.
- Checkout supports cash on delivery and a sample bank-transfer mode. No card or bank credentials are collected.
- The source report contains incomplete/inconsistent data tables; the implementation uses a normalized product-variant, order-line, and inventory model described in `docs/requirements.md`.

## Reference versions

See the official [Angular version compatibility table](https://angular.dev/reference/versions), [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html), and [Oracle JDBC guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/jjdbc/jdbc-developers-guide.pdf) when upgrading dependencies.
