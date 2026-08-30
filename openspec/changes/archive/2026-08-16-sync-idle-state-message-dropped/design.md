## Context

`SyncStatusBar.syncMessage` returns `null` in the IDLE state (the `else -> null` branch), so when the user opens Settings with no sync activity and no pending changes, the row renders only the sync action button — no icon, no message. On a device this looks like the sync state is absent, and only tapping the button reveals "Sincronizado". The spec already requires showing the sync status "for each state"; IDLE is the only state without a message.

## Goals / Non-Goals

**Goals:**
- The sync status row always shows a message, including an informative idle state.
- The fixed sync-action position (already covered by an earlier change) is preserved.

**Non-Goals:**
- Changing the pending-uploads/downloads, success, error, or in-progress messages.
- Changing the sync action's position or behaviour.

## Decisions

**D1 — Add an IDLE message in `syncMessage`.**
- When `syncStatus == IDLE` (and no pending uploads/downloads, no success/error), return an informative pair, e.g. cloud-off/sync icon + "Sem sincronizações pendentes".
- *Rationale*: minimal change; the existing precedence (pending → success → error → ... ) stays, and the IDLE branch only applies when nothing else matched.
- *Alternative considered*: leaving IDLE empty — rejected, since that is exactly the reported confusing state.

**D2 — Keep precedence order.**
- The existing order already checks IN_PROGRESS, pending uploads/downloads, SUCCESS, ERROR before the final else. Adding the IDLE message in the final branch keeps all current states unchanged and only fills the previously-empty case.

**D3 — Files touched.**
- `SyncStatusBar.kt` (`syncMessage`). Test update in `SyncStatusBarTest`. No server or schema change.

## Risks / Trade-offs

- [Message may be stale while actually syncing] → IN_PROGRESS is checked first, so the idle message never shows during an active sync.

## Migration Plan

None — app-only change.

## Open Questions

None.
