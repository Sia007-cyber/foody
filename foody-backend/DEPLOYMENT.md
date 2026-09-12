# Foody shared Tomcat deployment

## Compatibility baseline

- **Spring Boot:** 3.5.16.
- **Java:** build and runtime require Java 21 (class-file target 21).
- **Servlet API:** Jakarta Servlet (`jakarta.servlet`), not the legacy `javax.servlet` API.
- **Tomcat:** Tomcat 10.1 or newer is required. Tomcat 9 uses `javax.servlet` and Tomcat 10.0 implements Servlet 5.0, so neither is compatible with Spring Boot 3.5's Servlet 6.0 baseline.
- **MySQL:** require MySQL 8.x. All 22 migrations were verified on MySQL 8.4. The SQL is largely accepted by MySQL 5.7, but 5.7 ignores the schema's `CHECK` constraints and is outside the current Hibernate ORM/MySQL support baseline; do not deploy Foody on 5.7.

These are application requirements, not confirmed javahosting.ir Plan 4 capabilities. Confirm all three versions with the provider before purchase/upload.

## Build and artifact

Install JDK 21 and Maven 3.6.3 or newer, then run from `foody-backend`:

```sh
mvn clean package
```

The deployable artifact is `target/foody-backend.war`. Its measured size after the verified build is **75,101,447 bytes (75.10 MB decimal / 71.62 MiB)**, safely below the 100 MB upload limit. The Boot plugin also makes this WAR runnable with `java -jar` for local diagnostics, while `WEB-INF/lib-provided` keeps the provided embedded-Tomcat core/WebSocket libraries off the external container classpath.

Prefer renaming the artifact to `foody.war` for a `/foody` context, or to `ROOT.war` only if the provider permits root-context deployment. External Tomcat normally derives the context path from the filename:

- `foody.war` -> `https://api.example.com/foody`
- `ROOT.war` -> `https://api.example.com/`

No context path is hardcoded. API routes remain relative to whichever context Tomcat assigns. Set the separately deployed frontend's API base URL to the final backend origin plus context path.

## Database preparation

