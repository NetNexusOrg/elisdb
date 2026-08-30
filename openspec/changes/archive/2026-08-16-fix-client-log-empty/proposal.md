## Why

The client "Log" view silently shows "Não há histórico registado" in two wrong situations:

1. **Fetch errors are swallowed.** `Navigation.kt` wraps `fetchClientHistory` in `catch (e: Exception) { emptyList() }`, so any network/auth failure is returned as an empty list. The ViewModel then renders the "no history" state instead of the error, hiding the real cause (offline, unregistered key, server error).
2. **Pending (unsynced) clients show "no history".** When a client is created or edited, the change stays `pending` locally and is only pushed on the next sync (15 min periodic or manual). The Log fetches from the server, so a just-created/edited client has no server audit entries yet and the row shows "Não há histórico registado" — misleading, since the user just made a change that will be logged after the next sync.

On a physical device this is especially confusing: after creating/editing a client, opening the Log shows nothing, making the feature look broken.

## What Changes

- Fetch errors are surfaced instead of swallowed: a failed history fetch shows an actionable error message, not "no history".
- A client with local pending changes (created/edited but not yet acknowledged) shows a clear note in the Log that the history will appear after synchronization, instead of a bare "no history".

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the Log view distinguishes a fetch failure from genuinely empty history, and notes when a client has unsynced local changes.
- `client-sync`: history fetch failures propagate as errors rather than being converted to an empty list.

## Impact

- **App**: `Navigation.kt` (stop swallowing fetch errors), `ClientDetailViewModel` (carry pending state / error already present, ensure error path is reachable), `ClientDetailScreen` (Log dialog text: error message vs "sem histórico" vs "histórico aparece após sincronização").
- **Behavior**: a failed fetch shows the error; a pending client shows that the log will populate after sync; genuinely empty history still shows "no history".
- **Tests**: unit test for the ViewModel error/pending rendering; instrumentation test for the Log dialog states.
