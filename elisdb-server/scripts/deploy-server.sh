#!/usr/bin/env bash
# Publica o código do servidor ElisDB na VPS — mas só o que o CI já testou.
#
# O repositório é privado e o plano atual do GitHub não permite exigir checks
# verdes na main (branch protection). Esta é a guarda do lado do deploy: o
# código que vai para o contentor tem de ser um commit com o "Server CI" verde.
# `--check` faz apenas a verificação, sem tocar na VPS.
set -euo pipefail

HOST="${ELISDB_DEPLOY_HOST:-vps01}"
TARGET="${ELISDB_DEPLOY_TARGET:-docker/elisdb-server}"
# Opcional: URL de /health a confirmar depois do deploy (ex.: o URL público).
HEALTH_URL="${ELISDB_HEALTH_URL:-}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

erro() { echo "Erro: $*" >&2; exit 1; }

check_only=false
assume_yes=false
for arg in "$@"; do
  case "$arg" in
    --check) check_only=true ;;
    -y|--yes) assume_yes=true ;;
    -h|--help)
      sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) erro "argumento desconhecido: $arg" ;;
  esac
done

# 1. Código por commitar ou por empurrar nunca passou pelo CI. Só interessa o
#    que é copiado (elisdb-server/): o resto do repo pode estar a meio.
[[ -z "$(git status --porcelain -- elisdb-server)" ]] ||
  erro "há alterações por commitar em elisdb-server/; faz commit (ou stash) antes de publicar"
git fetch --quiet origin main
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] ||
  erro "a main local não coincide com origin/main; faz push ou pull antes de publicar"

# 2. O último commit que mexeu no servidor tem de ter o Server CI verde.
server_sha="$(git log -1 --format=%H -- elisdb-server)"
command -v gh >/dev/null || erro "o gh (GitHub CLI) não está instalado"
runs="$(gh run list --commit "$server_sha" --limit 20 --json workflowName,status,conclusion,url)" ||
  erro "não foi possível consultar o CI (o gh está autenticado?)"

estado="$(printf '%s' "$runs" | python3 -c '
import json
import sys

runs = [r for r in json.load(sys.stdin) if r["workflowName"] == "Server CI"]
if not runs:
    print("missing|")
elif any(r["status"] != "completed" for r in runs):
    print("running|")
elif all(r["conclusion"] == "success" for r in runs):
    print("green|")
else:
    falhado = next(r for r in runs if r["conclusion"] != "success")
    print("red|" + falhado["url"])
')"

case "${estado%%|*}" in
  green) ;;
  missing) erro "o commit ${server_sha:0:7} ainda não tem nenhum run do Server CI; espera pelo CI" ;;
  running) erro "o Server CI ainda está a correr para ${server_sha:0:7}; espera que termine" ;;
  red) erro "o Server CI falhou para ${server_sha:0:7}: ${estado#red|}" ;;
  *) erro "estado do CI inesperado: $estado" ;;
esac

echo "Server CI verde para ${server_sha:0:7} — $(git log -1 --format=%s "$server_sha")"

if $check_only; then
  echo "Verificação passou; nada foi publicado (--check)."
  exit 0
fi

# 3. Só agora se toca na VPS. O compose.yaml de produção fica de fora: é local
#    (bind mount, PUID/PGID e utilizadores próprios da VPS) e não vem do repo.
if ! $assume_yes; then
  read -r -p "Publicar em $HOST:$TARGET? [s/N] " resposta
  case "$resposta" in
    [sS]|[sS][iI][mM]) ;;
    *) echo "Cancelado."; exit 1 ;;
  esac
fi

rsync -az \
  --exclude=.venv --exclude=__pycache__ --exclude='*.pyc' --exclude=.pytest_cache \
  --exclude=compose.yaml \
  --exclude='*.db' --exclude='*.db-*' --exclude='*.config.json' \
  --exclude=photos/ --exclude=data/ --exclude='.env' --exclude='.env.*' \
  --exclude='*.log' \
  elisdb-server/ "$HOST:$TARGET/"

# Back up the running server before the new image runs its startup migrations.
if ssh "$HOST" "cd '$TARGET' && docker compose ps --status running -q elisdb-server | grep -q ."; then
  echo "A criar backup pré-deploy..."
  ssh "$HOST" "cd '$TARGET' && docker compose exec -T elisdb-server python -m elisdb.backup create /app/data/backups/pre-deploy-$(date +%s)"
else
  echo "Sem contentor em execução: backup pré-deploy ignorado (primeiro deploy)."
fi

ssh "$HOST" "cd '$TARGET' && docker compose up -d --build"

if [[ -n "$HEALTH_URL" ]]; then
  sleep 2
  curl -fsS "$HEALTH_URL"
  echo
fi

echo "Publicado. Estado: ssh $HOST 'cd $TARGET && docker compose ps'"
