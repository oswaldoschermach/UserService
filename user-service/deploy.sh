#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
ENV_FILE="$ROOT_DIR/.env"
JAR_FILE="$ROOT_DIR/app.jar"
DEFAULT_JWT_SECRET="dGVzdFNlY3JldEtleUZvckp3dEF1dGgxMjM0NTY3ODkwMTIzNDU2Nzg5MA=="
DEFAULT_DB_PASSWORD="postgres"
PLACEHOLDER_JWT_SECRET="gere_uma_chave_base64_forte"
PLACEHOLDER_DB_PASSWORD="troque_por_uma_senha_forte"

log() {
  printf '[deploy] %s\n' "$1"
}

fail() {
  printf '[deploy] erro: %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "comando obrigatorio nao encontrado: $1"
}

read_env_value() {
  local key="$1"
  local value
  value="$(grep -E "^${key}=" "$ENV_FILE" 2>/dev/null | tail -n 1 | cut -d '=' -f 2- || true)"
  printf '%s' "$value"
}

wait_for_app_health() {
  local app_id status attempt
  app_id="$(docker compose -f "$COMPOSE_FILE" ps -q app)"
  [[ -n "$app_id" ]] || fail "container da aplicacao nao encontrado"

  for attempt in $(seq 1 60); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$app_id")"
    if [[ "$status" == "healthy" ]]; then
      log "aplicacao saudavel"
      return 0
    fi
    if [[ "$status" == "unhealthy" ]]; then
      docker compose -f "$COMPOSE_FILE" logs --no-color --tail=200 app
      fail "aplicacao ficou unhealthy"
    fi
    sleep 2
  done

  docker compose -f "$COMPOSE_FILE" logs --no-color --tail=200 app
  fail "timeout aguardando healthcheck da aplicacao"
}

require_command docker
docker compose version >/dev/null 2>&1 || fail "docker compose nao esta disponivel"
[[ -f "$JAR_FILE" ]] || fail "app.jar nao encontrado. Este bundle precisa conter o jar da aplicacao."
[[ -f "$ENV_FILE" ]] || fail ".env nao encontrado. Este bundle agora usa apenas o .env."

JWT_SECRET_VALUE="$(read_env_value JWT_SECRET)"
DB_PASSWORD_VALUE="$(read_env_value DB_PASSWORD)"
APP_EXTERNAL_PORT_VALUE="$(read_env_value APP_EXTERNAL_PORT)"
PUBLIC_BASE_URL_VALUE="$(read_env_value PUBLIC_BASE_URL)"

[[ -n "$APP_EXTERNAL_PORT_VALUE" ]] || APP_EXTERNAL_PORT_VALUE="18743"

if [[ -z "$JWT_SECRET_VALUE" || "$JWT_SECRET_VALUE" == "$DEFAULT_JWT_SECRET" || "$JWT_SECRET_VALUE" == "$PLACEHOLDER_JWT_SECRET" ]]; then
  fail "JWT_SECRET ainda esta vazio ou usando o valor padrao. Ajuste o .env antes do deploy."
fi

if [[ -z "$DB_PASSWORD_VALUE" || "$DB_PASSWORD_VALUE" == "$DEFAULT_DB_PASSWORD" || "$DB_PASSWORD_VALUE" == "$PLACEHOLDER_DB_PASSWORD" ]]; then
  fail "DB_PASSWORD ainda esta vazio ou usando o valor padrao. Ajuste o .env antes do deploy."
fi

if [[ -z "$PUBLIC_BASE_URL_VALUE" || "$PUBLIC_BASE_URL_VALUE" == "http://localhost:18743" || "$PUBLIC_BASE_URL_VALUE" == "http://SEU_IP_OU_DOMINIO:18743" ]]; then
  log "aviso: PUBLIC_BASE_URL nao foi definido. O Swagger continua funcionando pela origem atual do navegador."
fi

log "iniciando build e deploy"
docker compose -f "$COMPOSE_FILE" up -d --build --remove-orphans

log "aguardando healthcheck da aplicacao"
wait_for_app_health

log "deploy concluido"
log "api: http://<host>:${APP_EXTERNAL_PORT_VALUE}"
log "swagger: http://<host>:${APP_EXTERNAL_PORT_VALUE}/swagger-ui.html"
log "health: http://<host>:${APP_EXTERNAL_PORT_VALUE}/actuator/health"

if [[ -n "$PUBLIC_BASE_URL_VALUE" ]]; then
  log "public_base_url: ${PUBLIC_BASE_URL_VALUE}"
fi

log "jar utilizado: ${JAR_FILE}"
