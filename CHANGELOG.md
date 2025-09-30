# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to SemVer.

## [Unreleased]
### Added
- Prepare development for v0.2.0-SNAPSHOT (version bump in backend).
- GitHub/GitLab webhooks: HMAC signature verification and payload normalization to internal contract.
- Idempotency via Redis (with in-memory fallback) and basic per-origin rate-limit.
### Planned
- Roadmap items for v0.2.0 and v0.3.0.
### Docs
- Added `docs/ROADMAP.md` com checklist do roadmap; regra de marcação com [x] + data (AAAA-MM-DD) e anotação opcional.
- README com link para o roadmap (seção Roadmap).
- Workflow `roadmap-issues.yml` para criar issues a partir do checklist (workflow_dispatch).
- Atualizado `docs/schema.sql` (placeholder) após nova migration base (V1__base_schema.sql).
- OpenAPI sincronizado para `openapi/openapi.json` (usando spec da última release enquanto backend está em SNAPSHOT).
- README: versões sincronizadas (Java, Spring Boot).
- Adicionados exemplos HTTP para webhooks assinados (GitHub/GitLab) em `docs/examples/`.
- README: adição de snippets curl para webhooks assinados (GitHub/GitLab).
 - Documentação de Redis + propriedades (`eod.rateLimit.perOriginPerMinute`, `eod.idempotency.ttlSeconds`).

## [0.1.0] - 2025-09-28
### Added
- Spring Boot backend (Java 21) with tasks, DoD policies, evidence ingestion.
- Webhook endpoints (GitHub, observability, flags) and DoD evaluation.
- Flyway migrations, OpenAPI config, Correlation-Id filter.
- CI workflows, PR template, CODEOWNERS, issue templates.
- Docker Compose for Postgres/Redis (dev).

[Unreleased]: https://github.com/vinicius-ssantos/todo-eod-mvp-v0.1/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/vinicius-ssantos/todo-eod-mvp-v0.1/releases/tag/v0.1.0