In the panel, create an empty MySQL 8.x database and a least-privilege user with full DDL/DML rights on that database. Use InnoDB and `utf8mb4`; a suitable default is `utf8mb4_unicode_ci` (or the provider's supported `utf8mb4` collation). Do not ask Hibernate to create the schema.

On first startup, Flyway uses the same datasource, creates `flyway_schema_history`, validates the immutable migration checksums, and applies V1 through V22. Keep Flyway enabled. Verify the table records 22 successful versions and that the log reports schema version 22. Never edit an already-applied migration.

## Required runtime configuration

Activate `sharedhost` and supply every value below. Environment-variable form:

```text
SPRING_PROFILES_ACTIVE=sharedhost
FOODY_DB_URL=jdbc:mysql://DB_HOST:3306/DB_NAME?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&sslMode=REQUIRED
FOODY_DB_USERNAME=...
FOODY_DB_PASSWORD=...
FOODY_JWT_SECRET=...
FOODY_CORS_ALLOWED_ORIGINS=https://frontend.example.com
FOODY_STORAGE_LOCAL_PATH=/provider/persistent/foody-uploads
```

`FOODY_JWT_SECRET` must be Base64 for at least 32 random decoded bytes. Use the database TLS mode required by the provider; prefer certificate/hostname verification when its CA and hostname permit it. Multiple exact HTTPS CORS origins may be comma-separated; do not use `*`, paths, or trailing slashes because credentialed requests are enabled.

If the panel cannot set environment variables, use JVM system properties with the exact placeholders:

```text
-Dspring.profiles.active=sharedhost
-DFOODY_DB_URL=...
-DFOODY_DB_USERNAME=...
-DFOODY_DB_PASSWORD=...
-DFOODY_JWT_SECRET=...
-DFOODY_CORS_ALLOWED_ORIGINS=https://frontend.example.com
-DFOODY_STORAGE_LOCAL_PATH=/provider/persistent/foody-uploads
```

Canonical Spring properties (for example `-Dspring.datasource.url=...`) also override packaged configuration. Keep secrets out of the WAR and source control.

## Resource and storage behavior

The shared profile creates one Hikari pool with maximum 4 connections and minimum 1 idle connection. It waits 30 seconds for a connection, retires idle connections after 5 minutes, and caps connection lifetime at 30 minutes. This is conservative for initial low traffic while retaining enough concurrency for short transactions and Flyway startup.

The application can control its datasource pool, logging levels, and multipart limits. It cannot control the provider-managed Tomcat connector, maximum threads, accept queue, request timeout/size, JVM heap, access-log rotation, or container restart policy through `application.yml`. Ask the provider about those settings; a starting point is a modest Tomcat worker limit and at least 256 MB heap, but the provider must size these alongside every application in the shared JVM.

The shared profile accepts images up to 5 MB and multipart requests up to 6 MB. Provider/Tomcat/proxy limits can be lower and take precedence. These limits are unrelated to the panel's 100 MB WAR-upload limit.

`FOODY_STORAGE_LOCAL_PATH` has no default in this profile. It must identify a directory outside `webapps`, the expanded WAR, classpath, and working directory. The directory must exist or be creatable, be writable by the Tomcat OS account, survive WAR replacement/re-expansion, and be included in backups. If javahosting.ir cannot guarantee that persistence, local uploads are a deployment risk/blocker.

Plan 4 has only 4 GB total storage: monitor uploads, database allocation, and provider logs. Before upload volume becomes significant, migrate the existing `UploadStorage` integration to external S3-compatible storage such as Arvan Cloud or Liara; that migration is intentionally outside this deployment task.

The profile writes no application log file and defaults to WARN globally, INFO for Foody, and WARN for Hibernate SQL. Output goes to provider-managed Tomcat logging. Confirm log access and provider rotation/retention so logs cannot consume the account disk.

No application scheduler, runtime shell command, Render absolute path, localhost-only runtime service, or Docker dependency is used in production. The Dockerfile remains only a separate deployment option and is not used by shared Tomcat.

## Panel deployment and verification

1. Back up the database and persistent upload directory before every upgrade.
2. Create/configure MySQL and the runtime settings above.
3. Create the external persistent upload directory and grant the Tomcat account read/write permission.
4. Build the WAR and optionally rename it to `foody.war` or `ROOT.war` as agreed with the provider.
5. Upload it through the panel/FTP and deploy or restart the application from the panel. Do not put uploads beside the WAR.
6. Watch the Tomcat logs for Spring startup, a Hikari connection, successful Flyway validation/migration through V22, and the final `Started FoodyBackendApplication` message. Any migration failure should stop startup; fix connectivity/permissions rather than bypassing Flyway.
7. Verify a public endpoint at the chosen context, for example `GET /api/businesses`. `/actuator/health` is allowed by security configuration but Spring Boot Actuator is not currently packaged, so do not rely on it unless that dependency is added later.
8. Verify an authenticated API request from the configured frontend origin and a small image upload, then confirm the file appears in the persistent directory and remains after a controlled redeploy.
9. For redeployment, back up first, upload the replacement under the same WAR name, let the panel replace/expand it, restart, and verify Flyway plus the API again. Never delete the external upload directory.

## Questions to confirm with javahosting.ir before purchase

- Which Java versions are available? Is Java 21 available and selectable?
- Which Tomcat versions are available?
- Is Tomcat 10.1 or newer available?
- Is MySQL 8.x available?
- How is `SPRING_PROFILES_ACTIVE` configured?
- Can custom environment variables be configured?
- Can JVM `-D` system properties be configured?
- Is there a persistent writable directory outside `webapps`?
- Does that directory survive WAR redeployment?
- What JVM heap is allocated per application or shared JVM?
- Can heap settings be changed?
- What are the Tomcat/proxy request and upload limits?
- What is the idle/request timeout?
- Are outbound HTTPS connections allowed?
- Can the application connect to external S3-compatible storage later?
- Is HTTPS/SSL provided and terminated before Tomcat?
- How are custom domains configured?
- Are Tomcat application, error, and access logs accessible and rotated?
- Is application restart available from the panel?
- Is automatic WAR expansion used?
- Can `ROOT.war` be deployed?
- Are scheduled application/Tomcat restarts performed by the provider?
- Is the application isolated in its own JVM or sharing one, and are Tomcat thread/connector settings configurable?
- Which persistent directory path and OS permissions should be used for uploads?
- Does MySQL require TLS, and which JDBC hostname/CA settings should be used?
