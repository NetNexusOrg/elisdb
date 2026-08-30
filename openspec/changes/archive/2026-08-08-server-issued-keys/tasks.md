## 1. Server schema: key comments

- [x] 1.1 Add an optional `comment` column to the `keys` table in `SCHEMA` and an idempotent `ALTER TABLE ... ADD COLUMN comment TEXT` migration on connect (guarded by `PRAGMA table_info(keys)`).
- [x] 1.2 Extend `add_key(key_id, public_key, comment="")` and `list_keys()` to store and return the comment alongside id, public key, and created_at.

## 2. Server: issue command

- [x] 2.1 Add `python -m app.keys issue <user-name>` that generates a keypair, registers the public key with comment `<user-name>`, prints the private seed for import, and the registered public key.
- [x] 2.2 Extend `python -m app.keys add <id> <public_key_b64> [comment]` to accept an optional comment, keeping external public-key registration working.
- [x] 2.3 Verify the issued key authenticates a signed request against the allowlist (end-to-end sign+verify).

## 3. Server: visibility of comments

- [x] 3.1 Update `python -m app.keys list` to print the comment per key.
- [x] 3.2 Confirm `GET /api/keys` includes the `comment` field (through `store.list_keys()`).

## 4. Android: import in settings

- [x] 4.1 Add an import action to the settings screen (from `add-settings-page`) that accepts a server-issued base64 seed, validates it as a 32-byte decoded seed, and persists it as the signing-key override in the settings store.
- [x] 4.2 Reject invalid seeds with an inline error without overwriting the stored key.
- [x] 4.3 Confirm `ApiClientProvider`/`Signer` use the imported seed for sync once saved (fallback to build config when none imported).

## 5. Tests and verification

- [x] 5.1 Server tests: comment column migration on an existing DB, issue/add with/without comment, list & `GET /api/keys` include comment, issued key authenticates, external add still works.
- [x] 5.2 App tests: import valid seed persisted, invalid seed rejected without overwrite, imported key used by the signer side store.
- [x] 5.3 Build and run both server and app test suites; no regressions.