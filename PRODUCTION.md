# Foody web production operator runbook

This is the deployment contract for the current web application only. Repository work is complete when this file is committed; provisioning accounts, secrets, domains, TLS, and live checks are operator actions. Do not treat this guide as evidence that a deployment has been performed.

## Repository-enforced production contract

- The Java 21 Docker image defaults to the `prod` profile, runs Flyway V1–V19, and Hibernate only validates schema.
- Production JDBC uses MySQL `sslMode=VERIFY_IDENTITY`; the Aiven hostname must match its certificate and its CA must be trusted by the JVM.
- Production rejects weak/default JWT secrets, invalid CORS origins, and incomplete S3-compatible storage configuration. There is no local upload fallback.
- Vite production builds require `VITE_API_BASE_URL`; `foody-frontend/vercel.json` provides SPA rewrites.
- V19 preserves demo rows and foreign-key history, but disables the known demo credentials, revokes their sessions, and suspends the seeded business.

## Definitive environment variables

Only these variables are read by the production code/configuration. Put backend values in Render's server-side environment. Never put secrets in a Vercel `VITE_*` variable.

### Backend / Render

| Variable | Required | Secret | Purpose and placeholder shape |
|---|---:|---:|---|
| `SPRING_PROFILES_ACTIVE` | Yes | No | `prod`; do not combine with `local` or `tc`. The image defaults to it, but set it explicitly. |
| `DB_HOST` | Yes | No | Aiven hostname matching its certificate: `<service>.aivencloud.com`. |
| `DB_PORT` | Yes | No | Aiven MySQL TLS port: `<port>`. |
| `DB_NAME` | Yes | No | Existing application database: `foody`. |
| `DB_USERNAME` | Yes | Yes | Principal with Flyway DDL and runtime DML rights: `<database-user>`. |
| `DB_PASSWORD` | Yes | Yes | Password for that principal: `<database-password>`. |
| `FOODY_JWT_SECRET` | Yes | Yes | Unique Base64 that decodes to at least 32 bytes: `<base64-32-byte-or-longer-secret>`. Generate, for example, with `openssl rand -base64 32`. |
| `FOODY_CORS_ALLOWED_ORIGINS` | Yes | No | Exact comma-separated HTTPS frontend origins, no path/trailing slash/wildcard: `https://app.example.com,https://www.example.com`. |
| `FOODY_BUSINESS_TIME_ZONE` | No | No | Reservation business timezone; defaults to `Asia/Tehran`: `Asia/Tehran`. |
| `FOODY_STORAGE_BUCKET` | Yes | No | S3-compatible bucket: `foody-public-images`. |
| `FOODY_STORAGE_REGION` | Yes | No | Provider region; R2 commonly uses `auto`. |
| `FOODY_STORAGE_ENDPOINT` | Provider-dependent | No | HTTPS S3 API endpoint, no credentials/query/fragment. Required for R2: `https://<account-id>.r2.cloudflarestorage.com`; leave blank only for AWS S3. |
| `FOODY_STORAGE_ACCESS_KEY` | Yes | Yes | Bucket-scoped S3 API access key: `<storage-access-key>`. |
| `FOODY_STORAGE_SECRET_KEY` | Yes | Yes | Corresponding API secret: `<storage-secret-key>`. |
| `FOODY_STORAGE_PUBLIC_BASE_URL` | Yes | No | HTTPS public image origin/prefix, no credentials/query/fragment: `https://media.example.com`. Persisted image URLs are beneath this value. |

`PORT` is optionally supplied by Render and consumed as `server.port` (default `8080`); do not set a conflicting fixed value. `FOODY_UPLOAD_DIR` is local/test only and unused in `prod`. `TZ=UTC` is baked into the image. `JAVA_TOOL_OPTIONS` is needed only if the Aiven CA is not trusted by Java 21: use a deployment-managed truststore, e.g. `-Djavax.net.ssl.trustStore=/path/to/aiven-truststore -Djavax.net.ssl.trustStorePassword=<truststore-password>`.

