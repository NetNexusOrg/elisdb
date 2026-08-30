## Why

A client record's location (latitude/longitude) is currently mandatory at every layer — the form blocks save without coordinates, the local schema uses `NOT NULL`, and the server API rejects records without them. A client may legitimately have no known location (e.g. a contact without a fixed address), and forcing a coordinate is a poor experience.

## What Changes

- The create/edit form no longer requires a location: saving without coordinates is allowed, and the location section is optional.
- The client record model, Room schema, sync DTOs and server schema make latitude/longitude nullable.
- The detail screen shows the map and the "navigate" action only when a location exists, and hides them otherwise.
- Sync (push/pull) sends and accepts records with or without coordinates; the server stores null coordinates when absent. **BREAKING** for the wire format: latitude/longitude become optional (`null`) in the push/pull payloads.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-data`: client record may be stored without coordinates; create/edit allowed without a location.
- `client-screens`: form no longer requires location; detail screen shows map/navigate only when a location exists.
- `client-location`: picking/displaying a location remains unchanged, but it is no longer a precondition for a client.
- `server-sync`: server accepts and stores records without coordinates; pull returns null coordinates.

## Impact

- **App**: `ui/form/ClientFormViewModel.kt` (remove location-required check), `data/local/Cliente.kt` (nullable lat/lon), `data/local/AppDatabase.kt` (new migration 6→7, nullable columns), `data/ClienteRepositoryImpl.kt` (`ClienteDraft` nullable), `ui/detalhe/ClientDetailScreen.kt` (conditional map/navigate), `remote/RemoteDto.kt`, `sync/SyncMapper.kt`.
- **Server**: `app/models.py` (`ClienteIn`/`ClienteOut` optional floats), `app/database.py` (schema `latitude`/`longitude` nullable).
- **Breaking API**: push/pull payloads may now carry `null` coordinates; older clients that require coordinates will reject null.
- **Tests**: form validation test inverted, repository/DAO tests, sync DTO tests, detail-screen test for location-less client, server pytest for optional coordinates, Room migration test.