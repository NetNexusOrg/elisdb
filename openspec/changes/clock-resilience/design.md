# Design

## Context

Records carry `updated_at` written by the sending device (shifted by the last measured device↔server offset). Pulls are cursor-based (`updated_at > since`) and `/api/latest` reports the maximum `updated_at`. A record stamped with a date older than another device's cursor is therefore never delivered to that device — the server now bumps accepted pushes ahead of the watermark, but prevention on the client is still needed: the app must stamp edits on the server's timescale.

## Decisions

### Observe the server clock on every response

Every response carries a `Date` header (RFC 1123). `ApiClient` parses it and feeds `ClockSync.observeServerTime(serverTime)`, which stores `offset = serverTime − deviceNow` and exposes `skew = −offset` (negative when the device is behind). This keeps the offset current at no extra request cost, and covers long sessions where the device clock drifts or is changed.

### Retry once on a stale-timestamp rejection

A 401 whose body contains `stale or future timestamp` means the signed timestamp is outside the server's 5-minute window. The client reads the server time from that same response's `Date` header (falling back to `GET /api/time`), corrects the offset, re-signs with the corrected timestamp and repeats the request exactly once. Any other 401 (invalid key) is reported immediately — retrying cannot help.

The shared `execute(timestamp, send)` helper wraps every signed call, so all endpoints (push, pull, delete, latest, history, photos, key identity) get the same behaviour.

### Measure at startup

`ElisdbApp` refreshes the offset in the application scope right after the container is created. Edits made before the first sync of a session then carry a usable timestamp. The refresh is best-effort: unconfigured or offline devices keep the previous offset and the sync proceeds as before.

### Warn instead of blocking

A skew below 120 seconds is noise (the server window is 300 s). Above it, the configuration screen renders a banner with how far off the clock is and its direction, plus a "Corrigir" action that calls `refreshClock()`. Syncing is never blocked: an incorrect clock is a data-integrity risk worth surfacing, not a reason to prevent work.

## Risks / trade-offs

- The `Date` header has one-second resolution and includes transfer latency, so each observation is off by a fraction of a second. This is far below the 300-second window and irrelevant for ordering.
- Continuous observation rewrites the stored offset on every request; a device whose clock jumps temporarily (e.g. manual change) will see the offset follow the server, which is the desired behaviour.
- The retry doubles one request in the (rare) skewed case. It is bounded to one attempt so a persistently wrong key cannot loop.
- The banner depends on having observed a server time at least once; a never-synced, never-started device shows nothing until the first response.

## Alternatives considered

- **Server-assigned sequence numbers**: removes timestamp semantics from ordering entirely, but requires a protocol change (`since_seq`), a Room migration and a compatibility path for old clients. Deferred.
- **Rejecting old timestamps on the server**: would reject legitimate offline edits instead of delivering them; the server now bumps accepted writes instead.
- **Blocking sync while the clock is wrong**: hostile on offline-first devices and unnecessary once the retry exists.
