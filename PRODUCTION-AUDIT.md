# Final production-readiness audit — 2026-09-09

**Decision at audit time: not yet ready for production deployment within the current web scope.**
The durable-upload code blocker was subsequently resolved with mandatory production S3-compatible storage;
provider provisioning and live deployment verification remain external prerequisites.
No live deployment was changed, and no PWA/mobile implementation was started.

## Verification

| Check | Result |
|---|---|
| Backend full suite, Java 21 | 294 passed; 0 failures/errors/skips |
| Frontend full suite | 93 passed; 0 failures/skips |
| Backend package | Passed: `mvn -q package -DskipTests`, Java 21 |
| Frontend production build | Passed with explicit HTTPS example API origin; not a live-service connectivity test |
| Missing frontend production API URL | Build fails as intended |
| Database / Flyway | Fresh Testcontainers MySQL 8.4 migrated V1–V19; checksums validated; Hibernate schema validation passed |
| Production startup | `prod` Spring context starts against container MySQL with explicit test secret/CORS/upload path; allowed/disallowed CORS tested |
| `git diff --check` | Passed |

The initial sandboxed backend attempt could not access Docker; the authorized Docker-enabled
rerun passed. Frontend tests include source contracts and simulated API/session tests, not browser E2E.
Aiven TLS, the deployed Vercel/Render settings, disk persistence, and the Docker image itself
were not exercised against live infrastructure.

## FIXED

- Production defaults: application and container default to `prod`; test resources explicitly select `tc`.
  Production requires explicit HTTPS CORS origins and an absolute upload path; mixed dev/prod profiles fail.
  JWT enforcement remains in place. CORS entries are trimmed. Docker uses Java 21 and UTC.
- Database transport now uses certificate/hostname verification (`VERIFY_IDENTITY`), and Flyway
  no longer silently baselines nonempty untracked schemas.
- Added **V19__disable_demo_accounts.sql**: disables the original known owner/admin credentials,
  revokes their refresh sessions and hides the demo business, preserving all historical references.
  V1–V18 were not rewritten.
- Frontend production builds require a valid HTTPS API origin, normalize trailing slashes, and cannot
  fall back to localhost. Added Vercel SPA rewrites for direct navigation/refresh.
- Owner catalog now reads an authenticated ownership-checked endpoint for pending/unapproved businesses,
  uses a separate private query key, refreshes product caches after edits, and displays read failures.
- Customer wallet route and home are customer-only; owner navigation leads to owner wallet administration.
  Offers shortcut opens the completed Offers page. Order/reservation creation invalidates cached lists.
- Removed unsupported wallet-payment marketing and geographic proximity claims for unranked discovery.
  No remaining fabricated ratings, discounts, distances, transactions or operational statistics were found
  in the focused customer/owner/admin UI review. Explicit coming-soon screens remain labelled.
- Product price precision/range now matches `DECIMAL(10,2)`; oversized order totals return a domain error
  before persistence. Malformed JSON/parameters and oversized multipart uploads receive controlled errors.
- Replaced obsolete seed-account/setup instructions with current deployment prerequisites.

## BLOCKER

- **Object storage is not provisioned or verified live.** Production code now requires S3-compatible storage
  and has no local fallback. Create/configure the public-image bucket and verify credentials and public delivery.
- **Release configuration still needs operator verification:** real administrator provisioning after demo
  retirement; Aiven CA trust/hostname validation; actual secrets and exact Vercel CORS/API origins; deployed
  customer/owner/admin acceptance. Renamed/copied demo accounts require inspection of the existing database.
  These cannot be certified from the repository or local container tests.

## NON-BLOCKER

- Wallets are business credit ledgers, not checkout payments; offers are claims, not discount codes.
  Reservations do not allocate tables or calculate capacity. Admin review moderation and other explicitly
  labelled future tools are not implemented; user/business moderation and wallet/order administration exist.
