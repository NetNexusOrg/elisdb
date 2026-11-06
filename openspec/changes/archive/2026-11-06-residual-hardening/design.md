# Design

## Context

See `proposal.md` for motivation. This change closes residual P3 findings after the P0/P1/P2 changes. It touches both components but deliberately avoids changing the core data model or sync protocol beyond additive fields.

Current constraints that shape the approach:

- Android has one shared Ktor client (`remote/ApiClient.kt`) and a runtime `ServerUrlPolicy`. The platform network security config cannot express private IPv4 CIDR ranges, so runtime policy remains the enforceable point for LAN HTTP.
- Photos are currently read into memory before a push and downloaded into memory before materialization (bounded by the 20 MB server limit, but still avoidable).
- The server writes each client push through per-record SQLite transactions; there is no batch-level transaction or idempotency table yet.
- `client_audit.action` currently allows only `create` and `edit`; SQLite requires a table rebuild to widen the CHECK constraint.
- `PrefsSettingsStore` currently falls back to plain preferences if encrypted storage cannot be opened.
- The release workflow already has a Room migration gate; the new sync smoke gate should reuse the existing disposable-server pattern rather than add a second release path.

## Goals / Non-Goals

**Goals:**

- Enforce cleartext restrictions at the single HTTP-client boundary and make bypasses detectable.
- Store signing keys only in encrypted storage; fail visibly when secure storage is unavailable.
- Bound Android push memory by uploading one photo at a time and make retries idempotent.
- Reject downloaded photo payloads whose magic bytes do not match the declared MIME.
- Apply mirrored EXIF orientations correctly.
- Paginate edit history, record deletions in the audit trail, and keep old clients working.
- Make push retries safe with an optional idempotency key.
- Add a sync smoke gate to release CI and a tested backup/restore procedure.

**Non-Goals:**

- Changing the trust model (every key still has access to the shared dataset).
- Encrypting the server database at rest.
- Replacing the sync protocol or the LWW/tombstone model.
- Adding a full mobile device-management or per-client ownership model.
- Backing up operator configuration files that contain private seeds; those need a separate encrypted backup process.

## Decisions

### 1. Network policy is enforced in the shared HTTP client

`ServerUrlPolicy` remains the single source of truth for URL validation. Add a Ktor request plugin (or a wrapper around `HttpClient`) that validates every outbound URL before connecting and checks redirect `Location` values; configure the client with redirects disabled or a custom redirect validator. Apply the same validator to `ApiClient`, photo downloads/uploads, release downloads, time, identity, and history calls.

Keep the Android `network_security_config` permissive for cleartext only where the platform cannot express private CIDRs, and add an architecture/lint check that forbids constructing another HTTP client outside `ApiClient`. This is a compromise: the runtime policy is the real guard, while the platform config cannot accidentally be relied on as the only guard.

**Alternatives considered:** setting `cleartextTrafficPermitted=false` globally would break the supported private-LAN HTTP deployment; adding every possible private IP to `network_security_config` is unbounded and not maintainable.

### 2. Secure storage fails closed

Replace the plaintext fallback in `PrefsSettingsStore` with an encrypted-only store. On startup, the store attempts `EncryptedSharedPreferences`; if it cannot be created, reads of the legacy plaintext values remain available (so the app can start), but writes of a signing key fail with a typed `SecureStorageUnavailable` error that the settings screen surfaces.

Migration reads the legacy plaintext values, writes the encrypted copy, reads it back to verify, and only then removes the plaintext copy. A verification failure keeps the plaintext values and reports a migration warning. No new key is ever written in plaintext.

**Alternatives considered:** keeping the fallback and warning only in logs does not meet the "not readable from plain shared preferences" contract; failing app startup is too aggressive for users who only need read-only access to existing data.

### 3. Push streams photos and uses a batch idempotency key

Build on the P0 two-phase push: first send record metadata referencing photo ids, then upload each missing or changed photo with one `PUT /api/photos/{id}` at a time. Do not put base64 photo bytes in the push body. The client reads one file, computes/streams its bytes, uploads it, and releases the buffer before moving to the next photo. Missing files are reported and skipped without aborting unrelated records.

Each push batch gets a UUID idempotency key. The key is held with the batch while it is retried; it is regenerated when the batch content changes. Send it in a dedicated header. The server stores `(key, payload_hash, response_json, created_at)` and checks it before processing.

**Alternatives considered:** keeping base64 in the push body is simpler but retains the memory spike and makes retries re-upload large payloads. A single request-level key without a payload hash cannot detect accidental key reuse with different content.

### 4. Server idempotency is transactional with the batch

Extend `Database.save_client` to accept an optional shared connection/transaction. The push endpoint opens one `BEGIN IMMEDIATE` transaction for the batch, checks the idempotency table, processes records, records the response, and commits. A repeated key with the same payload returns the stored response without touching records, photos, or audit. A repeated key with a different payload returns 409. If the process crashes before commit, nothing is applied and a retry is safe.

This is a moderate refactor of `database.py`; keep the existing per-record behavior for requests without a key so older clients are unaffected.

**Alternatives considered:** storing the idempotency result after per-record commits can duplicate audit entries on a crash between processing and storing. A separate in-memory cache would not survive restarts and would not work across workers.

### 5. Downloaded photos are validated before storage

