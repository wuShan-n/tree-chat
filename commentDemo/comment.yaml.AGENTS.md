# Comment API Split Guide

## Goal
Refactor the monolithic `src/main/resources/comment.yaml` OpenAPI document into per-module fragments while keeping a single bundled specification for tooling and publication.

## Target Layout
- `src/main/resources/comment.yaml` – root entrypoint with metadata and `$ref` links into module files.
- `src/main/resources/openapi/subjects.yaml` – subject registration, lookup, metrics.
- `src/main/resources/openapi/comments.yaml` – comment CRUD and pagination.
- `src/main/resources/openapi/reactions.yaml` – vote and emoji reactions plus summaries.
- `src/main/resources/openapi/moderation.yaml` – moderation queue, report intake, actions.
- `src/main/resources/openapi/events.yaml` – SSE stream definitions.
- `src/main/resources/openapi/components.yaml` – shared schemas, parameters, headers, responses, examples, and security schemes.

## Extraction Steps
- Keep the top-level metadata (`openapi`, `info`, `servers`, `tags`, `security`) inside `comment.yaml` and replace each concrete path definition with a `$ref` that points to the matching module file (`./openapi/<module>.yaml#/paths/...`).
- Move each path group under its module and preserve `operationId` values so generated clients stay stable.
- Relocate the monolithic `components` block into `openapi/components.yaml`, keeping shared items only once. Reference them from modules using relative pointers such as `$ref: './components.yaml#/components/schemas/Comment'`.
- Update module files to use relative `$ref`s exclusively; avoid `#/components/...` unless the target lives inside the same file.

## Bundling & Validation
- Use `comment.yaml` as the canonical entrypoint for linting and bundling (e.g., `redocly bundle src/main/resources/comment.yaml --output dist/comment.yaml`).
- Update CI to run `redocly lint` (or similar) so regressions in any fragment are caught early.

## Hygiene & Versioning
- Keep semantic versioning in `comment.yaml`; bump the patch number for structure-only reorganizations.
- Document the modular workflow (e.g., in `AGENTS.md`) so contributors know to edit the fragment that owns a route.
- Regenerate downstream artifacts (generated clients, docs) after each change to ensure `$ref` pointers resolve correctly.
