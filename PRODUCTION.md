# Foody dedicated Ubuntu VPS runbook

This is the current production procedure for Ubuntu 24.04, Java 21, MySQL 8.0,
Nginx, and systemd. It prepares repository artifacts only; it does not perform a
server deployment. The older external-Tomcat notes in
[`foody-backend/DEPLOYMENT.md`](foody-backend/DEPLOYMENT.md) are archived and are
not compatible with the current executable-JAR build.

## Production architecture and contract

- Nginx is the only public application entry point on ports 80/443. UFW must not
  expose 8080 or 3306.
- Nginx serves the Vite `dist` directory, falls back to `index.html` for SPA
  routes, and proxies `/api/` and `/uploads/` to `127.0.0.1:8080`.
- systemd runs the executable Spring Boot JAR as the unprivileged `foody` user.
- The `vps` Spring profile enforces `127.0.0.1:8080`, an absolute upload path,
  production CORS/JWT validation, local persistent storage, and a Hikari pool of
  at most four connections with one minimum idle connection.
- `/var/lib/foody/uploads` is persistent application data. Releases under
  `/opt/foody/releases` are immutable and replaceable.
- Flyway owns the schema and Hibernate only validates it. An empty `foody`
  database is initialized by the unchanged V1 through V24 migration chain.
- Demo-account bootstrapping is forced off by the VPS profile. Do not add demo
  password variables to the production environment.
- The cloud `prod` profile remains available for S3-compatible storage. The VPS
  deliberately uses `vps`; never combine production profiles.

## Required environment

Install [`deploy/vps/foody.env.example`](deploy/vps/foody.env.example) as
`/etc/foody/foody.env` and replace every placeholder. Required values are:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Must be exactly `vps`. |
| `FOODY_DB_URL` | JDBC URL for local MySQL database `foody`. |
| `FOODY_DB_USERNAME` | Application/Flyway database principal (`foody_app`). |
| `FOODY_DB_PASSWORD` | Database password; secret. |
| `FOODY_JWT_SECRET` | Base64 of at least 32 random bytes; secret. |
| `FOODY_CORS_ALLOWED_ORIGINS` | Comma-separated exact HTTPS frontend origins, with no path or trailing slash. |
| `FOODY_STORAGE_LOCAL_PATH` | Must be an absolute persistent path; use `/var/lib/foody/uploads`. |

The template also pins the optional business-time-zone default to `Asia/Tehran`
and explicitly leaves demo accounts disabled. Production startup rejects any
attempt to set `FOODY_DEMO_ACCOUNTS_ENABLED=true`.

The backend port and address are intentionally not environment variables in the
template: the VPS profile validates port 8080 and loopback binding. The frontend
needs no production environment variable for this layout; an unset/empty
`VITE_API_BASE_URL` means same-origin. Vite variables are public bundle content,
so never put a secret in one.

Generate the JWT value on a trusted machine with `openssl rand -base64 32`. Do
not paste secrets into shell command arguments, source files, release folders, or
systemd unit files.

## 1. Build a release

Use Java 21, Maven, and a Node version supported by the checked-in Vite version.
From a clean checkout on the build machine:

```sh
cd foody-backend
mvn clean package

cd ../foody-frontend
npm ci
npm test
VITE_API_BASE_URL= npm run build
```

Expected artifacts:

- backend: `foody-backend/target/foody-backend.jar`
- frontend: `foody-frontend/dist/`

The explicit empty frontend variable protects the production build from a local
developer `.env` file. Transfer only the JAR and the contents of `dist/` to the
VPS through the operator's normal authenticated channel. Do not transfer source
`.env` files, `target/`, `node_modules/`, or credentials.

## 2. One-time VPS directories and account

Run these manually on the VPS with administrative privileges:

```sh
sudo adduser --system --group --home /var/lib/foody foody
sudo install -d -o foody -g foody -m 0750 /var/lib/foody
sudo install -d -o foody -g foody -m 0750 /var/lib/foody/uploads
sudo install -d -o root -g root -m 0755 /opt/foody/releases
sudo install -d -o root -g root -m 0700 /etc/foody
```

If the account already exists, do not recreate it; verify it has no interactive
login and owns the two `/var/lib/foody` directories. Nginx does not need direct
filesystem access to uploads because `/uploads/` is proxied to Spring.

Create a release identified by a UTC timestamp, for example:

