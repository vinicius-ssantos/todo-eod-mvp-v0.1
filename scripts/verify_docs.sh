#!/usr/bin/env bash
set -euo pipefail

FIX="${1:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

has() { command -v "$1" >/dev/null 2>&1; }
err() { echo "ERROR: $*" >&2; exit 1; }

# 1) OpenAPI drift
GEN_FILE="backend/target/openapi.json"
[ -f "$GEN_FILE" ] || GEN_FILE="target/openapi.json"

if [ -f "$GEN_FILE" ]; then
  mkdir -p openapi
  DEST="openapi/openapi.json"
  if [ "${FIX}" = "--fix" ]; then
    cp "$GEN_FILE" "$DEST"
    echo "OpenAPI: sincronizado $DEST"
    # manter compatibilidade com versão versionada, se existir versão atual
    if has xmlstarlet; then
      POM=backend/pom.xml
      if [ -f "$POM" ]; then
        VER=$(xmlstarlet sel -t -v 'project/version' "$POM" 2>/dev/null || true)
        BASEVER=${VER%-SNAPSHOT}
        if [ -n "$BASEVER" ]; then
          mkdir -p docs/openapi
          cp "$GEN_FILE" "docs/openapi/openapi-$BASEVER.json" || true
          echo "OpenAPI: também atualizado docs/openapi/openapi-$BASEVER.json (compat)"
        fi
      fi
    fi
  else
    if [ -f "$DEST" ]; then
      if ! diff -u "$DEST" "$GEN_FILE" >/dev/null; then
        echo "OpenAPI drift detectado (rode com --fix para sincronizar)"
        exit 1
      fi
    else
      echo "Aviso: $DEST não existe no repo."
    fi
  fi
else
  echo "Aviso: $GEN_FILE não existe (plugin springdoc gerou?)."
fi

# 2) Versões do README
has xmlstarlet || err "xmlstarlet ausente"
JAVA_VER="$(xmlstarlet sel -t -v 'project/properties/java.version' backend/pom.xml 2>/dev/null || true)"
SB_VER="$(xmlstarlet sel -t -v 'project/parent/version' backend/pom.xml 2>/dev/null || true)"
NODE_VER=""
ANG_VER=""
if [ -f package.json ]; then
  has jq || err "jq ausente"
  NODE_VER="$(jq -r '.engines?.node // empty' package.json 2>/dev/null || true)"
  ANG_VER="$(jq -r '.dependencies["@angular/core"] // empty' package.json 2>/dev/null || true)"
fi

upsert_line() {
  local key="$1"; local val="$2"
  [ -z "$val" ] && return 0
  touch README.md
  if grep -q "^${key}:" README.md; then
    sed -E -i.bak "s|^(${key}:).*|\1 ${val}|g" README.md && rm -f README.md.bak
  else
    printf "\n%s: %s\n" "${key}" "${val}" >> README.md
  fi
}

if [ "${FIX}" = "--fix" ]; then
  [ -n "$JAVA_VER" ] && upsert_line "Java" "$JAVA_VER"
  [ -n "$SB_VER" ]   && upsert_line "Spring Boot" "$SB_VER"
  [ -n "$NODE_VER" ] && upsert_line "Node" "$NODE_VER"
  [ -n "$ANG_VER" ]  && upsert_line "Angular" "$ANG_VER"
  echo "README: versões sincronizadas (se disponíveis)."
else
  OUT=0
  [ -n "$JAVA_VER" ] && (grep -Eq "^Java: .*${JAVA_VER}" README.md || OUT=1)
  [ -n "$SB_VER" ]   && (grep -Eq "^Spring Boot: .*${SB_VER}" README.md || OUT=1)
  [ -n "$NODE_VER" ] && (grep -Eq "^Node: .*${NODE_VER}" README.md || OUT=1)
  [ -n "$ANG_VER" ]  && (grep -Eq "^Angular: .*${ANG_VER}" README.md || OUT=1)
  if [ "$OUT" -ne 0 ]; then
    echo "README desatualizado (rode com --fix para ajustar)."
    exit 1
  fi
fi

# 3) DB → ERD/schema (placeholder)
if [ -d backend/src/main/resources/db/migration ] || [ -d db/migration ]; then
  mkdir -p docs
  if [ "${FIX}" = "--fix" ]; then
    echo "-- schema dump placeholder (automatizar quando houver container/db no CI) $(date -Is)" > docs/schema.sql
    echo "ERD/schema: placeholder atualizado."
  else
    [ -f docs/schema.sql ] || { echo "Aviso: docs/schema.sql ausente."; }
  fi
fi

echo "OK: verify_docs concluído."

