# Tasks — scheduled backups

## 1. `elisdb/backup.py` helpers

- [x] 1.1 Add `AUTO_PREFIX = "auto-"`, `backup_name(now)` and `list_backups(dir)`
  returning paths sorted oldest first.
- [x] 1.2 Add `prune_backups(out_dir, keep)`: sort `auto-*` entries, delete all
  but the newest `keep`, skip directories without `manifest.json`, return the
  removed paths. `keep <= 0` means "keep everything".
- [x] 1.3 Add `estimate_backup_bytes(db_path, photos_dir)` and
  `free_space_bytes(path)`; skip threshold = `2 * estimate + 64 MiB`.
- [x] 1.4 Extend the CLI with `list <dir>` and `prune <dir> --keep N`
  (`--dry-run` optional but preferred).
- [x] 1.5 Tests in `tests/test_backup.py`: retention keeps newest N, manual and
  pre-deploy directories survive, manifest-less directories survive, estimate
  and free-space helpers.

## 2. Scheduler in `elisdb/main.py`

- [x] 2.1 Read `ELISDB_BACKUP_INTERVAL_SECONDS` (default 86400), `ELISDB_BACKUP_RETENTION`
  (default 3) and `ELISDB_BACKUP_DIR` (default `<data dir>/backups`) with
  `_env_int`/`os.environ`.
- [x] 2.2 Add `should_run_backup(interval, newest_created_at, now)` as a pure
  function for testability.
- [x] 2.3 Add `_maybe_backup()` mirroring `_maybe_gc_photos`: daemon thread,
  non-blocking lock, disk guard, `create_backup`, `verify_backup`, log, then
  `prune_backups`.
- [x] 2.4 Call it on startup (60 s delay) and from the existing periodic
  maintenance tick; never let a failure propagate to a request.
- [x] 2.5 Tests: interval `0` disables, recent backup skips, old/missing backup
  runs, failed verification does not prune, exception is logged and swallowed.

## 3. Configuration and docs

- [x] 3.1 `.env.example`: add the three keys with defaults and comments.
- [x] 3.2 `docs/configuracao.md`: add the keys to the table.
- [x] 3.3 `docs/backup.md`: document automatic backups, naming, retention,
  disk guard and how to list/prune manually.
- [x] 3.4 `docs/gestao-tui.md`: mention that status shows the newest automatic
  backup.

## 4. Status and TUI

- [x] 4.1 `elisdb/status.py`: add `backups` details (`count`, `newest_auto`,
  `newest_auto_age_seconds`) to `collect()`.
- [x] 4.2 `start.py`: print the newest automatic backup and its age in the
  Status screen; show `sem backups automáticos` when none exist.
- [x] 4.3 Tests for the new status fields (empty dir, one backup, mix of manual
  and automatic).

## 5. Verification

- [x] 5.1 `pytest` green (unit and integration).
- [x] 5.2 `openspec validate add-scheduled-backups`.
- [x] 5.3 Local Docker smoke: interval 60 s, confirm `auto-*` appears, restart
  and confirm no duplicate run inside the interval.
- [x] 5.4 VPS: deploy, set the interval in `.env`, confirm one automatic
  backup, retention across two runs, and that `pre-deploy-*` backups survive.
