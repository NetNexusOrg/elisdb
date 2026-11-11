# Proposal — remove-periodic-sync

## Why

The settings screen offers an opt-in "Automático (15 min)" toggle that schedules a
periodic background sync. In practice the app already syncs when it matters: on
launch, after saving a new or edited client, after a deletion, and on the manual
**Sincronizar** action. The periodic schedule adds a second, invisible transfer
path that is hard to reason about and is no longer wanted.

## What Changes

- Remove the "Automático (15 min)" toggle from the synchronization card.
- Remove the periodic schedule preference and its scheduling code.
- Cancel the legacy `elisdb-periodic-sync` job on startup, so devices that had
  the option enabled stop running it after the upgrade.
- Keep the transient-failure retry: after a network/server failure the app still
  schedules one retry with exponential backoff (30 s), as required by
  `client-resilience`.
- Update the user documentation and the tests that referenced the toggle.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-sync`: replaces the requirement "Sync happens on launch, save and
  manual trigger" with one that removes the optional period background schedule
  and the "Enabled periodic sync runs in the background" scenario.

## Impact

- **Android only**: settings screen, `SyncViewModel`, sync scheduling, app
  startup, tests and `docs/uso.md`.
- **Unchanged**: server, API, database, `client-resilience` retry behavior,
  version and `CHANGELOG.md`.
