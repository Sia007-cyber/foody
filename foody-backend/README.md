# Foody Backend — Phase 0 (Base Infrastructure)

Modular Monolith (Spring Boot) for the Foody platform. This module implements the
Phase 0 skeleton: project structure, auth (JWT), core data model, and Flyway
migrations — strictly following `foody-phase0-phase1-spec.md`.

## Stack

- Java 21 (bytecode target), Spring Boot 3.5.16
- Spring Security + JWT (jjwt 0.12) — access + refresh tokens
- Spring Data JPA / Hibernate, Flyway, MySQL Connector/J
- Testcontainers (MySQL 8.4) for integration tests
- Maven

## Module layout

```
com.foody
 ├── auth          controller/service/dto          — JWT, auth endpoints
 ├── users         controller/service/repository/entity/dto
 ├── businesses    controller/service/repository/entity/dto  (read-only lookup in P0)
 ├── menus, products, orders, reservations, reviews, notifications, admin  — skeletons
 └── common        exception / util / config       — shared error envelope, handler
```

**Boundary rule (enforced from day one):** modules talk to each other only through
service *interfaces* (`UserService`, `BusinessService`, …), never another module's
repository. e.g. `WebSecurityConfig`/`JwtAuthenticationFilter` load users via
`UserService`, not `UserRepository`.

## Running

### Option A — Testcontainers (no DB setup; default)
```bash
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))   # or /usr/lib/jvm/java-21-openjdk
mvn test                 # spins up MySQL in Docker automatically
mvn spring-boot:run      # boots with the 'tc' profile (needs Docker)
```

### Option B — local MySQL / MariaDB
```bash
# Create db + user once (adjust for your server):
#   CREATE DATABASE foody;
#   CREATE USER 'foody'@'%' IDENTIFIED BY 'foody';
#   GRANT ALL PRIVILEGES ON foody.* TO 'foody'@'%';
mvn spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-DDB_USERNAME=foody -DDB_PASSWORD=foody -Dspring.datasource.url=jdbc:mysql://localhost:3306/foody?createDatabaseIfNotExist=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8"
```

### Config
`foody.jwt.secret` (base64, 256-bit) — override via env `FOODY_JWT_SECRET` in real deployments.
`foody.jwt.access-token-ttl-minutes` (default 15), `foody.jwt.refresh-token-ttl-days` (default 7).
`foody.cors.allowed-origins` — comma-separated origins allowed to call the API
(default `http://localhost:5173`, the frontend dev server). Override via env
`FOODY_CORS_ALLOWED_ORIGINS` for other deployments.

## API (Phase 0)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register | public | register CUSTOMER, returns tokens |
| POST | /api/auth/login | public | returns tokens |
| POST | /api/auth/refresh | public | refreshToken → new access token |
| POST | /api/auth/logout | any | 200 (client discards tokens) |
| GET  | /api/users/me | Bearer | current user profile |
| PATCH| /api/users/me | Bearer | update fullName/phone/password |
| GET  | /api/businesses?type=&search= | public | Discover: list APPROVED businesses, optional type filter + name search (Phase 1) |
| GET  | /api/businesses/{id} | public | view an APPROVED business (seeded demo) |

Consistent error envelope (JSON):
```json
{ "timestamp":"...", "status":401, "error":"Unauthorized",
  "code":"INVALID_CREDENTIALS", "message":"...", "path":"...", "details":null }
```

## Database model (Phase 0)
`users`, `businesses`, `business_hours`, `menus`, `products` (per spec) +
`business_types` (reference table). `business_type` is a VARCHAR FK to
`business_types`; adding a new type = insert a row + add a `BusinessTypeCode`
constant — **no ALTER TABLE on businesses**.

Migrations are version-controlled and incremental: `V1__init.sql`,
`V2__seed_demo_business.sql`, ..., `V5__seed_admin_user.sql` (seeds an ADMIN
test account — `admin@foody.test` / `password123` — since the register endpoint
only ever creates CUSTOMER accounts). Never change the schema outside a migration.

## Tests
- `AuthServiceImplTest` — unit tests for register/login/refresh branches (mocked).
- `JwtServiceTest` — token round-trip + refresh-type validation.
- `AuthFlowIntegrationTest` — full flow against real MySQL (Testcontainers):
  register → me → refresh → logout, duplicate-email conflict (409), seeded
  business lookup (200/404).
Run: `mvn test`.
