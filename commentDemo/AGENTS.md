# Repository Guidelines

## Project Structure & Module Organization
The Spring Boot entry point lives in `src/main/java/com/example/commentdemo`. Comment features are grouped under `comment/...`: `api` holds request/response DTOs, `service` handles orchestration, `repository` wraps R2DBC access, and `web` exposes reactive controllers. Configuration helpers sit in `comment/config` and `comment/security`. Shared assets and contracts are in `src/main/resources` (`application.yml`, `comment.yaml`, `static/index.html`). Tests mirror the main layout inside `src/test/java/com/example/commentdemo`.

## Build, Test, and Development Commands
Use Maven directly (`mvn`; no wrapper is committed). Run `mvn clean compile` to verify compilation and annotation processing. Execute `mvn test` for the full JUnit 5 suite. Start the service with `mvn spring-boot:run`, which boots WebFlux with the R2DBC Postgres profile defined in `application.yml`. Override connection settings via environment variables such as `SPRING_R2DBC_URL` when your local database differs.

## Coding Style & Naming Conventions
Code targets Java 17+ and Spring Boot 3.5. Keep four-space indentation, expressive method names, and line lengths under roughly 120 characters. Stick to the existing package boundaries (e.g., new command endpoints in `comment/web`). Classes use PascalCase; DTOs end with `Request` or `Response`; repositories end with `Repository`. Lombok is available—prefer constructor injection and annotate only when it improves readability. Format code with your IDE or the configured formatter before committing.

## Testing Guidelines
Testing relies on JUnit 5 (`spring-boot-starter-test`) and `reactor-test`. Name test classes with the `*Tests` suffix and place them under the mirrored package path. Reserve `@SpringBootTest` for full-stack integration checks; prefer slice tests such as `@WebFluxTest` or `@DataR2dbcTest` for narrower coverage. Use `StepVerifier` assertions for reactive flows. Run `mvn test` (or a targeted `mvn -Dtest=ClassName test`) before every push.

## Commit & Pull Request Guidelines
Recent history favors concise, task-focused summaries (often in Chinese); keep the subject under ~60 characters and lead with the affected area (`comment: ...`). Reference issue IDs or links in the body when relevant. Pull requests should describe the change, call out schema or contract updates (`src/main/resources/scame.sql`, `comment.yaml`), and include screenshots or API examples when behavior changes. Confirm tests and linting pass before requesting review.

## Environment & Configuration Notes
The default profile expects PostgreSQL at `r2dbc:postgresql://localhost:5432/comment_mod` with `postgres/postgres` credentials. Keep secrets out of the repo—override via environment variables or config servers. Update `comment.yaml` whenever controller contracts change so the published OpenAPI remains accurate.
