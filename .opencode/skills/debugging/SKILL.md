---
name: debugging
description: Systematic bug investigation workflow for the ElisDB repo. Use when diagnosing a reported bug, a failing test, or a sync/UI/server issue.
---

# Debugging

Follow this order; do not jump to a fix.

1. **Reproduce** — state the exact repro steps and expected vs actual behavior.
   For sync issues, check the sync state/status in the app; for server issues, hit the
   endpoint or read logs.
2. **Collect evidence** — read logs (server stdout / `gh run view <id> --log`), relevant
   code paths, and any failing test output. Note versions (`app.versionName`, commit).
3. **Identify likely cause** — form a hypothesis grounded in the code, not guesses.
4. **Confirm hypothesis** — add a targeted test, log, or repro that proves the cause
   before changing anything.
5. **Implement a minimal fix** — the smallest change that addresses the root cause.
   Do not mask the symptom (e.g. swallowing an exception, adding a sleep, disabling a check).
6. **Test** — run the relevant tests (`./gradlew :app:testDebugUnitTest`, lint).
7. **Verify no regressions** — run the broader test suite and re-check the original repro.

## Project-specific gotchas

- Sync is async and manual (single-flight); a "no data" state may be a config fault
  (blank URL/key), a clock-skew failure, or a replay rejection — check
  `SyncFault`/`SyncStatus` first.
- LWW conflicts: a client that "disappears" locally may be a `stale`/`duplicate` result from
  `save_client` — inspect the push response, not just the UI.
- Room: after a schema change, missing/mismatched export JSON or a missing `Migration` breaks
  installs — verify `app/schemas/` is in sync.

## Rules

- Prefer root-cause fixes over workarounds; explain the root cause in the fix message.
- Do not change unrelated code while debugging.
