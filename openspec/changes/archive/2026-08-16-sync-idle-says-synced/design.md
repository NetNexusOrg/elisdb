## Context

`SyncStatusBar.syncMessage` already shows a message for every state. In the idle fallback (no pending uploads/downloads, not in progress/success/error) it currently returns "Sem sincronizações pendentes". The user wants "Sincronizado" there instead, so the idle state reads the same as the just-succeeded state.

## Goals / Non-Goals

**Goals:**
- The idle (nothing to sync) message reads "Sincronizado".

**Non-Goals:**
- Changing any other state's message or the sync action position/behaviour.

## Decisions

**D1 — Change the idle fallback string.**
- In `syncMessage`, replace the `else` branch text from "Sem sincronizações pendentes" to "Sincronizado".
- *Rationale*: minimal change; the precedence (pending → success → error → ... ) is unchanged; the idle branch only applies when nothing else matched and there is nothing pending.

**D2 — Files touched.**
- `SyncStatusBar.kt` (`syncMessage` idle string), `SyncStatusBarTest` (idle assertion). No server or schema change.

## Risks / Trade-offs

- [Idle and success now show the same text] → Intended; both mean the data is synchronized.

## Migration Plan

None — app-only change.

## Open Questions

None.
