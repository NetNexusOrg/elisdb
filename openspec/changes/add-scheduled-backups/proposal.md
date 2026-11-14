# Scheduled backups with retention

## Why

Today the only automatic backup is the one `scripts/deploy-server.sh` creates
right before a deploy. Everything else is manual: `python -m elisdb.backup
create <dir>`. The photo incident on 2026-11-14 was recovered only because a
pre-deploy backup existed minutes earlier; if the failure had happened a day
later the recovery point would have been the same old pre-deploy snapshot.

Server operators should not depend on remembering to run a command. The server
already runs its own maintenance loops (photo GC, retention pruning), so the
same mechanism can produce a daily, verified backup and rotate old automatic
ones without adding any dependency or host cron configuration.

## What Changes

- **New `elisdb/backup.py` helpers**:
  - `prune_backups(out_dir, keep)` — removes only `auto-*` backup directories,
    keeping the newest `keep`. Manual and pre-deploy backups are never touched.
  - `free_space_bytes(path)` and a size estimate so a scheduled backup is
    skipped when the disk cannot hold it twice over.
- **New background scheduler in `elisdb/main.py`** following the existing
  maintenance pattern:
  - `ELISDB_BACKUP_INTERVAL_SECONDS` (default `86400`, `0` disables).
  - `ELISDB_BACKUP_RETENTION` (default `3` automatic backups).
  - `ELISDB_BACKUP_DIR` (default `<data dir>/backups`).
  - On startup, if the newest `auto-*` backup is older than the interval, one
    runs shortly after boot; then a daemon thread repeats the check. A lock
    prevents overlapping runs and a failed backup never affects requests.
- **Each scheduled backup is verified** with `verify_backup()`; only verified
  backups are logged as successful and counted for retention.
- **CLI**: `python -m elisdb.backup prune <dir> --keep N` and `list <dir>` to
  inspect backups from the TUI/host without Python one-liners.
- **Configuration and docs**: new keys documented in `docs/configuracao.md`,
  `docs/backup.md` and `.env.example`; the deploy script and manual backups keep
  their current behaviour.
- **Status**: `start.py` shows the newest automatic backup and its age.

## Impact

- Affected capabilities: `server-backup` (new requirements), `server-deployment`
  (documented defaults only, no behaviour change), `server-management-tui`
  (status line only).
- Files: `elisdb/backup.py`, `elisdb/main.py`, `elisdb/status.py`, `start.py`,
  `tests/test_backup.py`, `tests/test_start.py`, docs, `.env.example`.
- No schema, API or auth changes. Disk usage grows by at most
  `ELISDB_BACKUP_RETENTION` × backup size (~80 MB today).