```sh
RELEASE_ID=20260919T120000Z
sudo install -d -o root -g root -m 0755 /opt/foody/releases/$RELEASE_ID/backend
sudo install -d -o root -g root -m 0755 /opt/foody/releases/$RELEASE_ID/frontend
sudo install -o root -g root -m 0644 /path/to/foody-backend.jar /opt/foody/releases/$RELEASE_ID/backend/foody-backend.jar
sudo cp -a /path/to/dist/. /opt/foody/releases/$RELEASE_ID/frontend/
sudo chown -R root:root /opt/foody/releases/$RELEASE_ID
sudo find /opt/foody/releases/$RELEASE_ID -type d -exec chmod 0755 {} +
sudo find /opt/foody/releases/$RELEASE_ID -type f -exec chmod 0644 {} +
sudo ln -sfn /opt/foody/releases/$RELEASE_ID /opt/foody/current.next
sudo mv -Tf /opt/foody/current.next /opt/foody/current
```

Use a new release directory for every deployment. Never place uploads, logs, or
the environment file below `/opt/foody/current`.

## 3. Database and environment file

MySQL must continue listening only on `127.0.0.1:3306`. The existing empty
database is named `foody` and the runtime user is `foody_app`. If they have not
already been provisioned, connect interactively as a MySQL administrator and use
a unique password in place of the placeholder:

```sql
CREATE DATABASE IF NOT EXISTS foody
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'foody_app'@'127.0.0.1'
  IDENTIFIED BY 'REPLACE_WITH_UNIQUE_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
  ON foody.* TO 'foody_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

The JDBC URL and MySQL account both use `127.0.0.1`; do not grant remote hosts.
Flyway needs the DDL privileges above for first initialization and later migrations.

Copy the example to a temporary root-readable path, edit it without putting
secrets in shell history, then install it:

```sh
sudo install -o root -g root -m 0600 /path/to/completed-foody.env /etc/foody/foody.env
sudo systemd-analyze verify /path/to/foody-backend.service
```

Before the final domain exists, the safe example HTTPS origin can remain for
startup because browser requests are same-origin and do not use CORS. Replace it
with the exact final HTTPS origin before domain cutover.

## 4. Install and start systemd

Install [`deploy/vps/foody-backend.service`](deploy/vps/foody-backend.service):

```sh
sudo install -o root -g root -m 0644 /path/to/foody-backend.service /etc/systemd/system/foody-backend.service
sudo systemctl daemon-reload
sudo systemctl enable foody-backend.service
sudo systemctl start foody-backend.service
```

The service uses a 128 MiB initial heap, 512 MiB maximum heap, 192 MiB maximum
metaspace, and a 768 MiB systemd memory ceiling. This leaves room on the 2 GB VPS
for MySQL, Nginx, the OS, and native JVM memory. Revisit these limits only with
measured memory/GC evidence.

Inspect first startup before configuring public traffic:

```sh
sudo systemctl status foody-backend.service --no-pager
sudo journalctl -u foody-backend.service -b --no-pager
sudo ss -ltnp | grep ':8080'
curl --fail --show-error http://127.0.0.1:8080/api/businesses
```

`ss` must show `127.0.0.1:8080`, never `0.0.0.0:8080` or `[::]:8080`. Startup
must fail rather than continue when secrets, database access, migrations, or the
upload directory are invalid.

## 5. Verify Flyway initialization

The first controlled backend start applies V1 through V23 to the empty database.
The journal should report successful migration and `Started FoodyBackendApplication`.
Verify from an interactive MySQL session without placing the password on the
command line:

```sql
USE foody;
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Every row must have `success = 1`, and the latest version must be `23`. Never edit
a historical migration or use Flyway repair to conceal a checksum mismatch.

## 6. Install Nginx configuration

Install [`deploy/vps/nginx-foody.conf`](deploy/vps/nginx-foody.conf):

```sh
sudo install -o root -g root -m 0644 /path/to/nginx-foody.conf /etc/nginx/sites-available/foody
sudo ln -s /etc/nginx/sites-available/foody /etc/nginx/sites-enabled/foody
sudo nginx -t
sudo systemctl reload nginx
```

Disable the default site if it conflicts with this catch-all server. Re-run
`nginx -t` before every reload. Confirm UFW still allows only 22, 80, and 443;
do not add rules for 8080 or 3306.

Verify through Nginx:

```sh
curl --fail --show-error http://127.0.0.1/api/businesses
curl --fail --show-error http://127.0.0.1/
```

Then test login and each role from a browser, upload a small valid image, load its
`/uploads/...` URL, restart the backend, and confirm the image remains available.
There is no Actuator dependency; `/api/businesses` is the health/readiness check.

## Real administrator and demo verification

There is no public administrator-registration endpoint. After HTTPS is active,
register a normal customer with the real administrator's private email/password,
verify the account, and record its ID from `/api/users/me`. Do not send this
credential over public HTTP and never put its password in SQL or a migration.
In an interactive local MySQL administration session, inspect and promote only
that exact active row:

