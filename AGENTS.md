# Repository Guidelines

## Project Structure & Module Organization
- Root services: `docker-compose.yml` (Postgres/Redis), CI in `.github/workflows`.
- Backend (Spring Boot): `backend/`
  - Source: `backend/src/main/java/com/todo/eod/{app,domain,infra,web}`
  - Resources: `backend/src/main/resources` (Flyway in `db/migration`, policies in `policies/`)
  - Tests: `backend/src/test/java`

## Build, Test, and Development Commands
- Start dependencies (dev): `docker compose up -d`
- Build & test: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`)
- Run locally: `cd backend && ./mvnw spring-boot:run`
- Run tests only: `cd backend && ./mvnw test`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Coding Style & Naming Conventions
- Java 21, Spring Boot 3.x, 4-space indentation, UTF-8, Unix line endings preferred.
- Packages: `com.todo.eod.{domain,app,infra,web}`.
- Suffixes: controllers `*Controller`, services `*Service`, repositories `*Repository`, DTOs `*Request`/`*Response`, mappers `*Mapper`.
- Enums UPPER_SNAKE (e.g., `PR_MERGED`), constants UPPER_SNAKE, fields camelCase, classes PascalCase.
- Use Lombok for boilerplate and MapStruct for DTO mapping where present; keep constructors minimal.

## Testing Guidelines
- Frameworks: JUnit 5 + Mockito.
- Location: mirror package under `backend/src/test/java`.
- Naming: `ClassNameTest` and descriptive test methods.
- Add tests for new logic (happy path + edge cases). Keep unit tests fast; avoid external services.
- Run: `./mvnw test` before pushing.

## Commit & Pull Request Guidelines
- Prefer Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- Commits: small, focused, with imperative subject and brief body when needed.
- PRs: include purpose, summary of changes, testing notes, and screenshots/logs when relevant. Link issues (e.g., `Closes #123`).

## Security & Configuration Tips
- Do not commit secrets. Configure DB via env vars; see `application.yml` defaults.
- Maintain Flyway migrations: add new files as `V{N}__short_desc.sql` in `db/migration`.
- Correlation IDs: propagate/use `Correlation-Id` header; avoid logging sensitive data.

## Agent-Specific Tips
- Keep changes minimal and localized; follow structure above.
- Update docs/tests alongside code. Run `./mvnw verify` before proposing changes.
- When adding endpoints, place DTOs in `web/dto`, mapping in `web/mapper`, business logic in `app`.
