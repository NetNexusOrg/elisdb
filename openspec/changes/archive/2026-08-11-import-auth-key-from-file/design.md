## Context

The settings screen currently has a free-text field for the signing key seed (`SettingsScreen.kt`, `SettingsViewModel.kt`), validated against `Signer.fromBase64`. The server operator issues keys with `python -m app.keys issue <nome>`, which prints the private seed and the registered public key. See proposal.md - Why. The `SettingsStore` persistence and `Signer` validation are reused unchanged.

## Goals / Non-Goals

**Goals**
- Replace manual key entry with file import, tolerating both a bare seed and the full `issue` output.
- Show the current key masked, with a clear action that restores the blank-key behavior.

**Non-Goals**
- No server-side changes (`app/keys.py` stays as-is — extraction happens on the client).
- No changes to base URL editing or sync triggering behavior.
- No key format encryption at rest in this change.

## Decisions

**D1: Use `ActivityResultContracts.OpenDocument` for picking.**
The app already uses `ActivityResultContracts.PickVisualMedia` for photos but that contract is media-only. `OpenDocument` is the correct picker for arbitrary files (e.g. a `.txt` or key file) and returns a content URI we can read via `contentResolver`. Alternative considered: `GetContent` — functionally similar but `OpenDocument` is the recommended contract for document pickers and plays well with persistent access needs.

**D2: Parse the seed in the ViewModel from the file's text.**
`SettingsViewModel` receives the `Uri` (or already-read text) and runs extraction + `Signer.fromBase64` validation, mirroring the current `isValidSeed` path. Extraction searches for the first line matching the `issue` output shape (a base64 seed, optionally after `private seed (import into app):`), falling back to treating the whole trimmed file content as the seed. On failure, the screen shows the existing "seed inválida" message and the stored key is untouched. Alternative considered: parsing in a separate util — rejected, footprint is small and validation logic already lives in the ViewModel.

**D3: Masked display + clear button, no text field.**
The key is rendered as a fixed masked string (e.g. a marker or `••••…`) — never the raw value — alongside an "Importar" button and a clear action. Clearing persists a blank value via the existing `settingsStore.save`, which the sync layer already treats as "no key configured". This keeps the current "empty configuration accepted / sync refused locally" contract without UI for typing keys.

## Risks / Trade-offs

- [Large or non-text file picked] → Read is bounded; parse/validate fails cleanly with the invalid-content message and no key change.
- [User picks a file containing the full issue output but the seed line format changes server-side] → Extraction is tolerant (bare seed also accepted); a format change degrades to "bare seed" imports, not a hard failure.
- [Masking hides whether the key is the *expected* one] → The clear + re-import flow gives the user a way to replace a wrong key; this mirrors the prior manual-entry risk without exposing the secret on screen.

## Migration Plan

Pure UI change; no data migration. Existing stored keys remain valid and are simply displayed masked after the update. Rollback is reverting the settings screen changes; `SettingsStore` schema is untouched.

## Open Questions

None.