# Proposal

## Why

The client log says only *who* touched the record and *when* ("Editou o cliente — Ricardo, 18:35"). It cannot answer the questions people actually ask when two devices disagree or a record looks different from what they typed: **what** changed? Did the name change? The contract? Was a photo added, removed or reordered? Today the server stores only `create`/`edit`, the actor and the timestamp, so that information is lost at the moment the change is accepted — the previous values are overwritten and never kept.

The server is the only party that can answer this reliably: it holds the previous row and the incoming record in the same transaction, and it already knows the previous and incoming photo sets. A client-side diff would only describe that device's own copy and would miss conflicts (a push rejected as stale never happened, a duplicate merge changes the canonical record).

## What Changes

- `client_audit` gains a nullable `details` column (JSON) with the accepted change: the fields whose values differ (previous → new) and the photos added, removed or reordered.
- `save_client` computes the diff against the stored row and photo set at accept time, inside the same transaction, and skips the audit entry when an accepted push changes neither a field nor a photo (noise from no-op pushes disappears).
- `GET /api/clients/{id}/history` returns the details per entry; entries recorded before this change return `details: null` and keep working.
- The client log page renders each change: field name, previous value struck through, new value; photo additions with thumbnails from the local copy, removals as a count, reordering as a note. Legacy entries keep today's "Criou/Editou" presentation.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `server-sync`: the audit entry now records what changed, and a no-op push records no entry; the history endpoint returns those details.
- `client-sync`: the history fetched by the app carries the change details.
- `client-screens`: the log page presents the changed fields and photo changes, not only the actor and time.

## Impact

- **Server**: `app/database.py` (schema migration, diff computation in `save_client`, `client_history`), `app/models.py` (`AuditDetailsOut`, `AuditEntryOut.details`), `app/main.py` (history response model). Additive column, no data rewrite.
- **App**: `remote/AuditEntryDto` (+`AuditDetailsDto`, `AuditFieldDto`, `AuditPhotosDto`), `data/local/FotoDao` (`getByIds`), `ui/detalhe/ClientLogViewModel` (resolves photo thumbnails from the local copy), `ui/detalhe/ClientLogScreen` (change rendering). No Room migration.
- **Tests**: server tests for field/photo diffs, no-op suppression and legacy null details; app unit test for the DTO/ViewModel mapping; instrumented tests for rich entries and legacy entries.
- **Docs**: `elisdb-android/docs/uso.md`.
- **Not affected**: the sync protocol, `updated_at` semantics, last-write-wins, photo storage.
