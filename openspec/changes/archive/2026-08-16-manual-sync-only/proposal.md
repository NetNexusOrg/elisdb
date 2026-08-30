## Why

The app currently synchronizes automatically every 15 minutes via a periodic WorkManager worker. The user wants synchronization to be **manual only**: the app should never push/pull on its own. Instead, when the user opens the Settings screen, the app checks (without transferring data) whether there is work to send (pending local changes) or to receive (newer server data), and shows that information so the user can decide to sync manually.

This keeps the offline-first model and the pending indicators, but removes the surprise of automatic data transfers and gives the user full control over when the network is used.

## What Changes

- Remove the periodic auto-sync worker; data is transferred only when the user taps the manual sync action.
- On opening the Settings screen, the app checks pending uploads (local) and pending downloads (server `/api/latest` vs the pull watermark) and shows the result:
  - "Sem sincronizações pendentes" — nothing to do
  - "Sincronização disponível (enviar)" — pending uploads only
  - "Sincronização disponível (receber)" — pending downloads only
  - "Sincronização disponível (enviar e receber)" — both
- The existing sync action button stays; manual sync performs the full push/pull.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the sync status row shows a "sincronização disponível" message (send/receive/both) computed when the Settings screen is opened; no auto-sync.
- `client-sync`: the app never synchronizes automatically; sync is triggered only manually.

## Impact

- **App**: `ElisdbApp`/`SyncManager` — stop scheduling the periodic worker; `SettingsViewModel` — check pending on open (already done); `SyncStatusBar`/`syncMessage` — new message text ("Sincronização disponível (enviar/receber/ambos)", "Sem sincronizações pendentes").
- **Behavior**: no automatic push/pull; Settings shows what sync would do; manual sync transfers.
- **Tests**: update `SyncStatusBarTest` messages; unit tests that the periodic worker is not scheduled.

> Note: this change supersedes the pending `sync-idle-state-message` change (the "Sem sincronizações pendentes" idle text is folded in here). `sync-idle-state-message` should be dropped.
