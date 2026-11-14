# Design — scheduled backups

## Decision: in-process scheduler, not host cron

| Option | Verdict |
|---|---|
| Host cron / systemd timer calling `docker compose exec` | Rejected: needs per-host setup, duplicates the configuration that already lives in `.env`, breaks in local mode, and cannot rotate its own snapshots. |
| In-process daemon thread in `elisdb/main.py` | **Chosen**: mirrors the existing photo-GC and retention loops, works identically in Docker and local mode, and is visible in the server log. |
| External backup tool (restic/borg/Litestream) | Not now: valuable for off-site copies, but the immediate need is a local recovery point. Documented as a follow-up. |

The work is I/O bound (copy the DB plus every photo), so it runs in a daemon
thread; the event loop and request handlers are never blocked. A
`threading.Lock` with `acquire(blocking=False)` guarantees a single run at a
time, exactly like the photo sweep.

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `ELISDB_BACKUP_INTERVAL_SECONDS` | `86400` | Interval between automatic backups. `0` disables them. |
| `ELISDB_BACKUP_RETENTION` | `3` | Number of automatic backups to keep. `0` means "keep everything". |
| `ELISDB_BACKUP_DIR` | `<data dir>/backups` | Where automatic backups are written. |

Values are read with the existing `_env_int` helper, so invalid input degrades
to the default and logs a warning instead of crashing startup.

## Naming and retention

Automatic backups are created as `auto-YYYYmmdd-HHMMSS` (UTC), matching the
`pre-deploy-<epoch>` style already used by the deploy script. Retention works
only on names starting with `auto-`:

- sort by name (lexicographic order equals chronological order);
- delete all but the newest `keep`;
- never touch `pre-deploy-*`, `pre-migrate-*`, `pre-2.0.0`, or any directory
  without the `auto-` prefix.

`prune_backups` refuses to delete a directory that does not contain a
`manifest.json`, so a half-written backup is left for inspection rather than
silently removed.

## Verification and failure handling

`create_backup()` already writes a manifest with SHA-256 per photo and runs
`PRAGMA integrity_check` on the copied database. After creating a scheduled
backup the scheduler calls `verify_backup()`:

- success → log `scheduled backup ok: <dir> (<n> photos, <size>)` and let
  retention run;
- failure → log the error, keep the directory (so the operator can inspect it),
  and do **not** count it for retention.

The next attempt happens on the normal interval; a failed run never raises into
the request path.

## Disk-space guard

Before copying, the scheduler estimates the backup size (`db size + photos
size`). If the free space on `ELISDB_BACKUP_DIR` is below twice that estimate
plus a 64 MB margin, the run is skipped with a warning. This prevents a full
disk from turning a backup into an outage — the most likely failure mode seen
on the VPS.

## Startup behaviour

The server records the timestamp of the newest verified `auto-*` backup. If it
is older than the interval (or there is none), a backup is scheduled after a
short delay (`BACKUP_STARTUP_DELAY_SECONDS = 60`) so the HTTP server is already
serving traffic. Restarting the container therefore does not lose a day of
backups.

## Testing

Unit tests (tmp directories, no network):

- `prune_backups` keeps the newest N and never removes manual backups;
- `prune_backups` ignores directories without a manifest;
- the spacing decision (`should_run`) respects interval `0`, a recent backup,
  and an old/missing backup;
- the disk guard skips when free space is below the threshold (injected value);
- an end-to-end create → verify → prune cycle with a small SQLite file and one
  photo.

Manual check on the VPS: start the container with a `600`-second interval,
confirm `auto-*` appears, restart, confirm retention keeps the configured count,
and confirm manual/pre-deploy backups survive.
