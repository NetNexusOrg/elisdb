#!/bin/sh
# Prepare the runtime directories, then start the server as the unprivileged
# user configured by `user:` in compose. This script runs with the host uid/gid
# already and never changes ownership; it fails early if a directory is closed.
set -e

db_dir="$(dirname "${ELISDB_DB:-data/elisdb.db}")"
photos_dir="${ELISDB_PHOTOS:-data/photos}"
configs_dir="${ELISDB_CONFIGS_DIR:-$db_dir/configs}"
apk_dir="${ELISDB_APK_DIR:-$db_dir/apk/latest}"

for dir in "$db_dir" "$photos_dir" "$configs_dir" "$apk_dir"; do
  mkdir -p "$dir"
  if [ ! -w "$dir" ]; then
    echo "error: $dir is not writable by uid $(id -u):$(id -g)" >&2
    echo "the container runs as PUID/PGID; fix the host ownership with:" >&2
    echo "  sudo chown -R $(id -u):$(id -g) elisdb-data configs" >&2
    exit 1
  fi
done

exec "$@"