- V13 retains legacy global wallet tables without assigning their balances to businesses.
- There is no Actuator endpoint. The guide uses existing public, database-backed `/api/businesses`
  for Render HTTP health checks. No current application flow depends on proxy-generated absolute URLs.
- With a persistent disk, the existing file implementation supports one mounted backend instance;
  it does not provide shared multi-instance object storage.

## Security and integration review

No regression found in JWT-secret enforcement, refresh rotation/revocation, failed-refresh session clearing,
logout revocation requests, private cache clearing, principal-derived ownership, suspended/deleted account
rejection or role-protected owner/admin APIs. Existing and added regression tests pass. Logout's server
notification remains best-effort if the network is unavailable; local session clearing is immediate.
Reviewed checkout/order status and history, reservation creation/status, offer claims, wallet operations,
review CRUD, owner catalog/business workflows and admin contracts. Confirmed fixes are listed above;
this is a focused code/test audit, not a live browser acceptance certification.

## DEFERRED / before PWA

Complete the production blockers and deployed web acceptance first. No current SPA architecture blocker
requiring redesign was identified. Offline/private-data cache policy, service workers, installability,
push and native packaging belong to the next PWA/mobile phase. None was implemented here.

See [deployment instructions](PRODUCTION.md) for required configuration and infrastructure references.

## Files changed

- `PRODUCTION-AUDIT.md`
- `PRODUCTION.md`
- `README.md`
- `foody-backend/Dockerfile`
- `foody-backend/README.md`
- `foody-backend/src/main/java/com/foody/auth/config/WebSecurityConfig.java`
- `foody-backend/src/main/java/com/foody/common/config/ProductionConfig.java`
- `foody-backend/src/main/java/com/foody/common/exception/GlobalExceptionHandler.java`
- `foody-backend/src/main/java/com/foody/orders/service/OrderServiceImpl.java`
- `foody-backend/src/main/java/com/foody/products/controller/ProductOwnerController.java`
- `foody-backend/src/main/java/com/foody/products/dto/CreateProductRequest.java`
- `foody-backend/src/main/java/com/foody/products/dto/UpdateProductRequest.java`
- `foody-backend/src/main/java/com/foody/products/service/ProductService.java`
- `foody-backend/src/main/java/com/foody/products/service/ProductServiceImpl.java`
- `foody-backend/src/main/resources/application.yml`
- `foody-backend/src/main/resources/db/migration/V19__disable_demo_accounts.sql`
- `foody-backend/src/test/java/com/foody/ProductionStartupIntegrationTest.java`
- `foody-backend/src/test/java/com/foody/auth/AuthFlowIntegrationTest.java`
- `foody-backend/src/test/java/com/foody/businesses/BusinessCatalogIntegrityIntegrationTest.java`
- `foody-backend/src/test/java/com/foody/common/config/ProductionConfigTest.java`
- `foody-backend/src/test/java/com/foody/orders/service/OrderServiceImplTest.java`
- `foody-backend/src/test/resources/application.properties`
- `foody-frontend/README.md`
- `foody-frontend/SETUP.md`
- `foody-frontend/src/App.tsx`
- `foody-frontend/src/components/PublicNav.tsx`
- `foody-frontend/src/features/auth/AuthVisual.tsx`
- `foody-frontend/src/features/catalog/catalogApi.ts`
- `foody-frontend/src/features/discover/CustomerHome.tsx`
- `foody-frontend/src/features/discover/DiscoverPage.tsx`
- `foody-frontend/src/features/orders/CheckoutPage.tsx`
- `foody-frontend/src/features/owner/OwnerMenusPage.tsx`
- `foody-frontend/src/features/reservations/NewReservationPage.tsx`
- `foody-frontend/src/lib/api.ts`
- `foody-frontend/src/lib/apiBaseUrl.ts`
- `foody-frontend/tests/api-base-url.test.mjs`
- `foody-frontend/tests/customer-home.test.mjs`
- `foody-frontend/vercel.json`
- `foody-frontend/vite.config.ts`
