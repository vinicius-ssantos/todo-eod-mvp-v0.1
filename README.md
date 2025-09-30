# TODO App — Evidence-of-Done (EoD) • MVP (v0.1)

> Um TODO onde a tarefa **só vira DONE** quando chegam **evidências objetivas** via **webhooks** (PR mergeado, CI verde, doc publicada, log com `correlationId`, feature-flag ≥ X%).  
> Repositório: `vinicius-ssantos/todo-eod-mvp-v0.1` (este repo).

![Build](https://img.shields.io/badge/build-passing-informational) ![Java](https://img.shields.io/badge/Java-21-blue) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen) ![License](https://img.shields.io/badge/license-MIT-lightgrey)

## Sumário
- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Quickstart](#quickstart)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [APIs & Docs](#apis--docs)
- [Webhooks suportados](#webhooks-suportados)
- [Webhooks assinados](#webhooks-assinados)
- [Definition of Done (DoD)](#definition-of-done-dod)
- [Observabilidade](#observabilidade)
- [Testes & CI](#testes--ci)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contribuição](#contribuição)
- [Licença](#licença)

## Visão Geral
O **EoD** automatiza a **Definition of Done**: quando uma evidência (ex.: `PR_MERGED`, `CI_PASSED`) chega por webhook, o **Evidence Service** avalia a política da task e atualiza o estado. Nada de checklist manual.

**Benefícios:** rastreabilidade, governança de mudanças, integração DevOps, e prova objetiva de “feito”.

## Arquitetura
- **Backend:** Spring Boot 3.x (Java 21), Postgres, Flyway, Spring Security (JWT), Springdoc/OpenAPI
- **Cache/Idempotência:** Redis
- **Opcional:** Kafka (Outbox)
- **Observabilidade:** logs JSON com `correlationId`, Micrometer/Prometheus/Grafana

```text
[GitHub/GitLab/CI]  [Observability]   [Feature Flags]
     │                     │                 │
   /webhooks/github   /webhooks/obs    /webhooks/flags
            \            |             /
             \         [Evidence Service]
              \             │
               └───────[API/DB]───────(Outbox→Kafka*)
```

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

3) Teste rápido (exemplos):
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

## Variáveis de Ambiente
| Variável | Default | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/todo_eod` | Postgres |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |  |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |  |
| `SPRING_REDIS_HOST` | `localhost` | Redis (idempotência/rate-limit) |
| `SPRING_REDIS_PORT` | `6379` |  |
| `APP_JWT_SECRET` | `change-me` | Assinatura JWT (dev) |
| `APP_ALLOWED_ORIGINS` | `http://localhost:4200` | CORS (se front) |

## APIs & Docs
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Endpoints principais:
- `POST /tasks` cria uma task com `dodPolicyId` e (opcional) `correlationId`
- `GET /tasks/{id}`/`GET /tasks?key=...`
- `POST /webhooks/github` recebe eventos como `PR_MERGED`
- `PUT /flags/{key}` define percentual da feature (0-100)
- `GET /flags/{key}` consulta percentual atual
- `GET /evaluations/{taskId}` histórico de avaliação de evidências (quando aplicável)

Rate-limit e idempotência:
- Limite por origem/minuto (default 600): `eod.rateLimit.perOriginPerMinute`
- TTL de idempotência em segundos (default 86400): `eod.idempotency.ttlSeconds`
- Ambos usam Redis se disponível; fallback in-memory por instância

Segurança (JWT HS256):
- Secret: `eod.security.jwt.secret` (ou env `APP_JWT_SECRET`)
- Escopos:
  - `tasks:*` para `/tasks/**` e `/flags/**`
  - `webhooks:ingest` para `/webhooks/observability` e `/webhooks/flags`
  - `/webhooks/github` e `/webhooks/gitlab` usam assinatura HMAC do provider (sem JWT)
- Token de exemplo (payload): `{ "sub":"dev", "scope":"tasks:* webhooks:ingest", "exp": <epoch> }`

Geração rápida de JWT:
- Node: `node scripts/jwt/gen-jwt.js --secret change-me --sub dev --scope "tasks:* webhooks:ingest" --exp 3600`
- Java: `javac scripts/jwt/GenJwt.java && java -cp scripts/jwt GenJwt --secret change-me --sub dev --scope "tasks:* webhooks:ingest" --exp 3600`
- Exemplo HTTP com Bearer: `docs/examples/tasks-secured.http:1`

## Webhooks suportados
| Tipo | Payload mínimo | Uso |
|---|---|---|
| `PR_MERGED` | `eventId`, `repo`, `branch`, `pr`, `taskKey` | Marca requisito “Código integrado” |
| `CI_PASSED` | `eventId`, `pipelineId`, `commit`, `taskKey` | Marca “CI verde” |
| `DOC_PUBLISHED` | `eventId`, `url`, `taskKey` | Marca “Documentação publicada” |
| `LOG_SEEN` | `eventId`, `correlationId`, `env`, `taskKey` | Marca “Log com correlationId no ambiente alvo” |
| `FF_PERCENT` | `eventId`, `featureKey`, `percent`, `taskKey` | Marca “Feature flag ≥ X%” |

> **Idempotência:** usamos `eventId` + Redis/DB para deduplicar eventos.

## Webhooks assinados
- Exemplos prontos (HTTP):
  - GitHub: `docs/examples/webhooks-github.http:1`
  - GitLab: `docs/examples/webhooks-gitlab.http:1`
- Configure os segredos em `backend/src/main/resources/application.yml:27`:
  - `eod.webhooks.github.secret`, `eod.webhooks.gitlab.secret`
- Envie os cabeçalhos de assinatura:
  - GitHub: `X-GitHub-Event`, `X-GitHub-Delivery`, `X-Hub-Signature-256`
  - GitLab: `X-Gitlab-Event`, `X-Gitlab-Event-UUID`, `X-Gitlab-Signature` (ou `X-Gitlab-Token`)
- Opcional: informe a task via `X-EOD-Task-Key: <taskKey>` ou `?taskKey=...`

Exemplos rápidos (curl, bash):

```bash
# GitHub: pull_request merged → PR_MERGED
SECRET="change-me"
BODY='{"action":"closed","number":42,"pull_request":{"merged":true,"base":{"ref":"main"},"merge_commit_sha":"abc123def4567890","html_url":"https://github.com/org/app/pull/42"},"repository":{"full_name":"org/app"}}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | sed 's/^.* /sha256=/')
curl -sS -X POST http://localhost:8080/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: gh-delivery-001" \
  -H "X-Hub-Signature-256: $SIG" \
  -H "X-EOD-Task-Key: TSK-123" \
  -d "$BODY"

# GitLab: pipeline success → CI_GREEN
SECRET="change-me"
BODY='{"object_kind":"pipeline","project":{"path_with_namespace":"org/app","web_url":"https://gitlab.com/org/app"},"object_attributes":{"status":"success","name":"build-and-test","sha":"cafebabedeadbeef00112233445566778899aabb","ref":"main"}}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)
curl -sS -X POST http://localhost:8080/webhooks/gitlab \
  -H "Content-Type: application/json" \
  -H "X-Gitlab-Event: Pipeline Hook" \
  -H "X-Gitlab-Event-UUID: gl-evt-002" \
  -H "X-Gitlab-Signature: $SIG" \
  -H "X-EOD-Task-Key: TSK-123" \
  -d "$BODY"
```

## Definition of Done (DoD)
A **política** é declarativa (ex.: armazenada em JSON/YAML no DB).  
Exemplo (conceitual):
```yaml
id: default-java-service
required:
  - type: PR_MERGED
  - type: CI_PASSED
anyOf:
  - { type: LOG_SEEN, env: "prod" }
  - { type: FF_PERCENT, min: 25 }
```
A cada webhook, o **Evidence Service** reavalia a task e atualiza o estado.

## Observabilidade
- Logs **JSON** com `correlationId` propagado desde a criação da task.
- Métricas (Micrometer): contagem por tipo de evidência, tempo de avaliação, erros de idempotência.
- Health: `GET /actuator/health` (se habilitado).

## Testes & CI
- Testes:
  ```bash
  ./mvnw test
  ```
- CI: `.github/workflows/ci.yml` faz build + testes (ajuste para rodar containers, se necessário).

## Estrutura de Pastas
```
backend/                # Spring Boot (API, domínios, webhooks)
docker-compose.yml      # Postgres + Redis para dev
.github/workflows/ci.yml
README.md
```

## Troubleshooting
- **“connection refused” Postgres/Redis** → ver `docker compose ps` e `.env`
- **Swagger não abre** → aguarde o app subir; confira `server.port`
- **Eventos duplicados** → ver `eventId`; checar Redis/estratégia de idempotência
- **UUID inválido no correlationId** → gere outro na criação da task

## Roadmap
- [ ] Salvar/editar políticas DoD via API
- [ ] Normalizar modelo “Evidence” e histórico por task
- [ ] Autenticação JWT end-to-end
- [ ] Painel web (progresso da DoD por task)
- [ ] Integrações extras (GitLab, CircleCI, LaunchDarkly)

## Contribuição
Issues/PRs são bem-vindos. Abra um PR com descrição clara, testes e atualização do README quando necessário.

## Licença
MIT. Use livremente para estudo/portfólio.

Java: 21
Spring Boot: 3.3.4
