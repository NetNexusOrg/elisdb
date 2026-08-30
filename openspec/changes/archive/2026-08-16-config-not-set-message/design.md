## Context

`SyncStatusBar.syncMessage` maps `SyncFault.CONFIG` to the message "Servidor não configurado. Abre as Definições para configurar." This text is long and, inside the Settings screen, redundantly tells the user to open Settings. The user wants a short "Não configurado".

## Goals / Non-Goals

**Goals:**
- The missing-server-configuration message reads "Não configurado".

**Non-Goals:**
- Changing any other sync state's message or the sync action position/behaviour.

## Decisions

**D1 — Change the CONFIG message string.**
- In `SyncStatusBar.syncMessage`, change the `SyncFault.CONFIG` branch text from "Servidor não configurado. Abre as Definições para configurar." to "Não configurado".
- *Rationale*: minimal change; the icon and precedence stay the same.

**D2 — Files touched.**
- `SyncStatusBar.kt` (`syncMessage` CONFIG string), `SyncStatusBarTest` (errorConfig assertion). No server or schema change.

## Risks / Trade-offs

- [Shorter message is less explanatory] → The Settings screen already shows the server fields as "Não configurado"; a short sync message is consistent and sufficient.

## Migration Plan

None — app-only change.

## Open Questions

None.
