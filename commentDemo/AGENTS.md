# Repository Guidelines

## Project Structure & Module Organization
The Spring Boot entry point sits in `src/main/java/com/example/commentdemo`. Comment features stay under `comment/...`: `api` contains DTOs, `service` holds business orchestration (see `DefaultReactionService` for the RESTful reaction workflow), `repository` encapsulates R2DBC access, and `web` exposes reactive controllers. Configuration helpers live in `comment/config` and `comment/security`. Shared contracts and assets live in `src/main/resources`, especially `comment.yaml` and `openapi/**` for the API definitions. Tests mirror the main layout inside `src/test/java/com/example/commentdemo`.

## Build, Test, and Development Commands
- `mvn clean compile` — verifies compilation and annotation processing.
- `mvn test` — runs the JUnit 5 + Reactor test suites.
- `mvn spring-boot:run` — boots the WebFlux service with the default Postgres R2DBC profile.
Use environment overrides such as `SPRING_R2DBC_URL` or `SPRING_R2DBC_USERNAME` when pointing at a different database. Static assets under `src/main/resources/static` are served automatically while the service runs.

## Coding Style & Naming Conventions
Target Java 17 and Spring Boot 3.5. Keep four-space indentation and line lengths roughly under 120 characters. Respect package boundaries (e.g., new reaction endpoints belong in `comment/web`). Classes use PascalCase; DTOs end with `Request` or `Response`; repositories end with `Repository`. Prefer constructor injection (Lombok’s `@RequiredArgsConstructor` is acceptable). Use your IDE formatter or `mvn fmt:format` if configured.

## Testing Guidelines
JUnit 5 (`spring-boot-starter-test`) and `reactor-test` back the suite. Name test classes with the `*Tests` suffix and mirror packages. Prefer slice annotations like `@WebFluxTest` or `@DataR2dbcTest`; reserve `@SpringBootTest` for end-to-end scenarios. Use `StepVerifier` for reactive assertions. Run `mvn test` (or `mvn -Dtest=ClassName test`) before pushing.

## Commit & Pull Request Guidelines
Commit subjects are short (<60 characters) and usually start with an affected area, e.g., `comment: align reactions API`. Reference tickets or context in the body when relevant. Pull requests should describe the change, highlight schema or OpenAPI updates (`src/main/resources/comment.yaml`, `openapi/reactions.yaml`), and attach screenshots or sample responses if behavior shifts. Confirm tests pass before requesting review.

## Security & Configuration Tips
Default connectivity assumes PostgreSQL at `r2dbc:postgresql://localhost:5432/comment_mod` with `postgres/postgres`. Never commit credentials—override via environment variables or config servers. After controller contract changes, regenerate or edit `comment.yaml` so consumers receive the latest API specification.
