# Repository Guidelines

## Project Structure & Module Organization
- Java sources live under `src/main/java/com/example/videodemo`, with `config` for MinIO wiring and web adapters, `service` for FFmpeg workflows, and `web` for REST controllers.
- Static assets and the upload UI reside in `src/main/resources/static`; runtime configuration is in `src/main/resources/application.yml`.
- Tests mirror the main packages at `src/test/java/com/example/videodemo`.
- Generated HLS variants are written to `storage/public/vod`; keep that directory untracked while serving it via `/vod/**`.

## Build, Test, and Development Commands
- `docker compose up -d` spins up the local MinIO instance the app expects.
- `mvn spring-boot:run` launches the API and static UI at `http://localhost:8080`.
- `mvn clean package` builds the executable JAR in `target/`.
- `mvn test` runs the full unit and integration suite; confirm `ffmpeg -version` before testing ingest flows.

## Coding Style & Naming Conventions
- Target Java 17, UTF-8 files, and four-space indentation.
- Packages remain lowercase; classes use UpperCamelCase (e.g., `TranscodeService`); methods and variables follow lowerCamelCase.
- Controllers stay lean, delegating work to services named with the `*Service` pattern.
- Favor descriptive method names and wrap resource handling with try-with-resources blocks.

## Testing Guidelines
- Rely on `spring-boot-starter-test` (JUnit 5 + Mockito); place classes as `*Tests` in `src/test/java/com/example/videodemo`.
- Mock `MinioClient` when isolating service logic.
- For end-to-end coverage, start MinIO, upload a small fixture video, and assert that `master.m3u8` and `v0/v1/v2` playlists exist in `storage/public/vod`.

## Commit & Pull Request Guidelines
- Follow the repo trend of concise Chinese summaries: `<module>: <change>` (example: `service: 调整转码码率`).
- Group related edits per commit; mention configuration or FFmpeg flag changes explicitly.
- Pull requests should provide a high-level summary, linked issues, manual test notes, and screenshots or logs for UI or FFmpeg pipeline updates.

## Configuration & Ops Tips
- Default MinIO credentials and endpoints live in `application.yml`; override via environment variables such as `APP_MINIO_ENDPOINT`.
- Regenerate and avoid committing `storage/public/vod` outputs; document bitrate or resolution changes whenever adjusting FFmpeg profiles.
