# Roadmap (Checklist)

> Regra de marcação: ao concluir um item, troque [ ] por [x], acrescente a data no formato AAAA-MM-DD e, se necessário, uma anotação breve no final.
> Exemplo: `- [x] Criar endpoint X (2025-09-29) Ajustado payload para compatibilidade`

## v0.1 — Núcleo EoD (MVP)
- [ ] Modelar domínio e migrations Flyway: `Task`, `DodPolicy`, `Evidence`, `WebhookInbox`
- [ ] CRUD de `Tasks` e `Policies`; `evaluate()` a cada `Evidence` com transições de estado
- [ ] Webhooks mock (`/webhooks/github`, `/webhooks/observability`, `/webhooks/flags`) com idempotência por DB (`WebhookInbox`)
- [ ] OpenAPI exposta e versionada; Redocly lint; `scripts/verify_docs.sh` integrado ao CI
- [ ] Logs estruturados com `correlationId`; Actuator/health básico
- [ ] Testes: unit de `evaluate()`, E2E feliz, idempotência (reenvio de `eventId` não duplica)

## v0.2 — Integrações Reais + Segurança
- [ ] Webhooks GitHub/GitLab com verificação de assinatura (HMAC) e normalização de payload
- [ ] Mapeamento de CI green (ex.: GitHub Actions) em `Evidence`
- [ ] Provider simples de feature-flags (percentual) conectado a `FLAG_ENABLED`
- [ ] Idempotência com Redis + rate-limit básico por origem
- [ ] Autenticação JWT com escopos (`tasks:*`, `webhooks:ingest`) e autorização inicial
- [ ] Testes de contrato dos webhooks e cenários de erro (assinatura inválida, repetidos, parciais)

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
- [ ] v0.2: webhooks assinados validados; Redis ativo; JWT protege endpoints sensíveis
- [ ] v0.3: métricas visíveis; UI mostra progresso e atualiza sem refresh manual
- [ ] v0.4: Outbox garante entrega; notificações operacionais; auditoria consultável
- [ ] v0.5: políticas por projeto; RBAC fino; alertas e relatórios prontos

## Próximos Passos
- [ ] Criar épicos/milestones v0.1–v0.5 e issues vinculadas
- [ ] Detalhar v0.1 em tasks de 0.5–1 dia (entidades+migrations, serviços/repos, controllers, webhooks mock, `evaluate()`, testes, OpenAPI, guardrails)