Download to a temporary file (streaming), read the first bytes, sniff the image format, and compare with the server-declared MIME. Accept only JPEG, PNG, and WebP. On mismatch or unknown bytes, delete the temporary file, keep the previous local file and row, and surface a retryable sync error. Only after validation move the temp file into the photo store and commit the row.

**Alternatives considered:** validating after writing into the store complicates rollback and can leave a bad file referenced by a row; in-memory validation retains the allocation this change is trying to remove.

### 6. EXIF orientation uses a full transform matrix

Map all eight EXIF orientations to a rotation plus optional horizontal/vertical flip. Apply the transform to the decoded bitmap before WebP encoding. Add fixture-based tests for orientations 1–8, including mirrored cases. The existing "no re-encode" rule still applies to already-stored/synced photos.

**Alternatives considered:** handling only rotation leaves mirrored selfies wrong; relying on the image viewer to correct orientation would not fix the stored pixels or the server copy.

### 7. History pagination and delete audit are additive

Add `limit` and `after_id` query parameters to `GET /api/clients/{id}/history`; return `next_after_id` when more entries exist. Keep the no-parameter response as the full history so old clients keep working. The app's history screen requests pages and appends them, with a "load more" affordance.

Widen `client_audit.action` to include `delete`. `delete_client` accepts the authenticated actor's key id and user name, records the delete entry in the same transaction as the tombstone and deletion, and stores the deleted client's name/identifier in details. Rejected deletes create no entry. Add an index on `(cliente_id, id)` to keep pagination efficient.

**Alternatives considered:** offset pagination is simpler but drifts when new audit entries are added; keyset pagination by audit id is stable and cheap.

### 8. Release smoke gate runs against a disposable server

Add a `sync-smoke` CI job that starts the server with a temporary data directory and a freshly provisioned test key, then runs a signed push (with a small photo), pull, and delete. Verify the pulled record, the photo bytes, and the tombstone. The release job depends on this job for `v*` tags. The job must not use production data or real keys.

For Android-side coverage, add a unit/integration test with the Ktor mock engine that exercises chunking, idempotency-key reuse, and streaming photo upload order; the CI smoke gate remains the end-to-end server check.

**Alternatives considered:** running the full Android instrumented suite as the gate is slow and already non-blocking for main/tag; the disposable-server smoke gives release confidence without that cost.

### 9. Backup uses SQLite's online backup API plus a manifest

Add `elisdb-server/scripts/backup.py` (or `app/backup.py` CLI) that:

1. Opens the live database and uses `sqlite3.Connection.backup()` to a temporary database file.
2. Reads the backup's `fotos` rows and copies each referenced photo to the backup directory.
3. Writes a JSON manifest with schema version, timestamp, database SHA-256, and each photo's relative path and SHA-256.
4. Verifies the manifest and runs `PRAGMA integrity_check` before declaring success.

Restore verifies the manifest and integrity, stages the database and photos in a temporary directory, moves the current data aside, swaps in the restored data, and starts the server. On any failure, it restores the previous data directory.

**Alternatives considered:** copying `elisdb.db` and the photos directory with `cp` is not safe with WAL; stopping the server for every backup is operationally expensive.

## Risks / Trade-offs

- **Idempotency batch transaction refactor** → Keep the no-key path unchanged; add focused tests for same-key/same-payload, same-key/different-payload, crash-before-commit, and audit non-duplication. Roll out behind the optional header so old clients are unaffected.
- **Runtime cleartext policy cannot stop arbitrary third-party code** → Add an architecture test/lint rule that forbids new HTTP clients, and keep the platform config as restrictive as the private-LAN requirement allows.
- **Secure storage unavailable on some devices** → Reads of existing data still work; only new key writes fail with a clear message. Document the recovery path (device with a working keystore).
- **Streaming photos changes push ordering** → Upload strictly before the metadata push for each batch, and use stable ordering; test a partial upload failure and retry.
- **Downloaded photo validation may reject legitimate images with misleading MIME** → Accept only the three formats the server already stores, and report a clear error; the server should declare the sniffed MIME.
- **History pagination changes the app's UI behavior** → Keep the full-history default for compatibility and add a bounded page size in the app; test old and new clients.
- **Backup may be large** → Stream/copy files, report progress without printing secrets, and document expected disk space. Do not include `configs/` by default.
- **Sync smoke adds CI time** → Reuse the Docker build cache and run the smoke only for release tags and manual dispatches, not every PR.

## Migration Plan

1. Android: ship the encrypted-only settings store with a lossless migration; keep reading legacy values until migration succeeds. No database migration required for network policy, EXIF, or downloaded-photo validation.
2. Server: migrate `client_audit` to allow `delete` and add the idempotency table plus history index. The migration is additive/idempotent and must preserve existing audit rows.
3. Deploy the server before or with the app. Old app clients continue to work: history without pagination returns the full list, and push without an idempotency key uses the existing per-record path.
4. Add the sync smoke job and backup script; run a backup/restore drill on a disposable directory before enabling the release gate.
5. Rollback: the server changes are additive; keep the previous server image available. Android can be rolled back, but the encrypted settings store must keep the same key alias so a rollback does not lose the configuration. If the idempotency table causes trouble, disable the header on the client and fall back to the existing per-record path.

## Open Questions

None blocking. The exact CI runner image and backup retention policy are operational choices that do not change the specs.
