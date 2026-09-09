# Foody backend

Java 21 / Spring Boot / Maven / MySQL 8.4. See the [root setup guide](../README.md)
and [production configuration](../PRODUCTION.md) for current instructions.

Run locally with `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`.
Run the full suite with `mvn test` and Docker available; package with `mvn package -DskipTests`.
The default runtime profile is `prod`. Test resources explicitly select `tc`.
Flyway V1–V19 owns the schema; historical demo credentials are disabled by V19.

See [audit results](../PRODUCTION-AUDIT.md) for verification evidence and unresolved release prerequisites.
