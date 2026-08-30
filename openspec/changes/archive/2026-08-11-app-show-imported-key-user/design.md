## Context

The app signs every request with the imported seed (`ApiClient`, `Signer`). The server change `server-key-user-association` adds `GET /api/keys/me`, which returns the username for the signing key. This change wires that lookup into the settings UI. The import/clear flow is being added in `import-auth-key-from-file`; this change layers identity display on top. See proposal.md - Why.

## Goals / Non-Goals

**Goals**
- Show the username the configured server reports for the imported key.
- Keep the lookup strictly informational — it must never block saving or sync.

**Non-Goals**
- No storing of the username in `SettingsStore` (the server remains the source of truth).
- No authorization UI or user switching.

## Decisions

**D1: `ApiClient.fetchKeyIdentity()` returns `{keyId, username?}` via a new method reusing the existing signer.**
It follows the same pattern as `pull`/`delete` (`GET` + timestamp + signature + `checkAuthError`), but is intentionally *outside* the `SyncApi` interface — sync should not depend on identity lookup, only the settings flow should.

**D2: The lookup happens in `SettingsViewModel` after import and after save, non-blocking.**
`importKey`/`save` persist the key first (existing behavior), then launch a `viewModelScope` coroutine that calls `fetchKeyIdentity()` and updates `keyUser` state. Any failure (network, 401, missing endpoint on an older server) is swallowed into a "no identity" presentation. Alternative considered: calling during `save()` synchronously — rejected, it would delay saving and break the non-blocking requirement.

**D3: UI shows three states: username, "sem utilizador associado", or nothing (lookup failed).**
To distinguish "no user on server" from "server unreachable", the ViewModel tracks whether a lookup succeeded. When it didn't, the screen shows nothing extra rather than a fabricated name or an error banner. Alternative: showing an error — rejected, the identity is auxiliary and a failure banner would alarm users over a non-critical feature.

## Risks / Trade-offs

- [Older server without `/api/keys/me`] → 404 → treated as lookup-no-result, key remains usable; degrades gracefully until the server change ships.
- [401 during lookup when the key really is registered] → surfaced as lookup-unknown; sync itself still surfaces real auth faults via `SyncFault`, so a genuine bad key is not hidden.
- [Username changes on the server but not re-fetched] → Lookup runs on import and save; a stale display self-corrects on the next save/import.

## Migration Plan

Dependent on the server endpoint shipping first (or in parallel). Feature-flagged by server support: without the endpoint the app simply shows no identity. No data migration; `SettingsStore` schema unchanged.

## Open Questions

None.