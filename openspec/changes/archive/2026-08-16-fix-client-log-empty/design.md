## Context

The client Log view has two defects that make it look broken on a physical device:

1. `Navigation.kt` wraps `fetchClientHistory` in `catch (e: Exception) { emptyList() }`. Any failure (offline, auth, server) is returned as an empty list, so the ViewModel's `logError` path is unreachable in practice and the UI shows "Não há histórico registado".
2. The Log fetches from the server; a client created or edited locally stays `pending` until the next sync. Until then the server has no audit entry for the change, so the log shows "no history" right after a change the user made.

## Goals / Non-Goals

**Goals:**
- A failed history fetch shows an actionable error, not "no history".
- A client with pending local changes shows that its history will appear after sync.

**Non-Goals:**
- Changing the audit recording itself (server-side create/edit logging already works).
- Auto-triggering a sync when the Log opens.

## Decisions

**D1 — Stop swallowing fetch errors in `Navigation.kt`.**
- Remove the `try/catch { emptyList() }` wrapper; let `fetchClientHistory` propagate its exceptions (or catch and return a sentinel/result). The ViewModel's existing `logError` state then becomes reachable, and the Log dialog shows "Não foi possível carregar o histórico do cliente."
- *Rationale*: the ViewModel already models the error; the wrapper was masking it.
- *Alternative considered*: returning a sealed `Result` — overkill; propagating the exception reuses the existing `runCatching`/`logError` path.

**D2 — Note pending changes in the Log view.**
- `ClientDetailViewModel` already holds the local `Cliente` (with `pending`/`deletePending`). In the Log dialog, when `logEntries` is empty and the local client is `pending`, show "Este cliente tem alterações por sincronizar; o histórico aparecerá após a sincronização." instead of only "Não há histórico registado."
- *Rationale*: directly addresses the misleading empty state for just-created/edited clients; no server change.

**D3 — Files touched.**
- `Navigation.kt` (remove the error-swallowing catch), `ClientDetailViewModel` (expose `hasPending` for the Log state), `ClientDetailScreen` (Log dialog branches: error / pending-note / empty / entries). No server change.

## Risks / Trade-offs

- [Error state appears on transient network failures] → Desired: the user learns the fetch failed rather than believing history is empty; a retry (re-open Log) re-fetches.

## Migration Plan

None — app-only change.

## Open Questions

None.
