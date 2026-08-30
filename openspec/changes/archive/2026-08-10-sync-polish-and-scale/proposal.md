## Why

Review R4–R8 came out as medium/low but worth bundling into one polish+scale change: `dataCriacao` is degraded to `updated_at` on pull (real creation time lost); the 401 "chave não registada" message is misleading when the real cause is a clock skew >5 min; the docs promise a sync status bar on the client list that doesn't exist; pull transfers the entire catalog on every sync; and photos whose file is missing leave "ghost" rows that never sync.

## What Changes

- **R4 — Data de criação fiável**: preserved `dataCriacao` on pull (server `created_at` added, or local preservation heuristic when only `updatedAt` changed).
- **R5 — Messaging de auth**: distinguish clock-skew from invalid/unregistered key, with an actionable "relógio do dispositivo dessincronizado / verifique a hora" message.
- **R6 — Observabilidade**: sync status bar (or sync button + status) on the top of the client list screen, matching `docs/uso.md`, or docs corrected to reflect current state.
- **R7 — Pull incremental**: add `since`/pagination (or photo `hash`) to the pull endpoint so clients avoid downloading the whole dataset every time.
- **R8 — Fotos "fantasma" / órfãs**: mark+remove local foto rows whose file is missing, and sweep orphaned files left by earlier aborted syncs.

These five are independent; they can be implemented/landed separately. This single proposal keeps the tracking together under one umbrella change, executed in independent task groups.

## Capabilities

### New Capabilities

- `sync-observability`: exposing sync state/triggers on the main screen, and clock-skew-aware auth messaging.
- `sync-incremental-pull`: delta/paginated pull so clients only receive changed records.
- `photo-hygiene`: phantom-orphan photo cleanup (missing-file rows and orphaned files).

### Modified Capabilities

- `server-sync`: pull endpoint gains `since` (or equivalent) and optional `created_at` transport; changing a server requirement.
- `client-sync`: `dataCriacao` no longer equals `updatedAt`; pull delta applied; auth messaging differentiates clock skew.
- `client-key-import`: if affected by `created_at`/registration, resync expectations — only if server-sync changes break it.

## Impact

- Server `main.py`/`database.py`: `created_at` field, `since` parameter on `/pull`, optional.
- App DTOs `RemoteDto.kt`, `SyncMapper`, DAOs (Room schema change for `created_at` if adopted), `SyncCoordinator`, `SettingsViewModel`, `ClientListScreen` (top bar), `SyncStatusBar`/messages.
- Docs: `docs/uso.md`, `docs/troubleshooting`.
- Tests: each sub-scope adds unit/instrumentation coverage; server tests for `/pull?since`.