## Why

The synchronization screen's "Dados" card only reports the number of clients stored **on the device**. The user cannot see how many clients exist **on the server**, so there is no way to tell whether the local copy is complete — for example after a partial pull, a fresh import, or when another device has created clients that this device has not received yet. The remaining pending indicators exist, but they lack the reference numbers that make them meaningful.

## What Changes

- The server's latest-data response (`GET /api/latest`) gains the number of live client records it stores, so a client can learn the server total without pulling the dataset.
- The synchronization screen's "Dados" card shows two client totals side by side: **clients on the server** and **clients in the app** (the existing local count is relabelled for clarity).
- When the server total is not known (no configuration, offline, or never fetched), the row shows a placeholder instead of a misleading `0`.
- The server total is refreshed together with the pending-downloads check — when the screen opens, when the user taps "Verificar", and after a successful sync — so it is always read from the same request as the pending state.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `server-sync`: the latest-data response also reports the number of stored client records.
- `client-sync`: the synchronization screen shows the server client total alongside the device total.

## Impact

- **Server**: `GET /api/latest` response gains `client_count` (additive and backward compatible); a `COUNT(*)` query over live clients supports it.
- **App**: `SyncApi`/`ApiClient` fetch the server info (latest timestamp + client count) in one request; `SyncManager` exposes the server count; `SyncViewModel`/`SyncUiState` carry it; `SyncScreen` renders the new row.
- **Behavior**: no data transfer is added — the count comes from the existing lightweight status request.
- **Tests**: server pytest for the count (after push and after delete); app unit tests for the new observable and instrumented tests for the rendered rows.
