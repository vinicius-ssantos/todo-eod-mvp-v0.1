# TODO EoD — MVP (v0.1)

Um TODO app orientado a **Evidence‑of‑Done (EoD)**: tarefas avançam de estado somente quando **evidências objetivas** chegam via **webhooks** (PR mergeado, CI verde, doc publicada, log em produção com `correlationId`, feature‑flag ≥ X%).

## Quickstart

1) Suba Postgres e Redis:
```bash
docker compose up -d
```
2) Rode o backend (Java 21): 
```bash
cd backend
./mvnw spring-boot:run
```
3) Teste:
```bash
# Criar task
curl -sS -X POST http://localhost:8080/tasks -H "Content-Type: application/json" -d '{
  "key":"TSK-123",
  "title":"Implementar /login com Correlation-Id",
  "dodPolicyId":"default-java-service",
  "assignee":"vinicius",
  "labels":["security","observability"],
  "correlationId":"a6f6e9f8-5c3c-4f1a-8d9a-1b2c3d4e5f60"
}' | jq .

# Enviar webhook de PR_MERGED
curl -sS -X POST http://localhost:8080/webhooks/github -H "Content-Type: application/json" -d '{
  "eventId":"gh-evt-123",
  "type":"PR_MERGED",
  "repo":"org/app",
  "branch":"main",
  "pr":42,
  "taskKey":"TSK-123"
}' | jq .
```

OpenAPI UI: http://localhost:8080/swagger-ui/index.html

---

## Estrutura
- `backend/` — Spring Boot + Postgres + Redis + Flyway + Springdoc
- `docker-compose.yml` — Postgres/Redis para dev
- `.github/workflows/ci.yml` — CI básico (build + testes)

## Notas de Arquitetura
- Idempotência por `webhook_inbox.event_id`.
- `Correlation-Id` obrigatório nas tasks; propagado para logs via filtro.
- DoD Policy declarativa: YAML carregado em `dod_policy.spec (jsonb)`.
- Avaliação automática a cada Evidence recebida.
