# Design

## Context

See `proposal.md` — Why. The screen is a single composable (`ConfigScreen`) fed by two existing view models: `SyncViewModel` (`totalClientes`, `serverClienteCount`, `pendingUploadCount`, `hasPendingUploads`, `hasPendingDownloads`, `lastSuccessAt`) and `ServerConfigViewModel` (URL, key, identity, import/remove). The status row is a separate component (`SyncStatusBar`). The check-for-updates action is removed: `SyncViewModel` already refreshes the pending state on open, so the action only duplicated that query.

## Goals / Non-Goals

**Goals:**
- A single place that answers "está tudo sincronizado?" and "o que falta?" without reading four rows.
- Make the server/device totals directly comparable.
- Remove dead ends: no primary action that is guaranteed to fail when unconfigured.

**Non-Goals:**
- No change to `SyncManager`, sync semantics, server API, or state model.
- No new screen, no automatic sync, no exact count for pending downloads (none exists server-side).

## Decisions

- **Keep `SyncStatusBar` as the summary row, enrich it.** Adding `detail: String?` and dropping the action leaves a simple `Row` (tinted icon + weighted `Column`) and avoids duplicating the status logic in the screen. Alternative: a new hero composable replacing the bar — rejected because it would duplicate `syncMessage`.
- **Shorten the pending title and move the specifics to the detail line.** `syncMessage` returns "Alterações por sincronizar" for any pending state; the screen composes the detail from the counts ("3 por enviar · novidades por receber"). Alternative: keep "(enviar e receber)" parenthetical — rejected: duplicates the data rows and gives no numbers.
- **State-tinted icon container instead of coloured text.** Colour comes from the M3 scheme (`primaryContainer` synced, `tertiaryContainer` pending/in-progress, `errorContainer` error, `surfaceVariant` unconfigured), so it survives dynamic colour and dark mode without hard-coded greens/ambers.
- **Last-sync as a caption under the title.** Removes a row without losing information; keeps the persisted timestamp requirement.
- **Two stat tiles for the totals, plain rows for pending changes.** `<value>` in `headlineSmall` + `<label>` in `bodySmall`, side by side, makes the server/app comparison immediate; pending rows stay `InfoRow` (labels + numeric values already asserted by tests).
- **One screen, two view models.** `ConfigScreen` composes the existing `SyncViewModel` and `ServerConfigViewModel` instead of merging their state into a new view model: each keeps a single responsibility, the navigation entry creates both, and no sync logic moves. The configured state is read from `ServerConfigViewModel.hasKey` (it changes immediately on import/remove), so `SyncUiState.hasKey` is dropped.
- **No sync control while unconfigured**: the summary states that synchronization is not available, and the only import action is the one in the server card, so the screen never shows two ways to import the same file. The pending state is refreshed on open, so no manual check is needed.
- **Server details use icon-led rows** (small label above the value) because the URL is too long for a right-aligned value; the import action is an outlined button and the destructive "Remover configuração" a text button in `error` colour.
- **Notice as a banner** (`secondaryContainer`, rounded) so the post-sync result reads as feedback rather than as part of the status.

## Risks / Trade-offs

- [Pending title loses the explicit "(enviar)/(receber)" wording the spec used] → the detail line names both directions explicitly; spec scenarios reworded to "indicate", tests assert the detail text.
- [Two totals side by side can differ while nothing is pending, e.g. a local client created offline is counted locally but not on the server] → totals are informational; the pending rows remain the source of truth for "há sincronizações a fazer". No derived "missing N" claim is made.
- [Merging the screens mixes sync and configuration concerns] → the screen only lays out two view models; the sync logic stays in `SyncManager`/`SyncViewModel` and the configuration logic in `ServerConfigViewModel`.
- [Bigger title may wrap with long failure messages] → detail/title use `maxLines` unbounded but the action stays right-aligned; messages are short sentences.

## Migration Plan

Pure UI change in the app; no data, API or configuration migration. Rollback is a code revert.
