# Foody backend

Java 21 / Spring Boot / Maven / MySQL 8.4. See the [root setup guide](../README.md)
and [production configuration](../PRODUCTION.md) for current instructions.

Run locally with `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`.
Run the full suite with `mvn test` and Docker available; package with `mvn package -DskipTests`.
The package is an executable JAR at `target/foody-backend.jar`. The dedicated VPS
selects `vps`; the default/cloud profile is `prod`; tests explicitly select `tc`.
Flyway V1–V23 owns the schema; historical demo credentials are disabled by V19.

See the [VPS runbook](../PRODUCTION.md) for current deployment instructions.
