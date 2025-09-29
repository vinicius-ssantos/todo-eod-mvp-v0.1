# Title & Branch

<!-- Title format: type(scope): short summary  -->
<!-- Branch format: type/short-kebab-summary (e.g., feat/add-agents-md) -->

# Summary

<!-- Briefly describe the purpose and scope of this PR. -->

# Context & Links

<!-- Link related issues and docs. Use keywords: Closes #123, Relates to #456. -->

# Changes

<!-- High-level list of changes. Keep it concise. -->
- 

# How to Test

<!-- Exact steps/commands to verify. Include sample requests if relevant. -->
```bash
# Start dependencies (dev)
docker compose up -d

# Build & run backend
cd backend
./mvnw verify
./mvnw spring-boot:run
```

Optional quick checks:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Example requests: see `api.http`

# Screenshots / Logs (optional)

<!-- Paste images or logs that demonstrate behavior. -->

---

## Objetivo
Sincronizar documentação e contratos.

## Checklist (Docs)
- [ ] OpenAPI regenerado (`mvn ... springdoc ... generate`)
- [ ] Lint OK (`redocly lint`)
- [ ] README com versões atualizadas
- [ ] ERD/schema atualizado (placeholder)
- [ ] Exemplos HTTP verificados (se aplicável)

## Checklist
- [ ] Commit messages follow Conventional Commits
- [ ] PR title follows `type(scope): summary`
- [ ] Branch name follows `type/short-kebab-summary`
- [ ] CHANGELOG updated (Unreleased) with user-facing changes
- [ ] Clear summary and context provided
- [ ] Linked issues (e.g., Closes #123)
- [ ] Tests added/updated and pass `./mvnw verify`
- [ ] Docs updated (README/AGENTS.md)
- [ ] OpenAPI updated and versioned (docs/openapi/openapi-${project.version}.json)
- [ ] DB migrations added if needed (Flyway `db/migration`)
- [ ] No secrets committed; logs avoid sensitive data
- [ ] Backward compatibility considered (breaking changes documented)

## Type
- [ ] feat
- [ ] fix
- [ ] docs
- [ ] test
- [ ] refactor
- [ ] chore