### Frontend / Vercel

| Variable | Required | Secret | Purpose and placeholder shape |
|---|---:|---:|---|
| `VITE_API_BASE_URL` | Yes | No | Backend HTTPS origin only—no `/api`, path, query, fragment, or credentials: `https://api.example.com`. |

There are no other frontend production variables. Vite embeds `VITE_*` values in the browser bundle.

## Durable storage: Cloudflare R2 example

The implementation does S3 `PutObject` to generated `profiles/`, `business-covers/`, and `products/` keys, and `DeleteObject` for replacement/removal. It does not use client filenames, bucket listing, or application-side object reads.

1. Create an otherwise empty bucket, e.g. `foody-public-images`.
2. Configure public **read** delivery through a provider public URL or, preferably, a verified custom hostname such as `media.example.com`; complete provider DNS/TLS verification. Use that public HTTPS URL for `FOODY_STORAGE_PUBLIC_BASE_URL`, not the S3 API endpoint.
3. Create an S3 API credential restricted to this bucket and object write/delete operations. Do not grant account-wide administration and never expose the credential to Vercel. For R2, create a token scoped only to the bucket with the minimum object read/write capability R2 offers; public reads come from the public domain, not Render's credential.
4. Map provider values exactly:

   | Provider value | Foody variable |
   |---|---|
   | Bucket | `FOODY_STORAGE_BUCKET=foody-public-images` |
   | R2 compatibility region | `FOODY_STORAGE_REGION=auto` |
   | R2 S3 API endpoint | `FOODY_STORAGE_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com` |
   | S3 access key ID | `FOODY_STORAGE_ACCESS_KEY=<render-secret>` |
   | S3 secret access key | `FOODY_STORAGE_SECRET_KEY=<render-secret>` |
   | Public/custom-domain image URL | `FOODY_STORAGE_PUBLIC_BASE_URL=https://media.example.com` |

5. After deployment, upload a profile, cover, or product image; confirm the saved public URL loads over HTTPS. Restart/redeploy Render and reload it.
6. Replace that image and confirm the record points to the new URL and the prior managed object is removed. Delete a product with a managed image and confirm its directly associated object is removed. A failed database replacement retains the previous URL; unknown legacy/external URLs are intentionally not deleted.

Do not use Render disk for production images. Existing `/uploads/...` database URLs are retained but need a separate content migration if they must stay visible after cutover.

## First real production administrator

There is intentionally no public admin-registration or permanent bootstrap endpoint. Registration rejects `ADMIN`, so the supported one-time procedure is normal registration followed by a narrowly targeted, audited database promotion. This is an explicit operator-controlled mechanism; no code change is needed.

1. On deployed HTTPS, register the real operator's unique email with an operator-chosen password. Verify login and record its numeric ID from `/api/users/me`.
2. In a trusted Aiven administration session, inspect the exact account:

   ```sql
   SELECT id, email, role, status, public_id
   FROM users
   WHERE id = <real-user-id> AND email = '<real-admin-email>';
   ```

3. Confirm it is the intended ACTIVE account, then promote exactly that row. `public_id` is cleared because it is a customer-only identifier.

   ```sql
   START TRANSACTION;

   UPDATE users
   SET role = 'ADMIN', public_id = NULL
   WHERE id = <real-user-id>
     AND email = '<real-admin-email>'
     AND role = 'CUSTOMER'
     AND status = 'ACTIVE';

   SELECT id, email, role, status, public_id
   FROM users
   WHERE id = <real-user-id> AND email = '<real-admin-email>';

   COMMIT;
   ```

   Commit only if precisely one row changed and the returned row is correct; otherwise `ROLLBACK`. Do not alter password hashes, status, IDs, or related records—this preserves referential integrity. Log out and sign in again after promotion to receive an ADMIN token.
4. Verify `/api/users/me` reports `ADMIN` and an admin-only screen works. Never reactivate a demo account and do not convert this SQL into a startup migration or public API.

