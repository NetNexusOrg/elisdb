# Tasks

## 1. Navigation and state

- [x] 1.1 Add the `ClientLog(clienteId)` navigation key.
- [x] 1.2 Add `ClientLogViewModel` (entries, loading, error, retry, client name/contract, pending flag).

## 2. Log page

- [x] 2.1 Add `ClientLogScreen`: top bar with contract, loading, error with "Tentar novamente", empty/pending states, day-grouped timeline.
- [x] 2.2 Replace the dialog action on the detail screen with `onViewLog` and remove the log state from `ClientDetailViewModel`.
- [x] 2.3 Wire the `ClientLog` entry in `Navigation.kt`.

## 3. Tests

- [x] 3.1 Unit tests for `ClientLogViewModel` (load, failure + retry, pending flags).
- [x] 3.2 Instrumented tests for the page (grouping, 24-hour clock, error + retry, empty states).
- [x] 3.3 Update `ClientDetailViewModelTest` and `ClientDetailScreenTest` (dialog tests replaced by the navigation test).

## 4. Verification

- [x] 4.1 `:app:testDebugUnitTest` and `:app:lintDebug` green.
- [x] 4.2 `ClientLogScreenTest` and `ClientDetailScreenTest` green on the emulator.
- [x] 4.3 OpenSpec validation: `openspec validate client-log-screen --strict`.
- [x] 4.4 Document the log page in `docs/uso.md`.
