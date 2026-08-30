## Why

Users want to know who created or edited a client and when. Today the app shows only the client's current data; there is no history of who changed what. The server already authenticates every push with a per-device key whose allowlist entry carries a user name (`keys.comment`), so the identity of the actor is available at save time but is never recorded.

## What Changes

- The server records an audit entry (actor user name, actor key id, timestamp, and whether it was a create or an edit) each time a client is persisted.
- The server exposes that history per client so the app can read it.
- The client detail screen gains a "Log" action that shows who created the client and who edited it, and when (create entry + most recent edit entries).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `server-sync`: push records an audit entry with the authenticated actor and timestamp when a client is created or updated; a per-client history endpoint returns those entries.
- `client-screens`: the client detail screen offers a "Log" action that displays the client's create/edit history.
- `client-sync`: the app fetches the per-client edit history from the server.

## Impact

- **Server**: `database.py` (audit table + recording on `save_client`), `main.py` (pass the authenticated actor to `save_client`; expose the history endpoint), `models.py` (history DTOs), tests.
- **App**: `SyncApi`/`ApiClient` (fetch history), `ClientDetailViewModel`/`ClientDetailScreen` (Log action + view), DTOs, tests.
- **Behavior**: the server records who and when for every persisted client change; the app shows that history from the detail screen.
- **Wire**: additive — new endpoint and DTOs; the push payload is unchanged (the actor comes from the authenticated request).
