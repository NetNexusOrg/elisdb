## Context

The app auto-syncs every 15 minutes: `ElisdbApp.onCreate` calls `SyncManager.schedulePeriodic()`, which enqueues a periodic `SyncWorker` that runs a full push/pull. The user wants sync to be manual only, with the Settings screen showing, on open, whether there is work to send or receive — without transferring data.

The pending indicators already exist (`SyncManager.hasPendingUploads`/`hasPendingDownloads`, refreshed on Settings open via `refreshPendingDownloads()`); this change removes the periodic worker and rewords the messages.

## Goals / Non-Goals

**Goals:**
- No automatic data transfer; sync happens only on the manual action.
- Settings, on open, shows "Sem sincronizações pendentes" / "Sincronização disponível (enviar/receber/ambos)" without transferring client/photo data.

**Non-Goals:**
- Changing the sync engine's push/pull logic or LWW semantics.
- Auto-triggering a sync on save (still manual).

## Decisions

**D1 — Stop scheduling the periodic worker.**
- `ElisdbApp` no longer calls `schedulePeriodic()`. The `schedulePeriodic`/`SyncWorker` plumbing can remain dormant (or be removed) — the key is that no periodic sync is enqueued.
- *Rationale*: the simplest way to make sync manual-only; the WorkManager periodic worker is the only automatic path.

**D2 — Pending checks already run on Settings open.**
- `SettingsViewModel` already calls `refreshPendingDownloads()` in `init` (which hits `/api/latest` and compares with the watermark) and observes `hasPendingUploads`. No transfer of client/photo data happens there.
- *Rationale*: the "check, don't transfer" behaviour is already in place; only the wording changes.

**D3 — Reword the sync status messages.**
- In `SyncStatusBar`/`syncMessage`, when idle with no pending work → "Sem sincronizações pendentes".
- When pending uploads only → "Sincronização disponível (enviar)".
- When pending downloads only → "Sincronização disponível (receber)".
- When both → "Sincronização disponível (enviar e receber)".
- *Rationale*: the user explicitly asked for this wording; it communicates what a manual sync would do.

**D4 — Supersede `sync-idle-state-message`.**
- This change folds the idle message in and should be applied instead of the still-pending `sync-idle-state-message` change; that change should be dropped (not implemented).

**D5 — Files touched.**
- `ElisdbApp.kt` (remove `schedulePeriodic()` call), `SyncStatusBar.kt` (messages), `SyncStatusBarTest` (messages), possibly `SyncManager` (leave worker dormant). No server change.

## Risks / Trade-offs

- [Changes stay unsynced until the user opens Settings and taps sync] → Intended: the user controls transfers; the Settings message tells them there is work.
- [Periodic worker code remains dormant] → Harmless; can be removed in a follow-up if desired.

## Migration Plan

None — app-only change.

## Open Questions

None.
