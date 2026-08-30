#!/usr/bin/env bash
# Constrói o APK de release assinado da ElisDB.
# Usa as credenciais de ~/.keystores/elisdb-release.env
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ELISDB_KEYSTORE_ENV:-$HOME/.keystores/elisdb-release.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Erro: ficheiro de credenciais não encontrado em $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

if [[ -z "${KEYSTORE_PATH:-}" || -z "${KEYSTORE_PASSWORD:-}" || -z "${KEY_ALIAS:-}" || -z "${KEY_PASSWORD:-}" ]]; then
  echo "Erro: variáveis de signing incompletas em $ENV_FILE" >&2
  exit 1
fi

cd "$ROOT"
./gradlew :app:assembleRelease

echo
echo "APK assinado: app/build/outputs/apk/release/app-release.apk"