## Demo seed verification

V2/V5 initially inserted `owner@foody.test` and `admin@foody.test`. V19 deliberately keeps their rows for historical/FK reasons, but targets the original ID-and-email pairs, sets `status='SUSPENDED'`, replaces the password hash with a disabled marker, revokes unrevoked refresh sessions, and suspends the seeded owner business.

- **Record existence:** expected for referential integrity.
- **Authentication capability:** unavailable; the disabled hash and suspended status both prevent authentication. `AuthFlowIntegrationTest.seededBusiness_isNotPublic` verifies both known credential pairs return 401.
- **Public visibility:** the seeded business is absent from public browse/detail APIs because only `APPROVED` businesses are public; that test verifies `GET /api/businesses/1` is 404. There is no public user-directory endpoint.

Before cutover, inspect for renamed/copied demo accounts or other fabricated data; V19 cannot identify records no longer matching its original ID/email predicate.

## Vercel + Render + Aiven release order

1. Provision Aiven MySQL and `DB_NAME`. Obtain hostname, TLS port, credentials, and CA chain. Confirm Java 21 trusts the verified CA; otherwise install it in a deployment-managed truststore and configure `JAVA_TOOL_OPTIONS`. Do not downgrade `VERIFY_IDENTITY`.
2. Back up any target database. Flyway `baseline-on-migrate` is disabled: reconcile a nonempty untracked schema and V12 duplicate-business-owner data before deployment. Never edit applied migrations or blindly repair checksums.
3. Provision the public storage bucket/domain and restricted credential above.
4. Create Render with root/Docker context `foody-backend` and its `Dockerfile`. Set every required backend variable, including exact final Vercel production origin(s) in `FOODY_CORS_ALLOWED_ORIGINS`.
5. Deploy backend. Render logs must show Flyway application/validation through V19, Hibernate validation, and successful bind. There is no implemented Actuator health endpoint: configure `GET /api/businesses` as Render's health check and verify 200 after startup.
6. Create Vercel with root `foody-frontend`, `npm ci`, `npm run build`, output `dist`, Node 22.12+ (or supported newer), and `VITE_API_BASE_URL=https://<render-backend-host>`. Deploy the SPA.
7. If final frontend domain differs from step 4, update Render CORS with only exact final HTTPS origin(s), redeploy backend, then redeploy Vercel if its API URL changed.
8. Establish the real administrator and run every live acceptance item below over HTTPS.

## Live acceptance checklist

These are manual production checks; repository tests do not mark them passed. Record URL, timestamp, and result.

### Customer

- [ ] Register, login, browse an APPROVED business and catalog.
- [ ] Create an order, reservation, and offer claim; confirm wallet behavior within the current business-credit scope.
- [ ] Create, update, and delete a review.
- [ ] Upload a profile image where available; logout; confirm the old refresh session cannot restore login.

### Business owner

- [ ] Login; access only own business; manage catalog, orders, reservations, offers, wallets, and available image uploads.
- [ ] With a second owner, verify cross-owner business/catalog/order/reservation access is rejected.

### Administrator

- [ ] Login with the promoted real admin; use user management, business moderation, and wallet administration.
- [ ] Verify PENDING, REJECTED, and SUSPENDED businesses remain hidden from public browse/detail APIs.

### Infrastructure/security

- [ ] Upload an image, restart/redeploy Render, and confirm it persists; test replacement/removal.
- [ ] Confirm MySQL uses verified TLS/hostname validation, not only encrypted transport.
- [ ] Confirm CORS accepts the configured production frontend and rejects an unapproved origin.
- [ ] Confirm known demo credentials fail and no secret is exposed in the Vercel bundle/environment.
- [ ] Confirm refresh rotation rejects reused tokens and logout prevents refresh-session restoration.

## Out of scope

Do not begin PWA/mobile work from this runbook. Offline/private-data caching, service workers, installability, push, and native packaging are separate work after web acceptance succeeds.
