# Design

## Context

`ClientDetailScreen` used `AlertDialog` to render `GET /api/clients/{id}/history` (`AuditEntryDto`: `action`, `username`, `key_id`, `ts`). The dialog had no scrolling, no retry and no room for the actor, the action and the timestamp. Histories grow with every edit, so the dialog was the wrong container from the start.

## Decisions

### A page, not a bigger dialog

A dedicated destination (`ClientLog` NavKey) matches the rest of the app: the detail screen already navigates to the form, the photo viewer and the map. Back returns to the client, and the log no longer competes with the detail content for space.

### History state in its own ViewModel

`ClientLogViewModel` owns the request, the loading/error flags and the retry. It also observes the client locally to fill the page header (name, contract) and to know whether local pending changes exist. `ClientDetailViewModel` keeps only what the detail screen needs, and `fetchHistory` stops being a detail-screen concern.

### Entries grouped by day

Server history arrives oldest → newest. The page groups entries by `dd/MM/yyyy` (device timezone, `Locale.getDefault()`), keeping chronological order, and each entry shows the time as `HH:mm` (24-hour clock, preserving the existing spec requirement). This turns a flat list into a readable timeline without adding grouping machinery to the server.

The server returns `action` = `create` / `edit`; unknown actions fall back to the raw value so new server actions still render.

### States with actions

- Loading → centred spinner.
- Failure → message + **Tentar novamente**, which re-runs the request (covers transient network errors; nothing is cached).
- Empty + pending → note that the history appears after sync (the client exists only locally).
- Empty → "no history recorded on the server".

## Risks / trade-offs

- The log page asks the server every time it opens; a client with a long history makes the page wait for the network. The local header renders as soon as the client is observed, and the spinner is bounded by the existing HTTP timeouts (30 s request).
- Grouping by local day can split the same server day differently across timezones; the timestamps themselves are absolute (epoch seconds), so no information is lost.
- The detail screen no longer fetches the history, so an edit followed by opening the log fetches once — same as before.

## Alternatives considered

- **Keep the dialog and add scrolling**: still a modal over the detail screen, still cramped, and the retry action would need a second button row.
- **Fetch the history in the detail screen and pass it to the log**: couples two screens and refetches the whole detail on back navigation.
