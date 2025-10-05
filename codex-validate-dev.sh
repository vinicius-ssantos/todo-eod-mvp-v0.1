#!/usr/bin/env bash
set -euo pipefail

# ---------- Config ----------
: "${SERVER_PORT:=8080}"
: "${SPRING_DATASOURCE_URL:=jdbc:postgresql://localhost:5432/todoeod}"
: "${SPRING_DATASOURCE_USERNAME:=todo}"
: "${SPRING_DATASOURCE_PASSWORD:=todo}"
: "${DOD_POLICY_ID:=00000000-0000-0000-0000-000000000001}"
: "${SPRING_DATA_REDIS_HOST:=localhost}"
: "${SPRING_DATA_REDIS_PORT:=6379}"

DB_CONTAINER_NAME=${DB_CONTAINER_NAME:-codex-validate-dev-db}
DB_IMAGE=${DB_IMAGE:-postgres:16.2}
DB_STARTED=""

REDIS_CONTAINER_NAME=${REDIS_CONTAINER_NAME:-codex-validate-redis}
REDIS_IMAGE=${REDIS_IMAGE:-redis:7}
REDIS_STARTED=""

ROOT_DIR=$(pwd)
APP_LOG="$ROOT_DIR/.codex-app.log"
EMBEDDED_PG_LOG="$ROOT_DIR/.codex-embedded-pg.log"
EMBEDDED_REDIS_LOG="$ROOT_DIR/.codex-embedded-redis.log"

