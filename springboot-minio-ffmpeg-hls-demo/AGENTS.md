# Repository Guidelines

## Project Structure & Module Organization

The Spring Boot sources live in `src/main/java/com/example/videodemo`, split into `config` for MinIO and web adapters,
`service` for FFmpeg workflows, and `web` for REST endpoints. Static assets and the upload UI sit in
`src/main/resources/static`, while runtime configuration is defined in `src/main/resources/application.yml`. Generated
HLS variants land in `storage/public/vod`; `/vod/**` serves them, so keep the directory untracked. Maven build artefacts
are written to `target/`, and new tests belong in `src/test/java` mirroring the main package path.

## Build, Test, and Development Commands

- `docker compose up -d` brings up the local MinIO instance expected by the app.
- `mvn spring-boot:run` launches the API and static UI on `http://localhost:8080`.
- `mvn clean package` produces an executable jar in `target/`.
- `mvn test` executes the unit and integration test suite.
  Confirm `ffmpeg -version` works before testing ingest.

## Coding Style & Naming Conventions

Target Java 17 with four-space indentation and UTF-8 source files. Keep package names lowercase, class names in
UpperCamelCase (`TranscodeService`), and methods or variables in lowerCamelCase. Controllers should remain lean entry
points delegating to services; new services should follow the existing `*Service` naming. Favor descriptive method names
and guard resource handling with try-with-resources.

## Testing Guidelines

The project depends on `spring-boot-starter-test` (JUnit 5 + Mockito). Place tests under
`src/test/java/com/example/videodemo/...` and name classes `*Tests` to align with Maven defaults. Unit-test pure logic (
e.g., path normalization helpers in `TranscodeService`) and mock `MinioClient` for service tests. For end-to-end
coverage, spin up MinIO, upload a small fixture video, and assert that `master.m3u8` plus `v0/v1/v2` playlists exist.
Run `mvn test` locally before submitting changes.

## Commit & Pull Request Guidelines

Recent commits are concise, Chinese summaries (e.g., “图片和视频”); keep that brevity but add context, ideally
`<module>: <change>` such as `service: 调整转码码率`. Group related changes per commit. Pull requests should include a
high-level summary, referenced issues, manual test notes, and screenshots or logs when touching the upload UI or FFmpeg
pipeline. Highlight configuration updates (MinIO endpoint, FFmpeg flags) so reviewers can reproduce them.

## Configuration & Ops Tips

Default credentials and endpoints live in `application.yml`; override them with environment variables such as
`APP_MINIO_ENDPOINT`. Regenerate `storage/public/vod` outputs rather than committing them. When adjusting FFmpeg
profiles, document bitrate and resolution choices in the PR to keep the transcode pipeline predictable.
