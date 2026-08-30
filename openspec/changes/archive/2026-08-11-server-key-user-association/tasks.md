## 1. Server: resolve authenticated key identity

- [x] 1.1 Add `Database.get_key_by_public_key(public_key)` returning the key record (`id`, `public_key`, `comment`) or `None`.
- [x] 1.2 Change `require_auth` to resolve the matching key record and attach it to the request (e.g. `request.app.state.authenticated_key`) instead of returning early on boolean success.
- [x] 1.3 Confirm `push`, `pull`, `clients`, `delete_client` still pass (existing behavior unchanged by 1.2).

## 2. Server: me endpoint

- [x] 2.1 Add `GET /api/keys/me` (authenticated) returning `{key_id, username}` with `username` null when the key has no `comment`.
- [x] 2.2 Update server tests (if any) for the new endpoint: valid key holder sees their username; externally registered key without comment returns `username: null`.

## 3. Server: mandatory user name on issuance

- [x] 3.1 In `app/keys.py`, refuse `issue` when the user name is missing or blank (no key added to the allowlist).
- [x] 3.2 Test: `python -m app.keys issue ""` (or missing arg) fails without adding a key; `issue nome` still registers with the name as `comment`.

## 4. Verification

- [x] 4.1 Run the server test suite and the `keys` CLI flows end to end.
- [x] 4.2 Confirm `GET /api/keys/me` works against a running local server for both named and unnamed keys.