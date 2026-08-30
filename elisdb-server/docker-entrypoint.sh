#!/bin/sh
set -e

# Ajusta o dono dos dados no arranque (lógica semelhante às imagens
# linuxserver): garante que ELISDB_DB e ELISDB_PHOTOS existem e pertencem ao
# utilizador que vai correr o serviço (PUID/PGID, por omissão 1000/1000).
# Isto corrige volumes nomeados que nascem com dono root, sem exigir
# `docker compose down -v`.

PUID="${PUID:-1000}"
PGID="${PGID:-1000}"

db_dir="$(dirname "${ELISDB_DB:-data/elisdb.db}")"
photos_dir="${ELISDB_PHOTOS:-data/photos}"
configs_dir="${ELISDB_CONFIGS_DIR:-$db_dir/configs}"

for dir in "$db_dir" "$photos_dir" "$configs_dir"; do
  mkdir -p "$dir"
  chown -R "${PUID}:${PGID}" "$dir"
done

# Provisiona as chaves dos utilizadores declarados em ELISDB_USERS (idempotente).
if [ -n "${ELISDB_USERS:-}" ]; then
  setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups python -m app.provision
fi

exec setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups "$@"