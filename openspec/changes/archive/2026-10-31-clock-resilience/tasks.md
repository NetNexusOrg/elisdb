# Tasks

## 1. Clock core

- [x] 1.1 Add `sync/ClockSync.kt`: persists the offset, exposes `skewSeconds`, `nowSeconds()`, `observeServerTime()`.
- [x] 1.2 Unit-test the offset/skew math and the never-measured case.

## 2. Transport

- [x] 2.1 Parse the `Date` header of every response and refresh the offset (`ApiClient.observeServerDate`).
- [x] 2.2 Wrap signed calls in `execute`: on a `stale or future timestamp` 401, correct the offset (Date header, fallback `/api/time`), re-sign and retry once.
- [x] 2.3 Keep other 401s (invalid key) failing immediately without a retry.
- [x] 2.4 Pass `ClockSync` through `ApiClientProvider` and `AppContainer`.
- [x] 2.5 Test retry-once, invalid-key-no-retry and Date-header refresh with `MockEngine`.

## 3. Startup and UI

- [x] 3.1 Refresh the offset at app start (`ElisdbApp`).
- [x] 3.2 Expose `clockSkewSeconds` and `correctClock()` in `SyncViewModel`.
- [x] 3.3 Show a warning banner in the Configuração screen when |skew| ≥ 120 s, with amount, direction and a "Corrigir" action.
- [x] 3.4 Instrumented tests for the banner, the correction action and the no-skew case.

## 4. Verification

- [x] 4.1 `:app:testDebugUnitTest` and `:app:lintDebug` green.
- [x] 4.2 `ConfigScreenTest` and `SyncViewModelTest` green on the emulator.
- [x] 4.3 OpenSpec validation: `openspec validate clock-resilience --strict`.
- [x] 4.4 Document the warning in `docs/uso.md`.
