# Codex – Doc Guardrails

## Quando rodar
- Sempre que houver mudanças em:
  - `srcmain**`, `srctest**`, `pom.xml`, `build.gradle`, `Dockerfile`, `openapi**`, `dbmigration**`, `package.json`, `appsweb**`.
- Em todo PR e após merge na `main`.

## O que verificar
1) **OpenAPI drift**
   - Gerar `targetopenapi.json` via plugin Maven do springdoc.
   - Comparar com `openapiopenapi.json` versionado.
   - Se diferente: substituir o versionado e rodar linter.

2) **Versões em README**
   - Extrair `java.version` e `spring-boot.version` do `pom.xml`.
   - Extrair `node` de `package.json` (em `engines.node` se houver) e `@angularcore` de `dependencies` (se houver).
   - Garantir que o README exponha essas versões em linhas no formato:
     - `Java: X`
     - `Spring Boot: Y`
     - `Node: Z`
     - `Angular: W`

3) **DB schema vs docs**
   - Se houver novas migrations, atualizar `docsschema.sql` (placeholder por enquanto) e citar no CHANGELOG.

4) **EndpointsContratos**
   - Se controllersDTOs mudarem: atualizar exemplos em `docsexamples*.http` (se existirem).

5) **Changelog**
   - Adicionar seção `### Docs` explicando as atualizações.

## Como corrigir (ordem)
- Regerar OpenAPI → Lint → sincronizar para `openapiopenapi.json`.
- Executar `scriptsverify_docs.sh --fix`.
- Atualizar exemplos e CHANGELOG.
- Abrir PR com título: `docs: sync openapireadmeerd`.
