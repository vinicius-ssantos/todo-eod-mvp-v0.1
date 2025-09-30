# Roadmap (Checklist)

> Regra de marcação: ao concluir um item, troque [ ] por [x], acrescente a data no formato AAAA-MM-DD e, se necessário, uma anotação breve no final.
> Exemplo: `- [x] Criar endpoint X (2025-09-29) Ajustado payload para compatibilidade`

## v0.1 — Núcleo EoD (MVP)
- [x] Modelar domínio e migrations Flyway: `Task`, `DodPolicy`, `Evidence`, `WebhookInbox` (2025-09-29) Base criada via Flyway V1__init.sql
- [x] CRUD de `Tasks` e `Policies`; `evaluate()` a cada `Evidence` com transições de estado (2025-09-29) Endpoints e serviço de avaliação implementados
- [x] Webhooks mock (`/webhooks/github`, `/webhooks/observability`, `/webhooks/flags`) com idempotência por DB (`WebhookInbox`) (2025-09-29) Ingest com dedupe por `eventId`
- [x] OpenAPI exposta e versionada; Redocly lint; `scripts/verify_docs.sh` integrado ao CI (2025-09-29) Guardrails e workflows ativos
- [x] Logs estruturados com `correlationId`; Actuator/health básico (2025-09-29) Filtro de Correlation-Id e Actuator
- [x] Testes: unit de `evaluate()`, E2E feliz, idempotência (reenvio de `eventId` não duplica) (2025-09-29) Cobertura mínima (unit + idempotência)

## v0.2 - Integrações Reais + Segurança
- [x] Webhooks GitHub/GitLab com verificação de assinatura (HMAC) e normalização de payload (2025-09-30) Assinatura HMAC validada e normalização para PR_MERGED/CI_GREEN; exemplos .http
- [x] Mapeamento de CI green (ex.: GitHub Actions) em `Evidence` (2025-09-30) `workflow_run/pipeline` → `CI_GREEN` persistido
- [x] Provider simples de feature-flags (percentual) conectado a `FLAG_ENABLED` (2025-09-30) Provider in-memory + `/flags` GET/PUT; avaliação usa `flagKey`+`minPercentage`
- [x] Idempotência com Redis + rate-limit básico por origem (2025-09-30) Redis com fallback local; 202 para duplicados; 429 para excesso
- [x] Autenticação JWT com escopos (`tasks:*`, `webhooks:ingest`) e autorização inicial (2025-09-30) Resource Server HS256; escopos aplicados em rotas
- [x] Testes de contrato dos webhooks e cenários de erro (assinatura inválida, repetidos, parciais) (2025-09-30) 401/403/429/202 cobertos; JSON inválido também

## v0.3 — Observabilidade + UI
- [ ] Métricas Micrometer: lead/cycle time, aging, % em VERIFICATION, PR_MERGED→DONE
- [ ] Dashboards Grafana “Flow” e “DoD Progress”
- [ ] UI Angular 18: lista de tasks, detalhe com progresso da DoD; atualização por polling/SSE
- [ ] Endpoint `GET /metrics/flow` para consumo pela UI e painéis

## v0.4 — Escalabilidade + Comms
- [ ] Outbox pattern + Kafka; publicar `TaskStateChanged` com garantias
- [ ] Notificações (Slack/email) nas transições e “faltas” de evidência
- [ ] Auditoria completa de transições manuais; hardening de inputs e limites de payload

## v0.5 — Governança + Produto
- [ ] Templates de DoD por projeto; UI para gerenciar políticas
- [ ] RBAC granular por projeto/label
- [ ] SLAs/alertas (stuck em VERIFICATION), relatórios exportáveis, auditoria ampliada
- [ ] Integrações de flags avançadas (ex.: LaunchDarkly) e novas fontes de evidência

## Trilhas Transversais (Contínuas)
- [ ] Guardrails de docs: OpenAPI versionada, README com versões, `docs/schema.sql`, exemplos HTTP e CHANGELOG “### Docs”
- [ ] Qualidade: ampliar suite (unit, contrato, E2E), testes de idempotência sob carga
- [ ] DevEx/CI: pipelines rápidos, builds reprodutíveis, validações Danger
- [ ] Segurança: secrets em vault, princípio do menor privilégio nas integrações

## Critérios de Aceite (Checklist por versão)
- [ ] v0.1: cenários 1–5 de aceitação executam; OpenAPI gerada e lintada; idempotência por `eventId`
- [x] v0.2: webhooks assinados validados; Redis ativo; JWT protege endpoints sensíveis (2025-09-30)
- [ ] v0.3: métricas visíveis; UI mostra progresso e atualiza sem refresh manual
- [ ] v0.4: Outbox garante entrega; notificações operacionais; auditoria consultável
- [ ] v0.5: políticas por projeto; RBAC fino; alertas e relatórios prontos

## Próximos Passos
- [ ] Criar épicos/milestones v0.1–v0.5 e issues vinculadas
- [ ] Detalhar v0.1 em tasks de 0.5–1 dia (entidades+migrations, serviços/repos, controllers, webhooks mock, `evaluate()`, testes, OpenAPI, guardrails)

## Testes — Roadmap

> Regra de marcação: ao concluir um item, troque [ ] por [x], acrescente a data no formato AAAA-MM-DD e, se necessário, uma anotação breve no final.
> Exemplo: `- [x] Criar endpoint X (2025-09-29) Ajustado payload para compatibilidade`

- Unidade — EvaluationService
  - [x] Requisitos básicos PR_MERGED/CI_GREEN (2025-09-29)
  - [x] FLAG_ENABLED com minPercentage (2025-09-29)
  - [ ] DOC_PUBLISHED com urlPattern
  - [ ] LOG_SEEN com minCount
  - [ ] Especificação inválida/JSON quebrado não explode; retorna incomplete

- Unidade — WebhookIngestService
  - [x] Idempotência por eventId (2025-09-29)
  - [x] Task inexistente → erro (2025-09-29)
  - [x] Persistir Evidence mesmo com payload não serializável (2025-09-29) armazena `{}`
  - [x] REVIEW→VERIFICATION com PR_MERGED/CI_GREEN (2025-09-29)
  - [x] DONE quando DoD completa + persistência (2025-09-29)

- Web (MockMvc)
  - [x] POST /tasks 201 + Location (2025-09-29)
  - [x] POST /tasks validação 400 (2025-09-29)
  - [ ] GET /tasks aplica filtros state/assignee/label
  - [x] GET /dod-policies lista (2025-09-29)
  - [x] POST /webhooks/github feliz (2025-09-29)
  - [ ] POST /webhooks duplicado (idempotência) retorna 202/accepted

- Integração (DB/JPA)
  - [ ] TaskRepository.search com filtros (Testcontainers Postgres)
  - [ ] ElementCollection labels persiste/consulta ok
  - [ ] jsonb em DodPolicy/Evidence persiste/consulta ok
  - [ ] Flyway aplica V1__init.sql sem erros

- E2E (Fluxo DoD)
  - [ ] Cria task → envia webhooks → DONE
  - [ ] Reenvio de eventId não duplica evidências

- Contrato (Webhooks)
  - [ ] Payload mínimo aceita/valida campos obrigatórios
  - [ ] Campos inválidos resultam em 400

- Observabilidade
  - [x] Logs JSON com correlationId no MDC (2025-09-29)
  - [ ] Métricas (quando disponíveis) expostas em /actuator/metrics
