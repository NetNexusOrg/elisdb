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
# Pacote Android publicado para atualizações na app (elisdb-<versao>-<codigo>.apk).
apk_dir="${ELISDB_APK_DIR:-$db_dir/apk/latest}"

for dir in "$db_dir" "$photos_dir" "$configs_dir" "$apk_dir"; do
  mkdir -p "$dir"
  chown -R "${PUID}:${PGID}" "$dir"
done

# Provisiona/revoga chaves dos utilizadores declarados em ELISDB_USERS (idempotente).
# Corre sempre no arranque — se ELISDB_USERS estiver vazio ou ausente, todas as
# chaves auto-provisionadas são revogadas.
setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups python -m elisdb.provision

exec setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups "$@"