cleanup(){
  kill_app
  if [[ "$DB_STARTED" = "docker" ]]; then
    docker rm -f "$DB_CONTAINER_NAME" >/dev/null 2>&1 || true
  elif [[ "$DB_STARTED" = "embedded" ]]; then
    if [[ -n "${EMBEDDED_PG_PID:-}" ]]; then
      kill "$EMBEDDED_PG_PID" >/dev/null 2>&1 || true
      wait "$EMBEDDED_PG_PID" >/dev/null 2>&1 || true
    fi
  fi
  if [[ "$REDIS_STARTED" = "docker" ]]; then
    docker rm -f "$REDIS_CONTAINER_NAME" >/dev/null 2>&1 || true
  elif [[ "$REDIS_STARTED" = "embedded" ]]; then
    if [[ -n "${EMBEDDED_REDIS_PID:-}" ]]; then
      kill "$EMBEDDED_REDIS_PID" >/dev/null 2>&1 || true
      wait "$EMBEDDED_REDIS_PID" >/dev/null 2>&1 || true
    fi
    pkill -f "redis-server.*:$SPRING_DATA_REDIS_PORT" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

# ---------- Funções ----------
port_open(){ (echo > /dev/tcp/127.0.0.1/$1) >/dev/null 2>&1; }
wait_port(){ local p="$1"; for i in {1..60}; do port_open "$p" && return 0 || sleep 1; done; return 1; }
kill_app(){
  if [[ -n "${APP_PID:-}" ]]; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
    unset APP_PID
  fi
  pkill -f 'org.springframework.boot.loader.JarLauncher|spring-boot:run' >/dev/null 2>&1 || true
}

parse_jdbc(){
  python - <<'PY' "$SPRING_DATASOURCE_URL"
import sys
url = sys.argv[1]
prefix = "jdbc:postgresql://"
if not url.startswith(prefix):
    print("", "", "", sep="\n")
    raise SystemExit
rest = url[len(prefix):]
host_port, _, db = rest.partition("/")
if not host_port:
    host_port = "localhost"
if ":" in host_port:
    host, port = host_port.split(":", 1)
else:
    host, port = host_port, "5432"
if not db:
    db = "postgres"
print(host)
print(port)
print(db)
PY
}

ensure_local_postgres(){
  mapfile -t _jdbc <<<"$(parse_jdbc)"
  DB_HOST=${_jdbc[0]:-localhost}
  DB_PORT=${_jdbc[1]:-5432}
  DB_NAME=${_jdbc[2]:-postgres}

  if [[ "$DB_HOST" != "localhost" && "$DB_HOST" != "127.0.0.1" ]]; then
    echo "==> Pulando provisionamento do Postgres (host $DB_HOST)"
    return
  fi

  if port_open "$DB_PORT"; then
    echo "==> Postgres já disponível em $DB_HOST:$DB_PORT"
    return
  fi

  if command -v docker >/dev/null 2>&1; then
    echo "==> Subindo Postgres via Docker ($DB_CONTAINER_NAME)"
    docker rm -f "$DB_CONTAINER_NAME" >/dev/null 2>&1 || true
    if ! docker run -d --name "$DB_CONTAINER_NAME" \
      -e POSTGRES_USER="$SPRING_DATASOURCE_USERNAME" \
      -e POSTGRES_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
      -e POSTGRES_DB="$DB_NAME" \
      -p "$DB_PORT":5432 "$DB_IMAGE" >/dev/null; then
      echo "FALHA: não foi possível iniciar o container do Postgres" >&2
      exit 4
    fi
    DB_STARTED="docker"

    echo "==> Esperando Postgres em $DB_HOST:$DB_PORT..."
    if ! wait_port "$DB_PORT"; then
      echo "FALHA: Postgres não abriu a porta $DB_PORT" >&2
      docker logs "$DB_CONTAINER_NAME" || true
      exit 5
    fi
    sleep 2
    return
  fi

  start_embedded_postgres
}

ensure_local_redis(){
  REDIS_HOST=${SPRING_DATA_REDIS_HOST}
  REDIS_PORT=${SPRING_DATA_REDIS_PORT}

  if [[ "$REDIS_HOST" != "localhost" && "$REDIS_HOST" != "127.0.0.1" ]]; then
    echo "==> Pulando provisionamento do Redis (host $REDIS_HOST)"
    return
  fi

  if port_open "$REDIS_PORT"; then
    echo "==> Redis já disponível em $REDIS_HOST:$REDIS_PORT"
    return
  fi

  if command -v docker >/dev/null 2>&1; then
    echo "==> Subindo Redis via Docker ($REDIS_CONTAINER_NAME)"
    docker rm -f "$REDIS_CONTAINER_NAME" >/dev/null 2>&1 || true
    if ! docker run -d --name "$REDIS_CONTAINER_NAME" -p "$REDIS_PORT":6379 "$REDIS_IMAGE" >/dev/null; then
      echo "FALHA: não foi possível iniciar o container do Redis" >&2
      exit 7
    fi
    REDIS_STARTED="docker"
    echo "==> Esperando Redis em $REDIS_HOST:$REDIS_PORT..."
    if ! wait_port "$REDIS_PORT"; then
      echo "FALHA: Redis não abriu a porta $REDIS_PORT" >&2
      docker logs "$REDIS_CONTAINER_NAME" || true
      exit 8
    fi
    sleep 2
    return
  fi

  start_embedded_redis
}

start_embedded_redis(){
  echo "==> Subindo Redis embutido (porta $SPRING_DATA_REDIS_PORT)"
  : > "$EMBEDDED_REDIS_LOG"
  (
    cd backend
    EMBEDDED_REDIS_PORT="$SPRING_DATA_REDIS_PORT" \
    ./mvnw -q -DskipTests \
      -Dexec.classpathScope=test \
      -Dexec.cleanupDaemonThreads=false \
      -Dexec.mainClass=com.todo.eod.devinfra.EmbeddedRedisRunner \
      test-compile exec:java > "$EMBEDDED_REDIS_LOG" 2>&1
  ) &
  EMBEDDED_REDIS_PID=$!
  REDIS_STARTED="embedded"

  echo "==> Esperando Redis embutido em $SPRING_DATA_REDIS_HOST:$SPRING_DATA_REDIS_PORT..."
  if ! wait_port "$SPRING_DATA_REDIS_PORT"; then
    echo "FALHA: Embedded Redis não abriu a porta $SPRING_DATA_REDIS_PORT" >&2
    tail -n 200 "$EMBEDDED_REDIS_LOG" >&2 || true
    exit 9
  fi
  sleep 2
}

start_embedded_postgres(){
  echo "==> Subindo Postgres embutido (porta $DB_PORT)"
  : > "$EMBEDDED_PG_LOG"
  (
    cd backend
    EMBEDDED_PG_PORT="$DB_PORT" \
    EMBEDDED_PG_DB="$DB_NAME" \
    EMBEDDED_PG_USER="$SPRING_DATASOURCE_USERNAME" \
    EMBEDDED_PG_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
    ./mvnw -q -DskipTests \
      -Dexec.classpathScope=test \
      -Dexec.cleanupDaemonThreads=false \
      -Dexec.mainClass=com.todo.eod.devinfra.EmbeddedPostgresRunner \
      test-compile exec:java > "$EMBEDDED_PG_LOG" 2>&1
  ) &
  EMBEDDED_PG_PID=$!
  DB_STARTED="embedded"

  echo "==> Esperando Postgres embutido em $DB_HOST:$DB_PORT..."
  if ! wait_port "$DB_PORT"; then
    echo "FALHA: Embedded Postgres não abriu a porta $DB_PORT" >&2
    tail -n 200 "$EMBEDDED_PG_LOG" >&2 || true
    exit 6
  fi
  sleep 2
}

json_field_py(){  # json_field_py '{"k":"v"}' k
  python - "$1" "$2" <<'PY'
import sys, json
doc=sys.argv[1]; key=sys.argv[2]
print(json.loads(doc)[key])
PY
}

generate_uuid(){
  python - <<'PY'
import uuid
print(uuid.uuid4())
PY
}

retry_curl(){
  local attempts=${RETRY_ATTEMPTS:-10}
  local delay=${RETRY_DELAY:-1}
  local tmp_out tmp_err i
  tmp_out=$(mktemp)
  tmp_err=$(mktemp)
  for ((i=1; i<=attempts; i++)); do
    if curl -fsS "$@" >"$tmp_out" 2>"$tmp_err"; then
      cat "$tmp_out"
      rm -f "$tmp_out" "$tmp_err"
      return 0
    fi
    sleep "$delay"
  done
  cat "$tmp_err" >&2
  rm -f "$tmp_out" "$tmp_err"
  return 1
}

# ---------- Start app (auth OFF) ----------
ensure_local_redis
ensure_local_postgres
kill_app
echo "==> Subindo app (auth OFF)"; rm -f "$APP_LOG"
(
  cd backend
  ./mvnw -q spring-boot:run \
    -Dserver.port="$SERVER_PORT" \
    -Dspring.datasource.url="$SPRING_DATASOURCE_URL" \
    -Dspring.datasource.username="$SPRING_DATASOURCE_USERNAME" \
    -Dspring.datasource.password="$SPRING_DATASOURCE_PASSWORD" \
    -Dspring.data.redis.host="$SPRING_DATA_REDIS_HOST" \
    -Dspring.data.redis.port="$SPRING_DATA_REDIS_PORT" \
    > "$APP_LOG" 2>&1
) &
APP_PID=$!

echo "==> Esperando porta $SERVER_PORT..."
wait_port "$SERVER_PORT" || { echo "FALHA: porta $SERVER_PORT não abriu"; tail -n 200 "$APP_LOG"; exit 1; }
sleep 3

# ---------- Health / OpenAPI ----------
echo "==> Health"
HEALTH_JSON=$(RETRY_ATTEMPTS=30 RETRY_DELAY=1 retry_curl "http://localhost:$SERVER_PORT/actuator/health")
echo "$HEALTH_JSON" | tee .health.json >/dev/null
echo "==> OpenAPI (trecho)"
OPENAPI_JSON=$(retry_curl "http://localhost:$SERVER_PORT/v3/api-docs")
python - "$OPENAPI_JSON" <<'PY'
import json, sys
doc = json.loads(sys.argv[1])
snippet = {
    "openapi": doc.get("openapi"),
    "title": doc.get("info", {}).get("title"),
    "version": doc.get("info", {}).get("version"),
    "paths": len(doc.get("paths", {})),
}
print(json.dumps(snippet, indent=2))
PY

# ---------- Criar task ----------
CID=$(generate_uuid)
TASK_PAYLOAD=$(cat <<JSON
{"key":"TSK-$(printf %04d $RANDOM)","title":"E2E DEV","dodPolicyId":"$DOD_POLICY_ID","assignee":"vinicius","labels":["backend","test"],"correlationId":"$CID"}
JSON
)
echo "==> Criando task"
CREATE_RES=$(retry_curl -H "Content-Type: application/json" -d "$TASK_PAYLOAD" "http://localhost:$SERVER_PORT/tasks")
echo "$CREATE_RES" | tee .task.json
TASK_ID=$(json_field_py "$CREATE_RES" id)
TASK_KEY=$(json_field_py "$CREATE_RES" key)

# ---------- Enviar evidências mínimas ----------
echo "==> Observability: LOG_SEEN"
retry_curl -H "Content-Type: application/json" -d "{\"eventId\":\"evt-$RANDOM\",\"type\":\"LOG_SEEN\",\"message\":\"StartedApp\",\"correlationId\":\"$CID\",\"taskKey\":\"$TASK_KEY\"}" \
  "http://localhost:$SERVER_PORT/webhooks/observability" | cat

echo "==> Observability: DOC_PUBLISHED"
retry_curl -H "Content-Type: application/json" -d "{\"eventId\":\"evt-$RANDOM\",\"type\":\"DOC_PUBLISHED\",\"url\":\"https://docs.meuapp.io/$TASK_KEY\",\"taskKey\":\"$TASK_KEY\"}" \
  "http://localhost:$SERVER_PORT/webhooks/observability" | cat

echo "==> Flags: 55% em feature.$TASK_KEY"
retry_curl -X PUT -H "Content-Type: application/json" \
  -d '{"percentage":55}' \
  "http://localhost:$SERVER_PORT/flags/feature.$TASK_KEY" | cat

echo "==> GitHub simplificado: PR_MERGED"
retry_curl -H "Content-Type: application/json" \
  -d "{\"eventId\":\"evt-$RANDOM\",\"type\":\"PR_MERGED\",\"repo\":\"org/app\",\"branch\":\"main\",\"pr\":42,\"taskKey\":\"$TASK_KEY\"}" \
  "http://localhost:$SERVER_PORT/webhooks/github" | cat

echo "==> GitHub simplificado: CI_GREEN"
retry_curl -H "Content-Type: application/json" \
  -d "{\"eventId\":\"evt-$RANDOM\",\"type\":\"CI_GREEN\",\"repo\":\"org/app\",\"workflow\":\"build.yml\",\"branch\":\"main\",\"taskKey\":\"$TASK_KEY\"}" \
  "http://localhost:$SERVER_PORT/webhooks/github" | cat

# ---------- Verificar DONE ----------
echo "==> Conferindo tarefa"
TASK_GET=$(retry_curl "http://localhost:$SERVER_PORT/tasks/$TASK_ID")
echo "$TASK_GET" | tee .task_get.json
STATE=$(python - "$TASK_GET" <<'PY'
import json, sys
doc=json.loads(sys.argv[1])
print(doc.get("state"))
PY
)
DOD_COMPLETE=$(python - "$TASK_GET" <<'PY'
import json, sys
doc=json.loads(sys.argv[1])
print(doc.get("dod",{}).get("complete"))
PY
)

echo "STATE=$STATE / DOD_COMPLETE=$DOD_COMPLETE"
[ "$STATE" = "DONE" ] && [ "$DOD_COMPLETE" = "True" ] || { echo "FALHA: DoD não completo"; exit 2; }

echo "==> DEV validation: PASS ✅"