```sql
SELECT id, email, role, status, public_id
FROM users
WHERE id = <REAL_ADMIN_USER_ID> AND email = '<REAL_ADMIN_EMAIL>';

START TRANSACTION;
UPDATE users
SET role = 'ADMIN', public_id = NULL
WHERE id = <REAL_ADMIN_USER_ID>
  AND email = '<REAL_ADMIN_EMAIL>'
  AND role = 'CUSTOMER'
  AND status = 'ACTIVE';
SELECT ROW_COUNT();
SELECT id, email, role, status, public_id
FROM users
WHERE id = <REAL_ADMIN_USER_ID> AND email = '<REAL_ADMIN_EMAIL>';
COMMIT;
```

Commit only when exactly one intended row changed; otherwise issue `ROLLBACK`
instead. Log out and sign in again to obtain an ADMIN token. Never reactivate the
V2/V5 demo identities, turn this operation into a migration, or add a public
admin-registration API. Confirm `owner@foody.test`, `admin@foody.test`, and the
seeded demo business remain suspended/non-public after startup.

Before declaring the release healthy, record manual results for:

- customer registration/login, public catalog, order, reservation, offer, wallet,
  review, profile upload, logout, and refresh-session behavior;
- two separate business owners, including catalog/order/reservation/upload flows
  and rejection of cross-owner access;
- the real administrator's user management, business moderation, and wallet flows;
- hidden non-approved businesses, rejected unapproved CORS origins, failed demo
  logins, upload persistence across restart, and no secrets in the frontend bundle.

## Logs and diagnosis

```sh
sudo journalctl -u foody-backend.service -n 200 --no-pager
sudo journalctl -u foody-backend.service -f
sudo journalctl -u nginx.service -n 100 --no-pager
sudo tail -n 100 /var/log/nginx/error.log
```

Application logs go only to journald; no writable log directory is required in a
release. Configure and monitor journald/Nginx retention at the OS level.

## Automated future deployments

The production-safe GitHub Actions pipeline, required GitHub values, exact
least-privilege VPS setup, immutable activation, health checks, rollback, and
release retention are documented in
[`deploy/vps/CI-CD.md`](deploy/vps/CI-CD.md). The manual sequence below remains
useful as an operator reference, but routine deployments should use the reviewed
pipeline after its one-time setup and environment protection are verified.

## Manual future deployment reference

1. Back up MySQL and `/var/lib/foody/uploads`.
2. Build and test from a clean checkout using the commands above.
3. Copy artifacts into a new root-owned timestamped release directory.
4. Record the current symlink target: `readlink -f /opt/foody/current`.
5. Atomically point `/opt/foody/current` at the new release.
6. Run `sudo systemctl restart foody-backend.service`.
7. Watch the journal for Flyway validation/migration and successful startup.
8. Run localhost and Nginx health checks plus a focused login/upload smoke test.
9. Keep at least the previous release until verification and backup retention are
   complete; delete old releases only under an explicit retention policy.

For an API-incompatible release, use a brief maintenance window so the newly
served frontend cannot call the old backend during restart.

## Rollback

An application rollback does not undo a Flyway migration. Only roll back the JAR
alone when the previous application is compatible with the migrated schema:

```sh
sudo ln -sfn /opt/foody/releases/PREVIOUS_RELEASE /opt/foody/current.next
sudo mv -Tf /opt/foody/current.next /opt/foody/current
sudo systemctl restart foody-backend.service
curl --fail --show-error http://127.0.0.1:8080/api/businesses
```

If a migration is not backward-compatible, stop the service, restore the matched
pre-deployment MySQL backup and upload snapshot, switch the release symlink, then
start and verify. Never manually delete Flyway history rows as a rollback.

## Backups

- Take consistent MySQL backups (for example `mysqldump --single-transaction
  --routines --triggers foody`) using a protected option file or interactive
  credentials, never a password in the command line.
- Back up `/var/lib/foody/uploads` in the same release window so database media
  URLs and files remain consistent.
- Store encrypted copies off the VPS, apply retention, monitor backup failures,
  and regularly test restoring both database and uploads to an isolated system.
- Back up `/etc/foody/foody.env` securely and separately; access must remain
  restricted because it contains production secrets.

## Pending domain and HTTPS steps

When the final domain is known:

1. Point DNS A/AAAA records to the VPS and replace `server_name _` with the domain.
2. Set `FOODY_CORS_ALLOWED_ORIGINS` to the exact `https://domain` value and restart
   the backend.
3. Obtain and install a certificate using the operator's chosen ACME client (for
   example Certbot's Nginx integration), enable HTTP-to-HTTPS redirect, and test
   automatic renewal.
4. Run `nginx -t`, reload Nginx, and repeat API, SPA-route, login, and upload tests
   over HTTPS.

Until those steps are complete, HTTPS readiness is pending. Do not hardcode the
VPS IP, domain, or backend port into the frontend bundle.
