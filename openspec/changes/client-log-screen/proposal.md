# Proposal

## Why

A client's edit history was shown in a small `AlertDialog`: a narrow, non-scrollable box with a single muted line per change (`"Criado por Maria — 16/08/2026 14:30"`). Real histories already have several entries, and the dialog gives no room to read them, no grouping and no way to retry after a failure — the only action is "Fechar".

## What Changes

- The "Log" action on the client detail screen opens a **dedicated page** instead of a dialog.
- The page shows the client name and contract in the header, the number of changes, and the entries grouped by day, one card per change with an icon for the action, the actor, what happened and the time (24-hour clock).
- Loading, failure and empty states get the full page: a spinner while loading, an error state with **Tentar novamente** (retry), and a clear note when the client still has local changes that the server does not know about.
- The log state, its request and the retry move out of `ClientDetailViewModel` into a `ClientLogViewModel`, so the detail screen only decides where to navigate.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the log requirement changes from a view opened on the detail screen to a dedicated page, with entries grouped by day, a retry action after a fetch failure, and the existing 24-hour clock and pending-change note preserved.

## Impact

- **App**: new `ui/detalhe/ClientLogScreen.kt` and `ui/detalhe/ClientLogViewModel.kt`, new `ClientLog` navigation key; `ClientDetailScreen.kt` gains `onViewLog` and loses the log dialog; `ClientDetailViewModel.kt` loses the log state and `fetchHistory`; `Navigation.kt` wires the new entry.
- **Tests**: new unit `ClientLogViewModelTest` and instrumented `ClientLogScreenTest`; `ClientDetailViewModelTest` and `ClientDetailScreenTest` updated (dialog tests replaced by a navigation test).
- **Docs**: `elisdb-android/docs/uso.md` mentions the log page.
- **Not affected**: server API, database schema, sync protocol.
