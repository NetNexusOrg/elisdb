## Approach

Extend the existing lightweight status request instead of adding a new endpoint or a data transfer. The app already calls `GET /api/latest` to decide whether there are updates to receive; that request is the natural place to also report how many clients the server holds.

### Server

`GET /api/latest` returns `{"latest_updated_at": <int>, "client_count": <int>}`.

- The count is `SELECT COUNT(*) FROM clientes` — live clients only; tombstones do not count.
- The field is additive, so older clients that ignore unknown fields keep working.
- Cost is a single indexed count over a small table, on an endpoint that is already called when the sync screen opens.

### App

`SyncApi` exposes one call returning both values:

```kotlin
data class ServerInfo(val latestUpdatedAt: Long, val clientCount: Int)
suspend fun fetchServerInfo(timestamp: Long): ServerInfo
```

`SyncManager` replaces its `fetchLatestUpdatedAt: suspend () -> Long?` dependency with
`fetchServerInfo: suspend () -> ServerInfo?` and:

- keeps deriving `hasPendingDownloads` from `latestUpdatedAt` vs the pull watermark (unchanged semantics);
- publishes `serverClienteCount: StateFlow<Int?>` from the same response.

`null` means "unknown" (never fetched, no configuration, or the request failed). The last known
value is kept when a later refresh fails, matching the best-effort behaviour of the pending
indicators.

### UI

The "Dados" card becomes:

| Row | Value |
| --- | --- |
| Clientes no servidor | server total, or `—` when unknown |
| Clientes na app | local total (unchanged query, excludes pending deletions) |
| Alterações por enviar | numeric count (unchanged) |
| Alterações por receber | `0` / `≥1` (unchanged) |

The "Estado" card already answers "há sincronizações a fazer" (sincronizado / enviar / receber /
both), so the data card only adds the reference totals.

## Risks

- **Stale count while offline**: the row keeps the last successful value. Accepted: it matches
  the pending indicators' best-effort behaviour and avoids flickering; a never-fetched count is
  shown as `—` rather than `0`.
- **Older servers**: a server without `client_count` yields an unknown count (`null`), so the
  row shows `—` rather than a misleading `0`.
