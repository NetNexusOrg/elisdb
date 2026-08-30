## 1. Auth building blocks

- [x] 1.1 Add `cryptography` to `../elisdb-server/requirements.txt` and install it in the venv.
- [x] 1.2 Add the `keys` table to the SQLite schema (`id TEXT PRIMARY KEY`, `public_key TEXT`, `created_at INTEGER`) in `app/database.py` with methods `add_key`, `remove_key` and `list_keys`.
- [x] 1.3 Create `app/auth.py` implementing message construction (`METHOD\nPATH\nSHA256(body)\nTIMESTAMP`), and the ed25519 `verify` function that returns True/False given a public key, signature, and the constructed message.

## 2. Request verification

- [x] 2.1 Implement the timestamp check (reject outside a ±300 second window around the server clock).
- [x] 2.2 Implement the FastAPI `require_auth` dependency: read `X-Elisdb-Timestamp`, `X-Elisdb-Signature` (and optional `X-Elisdb-Key`), reject missing/malformed headers with `401`, then verify the timestamp window and the signature against all allowlisted keys; raise `401` on failure.

## 3. Wiring and management

- [x] 3.1 Apply `require_auth` as a dependency to `POST /api/push`, `GET /api/pull`, `GET /api/clients`, and `DELETE /api/clients/{id}`; leave `GET /health` open.
- [x] 3.2 Add a `GET /api/keys` endpoint listing allowlisted keys.
- [x] 3.3 Add a CLI module `app/keys.py` (`add`, `remove`, `list`) operating on the same database.

## 4. Verification

- [x] 4.1 Add pytest tests: signed request accepted; missing signature rejected (401); unknown key rejected; tampered body/signature rejected; stale timestamp rejected; future timestamp rejected; removed key no longer accepted; health endpoint accessible without a signature; key add/remove/list via the CLI/database.
- [x] 4.2 Run `python -m pytest` in `../elisdb-server` — all tests pass (including the existing db-server tests, updated to sign their requests).
- [x] 4.3 Manual check: run uvicorn, generate a keypair, register the public key, and confirm a signed request is accepted while an unsigned/tampered/stale request is rejected with `401`.