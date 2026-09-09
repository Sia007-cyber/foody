# Foody

Foody is a Persian web application with customer, business-owner, and admin workflows.
The backend uses Java 21, Spring Boot, MySQL and Flyway; the frontend uses React,
TypeScript, Vite, React Router and TanStack Query.

Implemented scope: authentication and refresh-token sessions, approved-business discovery,
owner catalog management, pickup/delivery orders and checkout, independent table reservations,
business-scoped wallets, limited-capacity offers and claims, customer reviews, notifications,
and admin user/business/order/wallet management.

Wallets are business credit ledgers, not an online payment gateway or checkout payment method.
Offers are capacity-limited claims, not discount codes. Reservation capacity/table allocation
is not calculated. Additional dashboard tools labelled “coming soon” remain outside this scope.

Production uploads require configured S3-compatible object storage; local/test profiles use disk storage.
See [production setup and release prerequisites](PRODUCTION.md) and
[final audit results](PRODUCTION-AUDIT.md). Historical phase notes are not deployment instructions.

## Local development

Use Java 21, Maven, MySQL 8.4 and Node 22.12+ (or a supported newer Node release).

```sh
cd foody-backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

The local profile uses database `foody` and env-overridable `DB_HOST`, `DB_PORT`,
`DB_USERNAME`, `DB_PASSWORD` (defaults: localhost, 3306, foody, foody).
It creates the database if permitted. Flyway owns the schema; Hibernate only validates it.
The default application and Docker profile is **prod**, which requires explicit configuration.
`tc` is selected by test resources and only the integration-test harness starts containers.

In a second terminal:

```sh
cd foody-frontend
npm ci
npm run dev
```

Development API fallback: `http://localhost:8080`; Vite serves `http://localhost:5173`.
Register fresh customer/owner accounts. V19 disables the old demo accounts and public demo business.
Provision an administrator as described in the production guide.

## Verification

```sh
cd foody-backend
mvn test
mvn package -DskipTests
```

Docker must be available for MySQL integration tests, which migrate a fresh schema.
If this environment prevents Mockito attaching its agent, use the installed Byte Buddy agent
with Maven's `-DargLine=-javaagent:/absolute/path/to/byte-buddy-agent.jar` option.

```sh
cd foody-frontend
npm test
VITE_API_BASE_URL=https://your-api.onrender.com npm run build
```

The frontend tests use Node's built-in runner and include session/API behavior and UI contract
checks; they are not a live browser acceptance suite. PWA/mobile implementation is a separate phase.
