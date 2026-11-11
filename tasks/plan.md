# Implementation Plan: NewMen Store

## Overview

Build a Vietnamese men's apparel store with an Angular storefront and operations console, backed by a Spring Boot REST API. The requested database target is Oracle; the runnable local profile uses H2 with the same JPA model. Product images are stored on local disk. Payment, shipping, and messaging providers remain sample adapters.

## Architecture decisions

- Spring Boot 4.0.6 / Java 21, Maven bootstrap wrapper, REST API under `/api/v1`.
- Angular standalone application with typed API services and a development proxy.
- H2 in-memory profile for local operation with JPA schema update; Oracle profile reads credentials from environment variables and validates a pre-provisioned schema from the included SQL example. Add a versioned migration tool before using a shared or production Oracle database.
- Session authentication with HttpOnly session cookie and CSRF token; demo admin seeded only in the local profile.
- Local image storage outside the classpath, configurable directory, MIME/size allowlist, and server-generated names.
- Normalize the core commerce model around products and sellable variants, orders and order lines, inventory movements, and order status history.

## Delivery slices

1. [x] Foundation: requirements, API contract, project skeleton, profiles, startup instructions.
2. [x] Catalog: categories/products/variants, search and detail APIs, sample catalog, storefront views.
3. [x] Customer flow: account/profile, cart, checkout, order history and tracking.
4. [x] Store operations: staff access, POS, product/variant/inventory management, local image uploads.
5. [x] Administration: order lifecycle, promotions/vouchers, customer list, dashboard and reports.
6. [x] Finish: docs, build/startup checks, H2 smoke run, review and handoff.

## Verification

- Build the Spring Boot application and Angular production bundle where dependencies are available.
- Start the backend with the H2 `dev` profile and confirm health plus seed data.
- Run the storefront against the local API and inspect the main customer and operator flows.
- No automated tests are added or run in this delivery.

## Verification result

- Backend: offline clean Maven compile succeeded; H2 dev health returned `UP`.
- Frontend: Angular production build succeeded. Angular reports two non-fatal component stylesheet budget warnings.
- H2 manual smoke: CSRF login and protected POS requests succeeded; sorting, recommendation limits, customer filtering/pagination, free-shipping threshold, order/return transitions, and exactly-once stock restoration were checked. Concurrent cancellation returned one success and one conflict, with stock restored once.
- Oracle: JDBC driver, profile, and schema example are included; a live Oracle connection was not available to verify.

## Risks and assumptions

- The report has inconsistent and copied descriptions (including references to jewelry); the men's shirt domain in the title and core business sections is authoritative.
- The report's database tables are incomplete and sometimes inconsistent. Implement a coherent normalized core model rather than reproducing those errors.
- Real Oracle credentials/database, payment gateway, shipping carrier, email/SMS provider, and production deployment settings were not provided; their adapters/configuration will be examples only.
- Recommendation rules are a transparent prototype, not machine learning.
