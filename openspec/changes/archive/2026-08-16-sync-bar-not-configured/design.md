## Context

`SyncStatusBar.syncMessage` derives its message from `SyncStatus`/`SyncFault` and the pending flags. When no server config is loaded, `SyncStatus` is `IDLE` and `SyncFault` is `null`, so the message falls through to the idle "Sincronizado". Only after a sync attempt that fails with `SyncFault.CONFIG` does it show "Não configurado". The no-config state should be reflected immediately, independent of a sync attempt.

## Goals / Non-Goals

**Goals:**
- With no server configuration loaded, the sync bar shows "Não configurado" immediately.
- With configuration present, all existing messages are unchanged.

**Non-Goals:**
- Changing the sync engine, the pending checks, or the sync action position/behaviour.

## Decisions

**D1 — Pass "has configuration" into the bar.**
- `SettingsViewModel` already exposes `hasKey` in `SettingsUiState`. Pass it to `SyncStatusBar` (a new `hasConfig: Boolean` parameter) and to `syncMessage`.
- `syncMessage` checks `!hasConfig` first: when false (and not in-progress), return "Não configurado". When true, keep the existing precedence.
- *Rationale*: the config state is known locally; the bar should not wait for a failed sync to report it.
- *Alternative considered*: deriving it from `baseUrl.isNotBlank()` — `hasKey` is the existing, accurate signal.

**D2 — Precedence.**
- Put the `!hasConfig` check right after the in-progress check (so an in-progress sync still shows "A sincronizar…"), before the pending/other states (which can't be meaningfully pending without a config).
- *Rationale*: keeps an active sync visible even in a transient no-config state, and matches the spec order.

**D3 — Files touched.**
- `SettingsViewModel`/`SettingsScreen` (pass `hasKey`), `SyncStatusBar`/`syncMessage` (new parameter + branch), `SyncStatusBarTest` (idle-with/without-config). No server or schema change.

## Risks / Trade-offs

- [In-progress sync with no config] → In-progress is checked first, so it still shows "A sincronizar…"; after it fails, "Não configurado".

## Migration Plan

None — app-only change.

## Open Questions

None.
