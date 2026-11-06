# Tasks

## 1. Android network policy

- [x] 1.1 Add a request-time network policy check that reuses `ServerUrlPolicy` for every outbound URL and validates redirect destinations; configure the shared client to reject disallowed redirects. Verify with unit tests for public HTTP (rejected), private HTTP (allowed), public HTTPS (allowed), unsupported schemes, and redirect to a public cleartext host.
- [x] 1.2 Route time, identity, history, sync, photo transfer, and release download through the same shared client and policy. Verify with an architecture/lint check or test that fails if another HTTP client is constructed outside `ApiClient`.
- [x] 1.3 Update the Android network security configuration and document the private-LAN HTTP exception. Verify lint passes and the documented manual matrix (private HTTP works, public HTTP fails closed) is reproducible.

## 2. Encrypted-only settings storage

- [x] 2.1 Remove the plaintext fallback from `PrefsSettingsStore`; return a typed secure-storage-unavailable error on writes when encrypted storage cannot be opened. Verify with unit tests using a failing store fake that no signing key is written in plaintext.
- [x] 2.2 Keep the lossless migration: read legacy plaintext values, write the encrypted copy, read it back, and only then remove the plaintext copy. Verify with tests for success, verification failure, and app start with existing data.
- [x] 2.3 Surface the secure-storage error in the settings screen with an actionable message. Verify with a ViewModel/UI test that the user sees the error and the previous configuration is unchanged.

## 3. Streaming photo push and idempotency

- [x] 3.1 Change the push to send record metadata without base64 photo bodies and upload each missing or changed photo one at a time through the photo endpoint. Verify with a unit test using a fake photo store that reads one photo at a time and releases bytes before the next.
- [x] 3.2 Report and skip a missing photo without aborting the client's remaining photos or unrelated records. Verify with tests for a missing file and for a later successful retry.
- [x] 3.3 Generate a stable idempotency key per push batch, reuse it on timeout/transient retries, and regenerate it when batch content changes. Verify with unit tests that assert the same header on retry and a different key after an edit.
- [x] 3.4 Add a Ktor mock-engine integration test for a push with several photos: assert metadata-only records, sequential photo uploads, stable ordering, and idempotency-key reuse. Verify the test fails if photo bytes are embedded in the push body.

## 4. Photo validation and EXIF

- [x] 4.1 Download photos to a temporary file, sniff JPEG/PNG/WebP magic bytes, and compare with the declared MIME before moving the file into the photo store. Verify with tests for matching bytes, MIME mismatch, unknown bytes, and preservation of the previous local file. _(A validação ocorre antes de qualquer escrita; o `FilePhotoStore` grava em ficheiro temporário e faz rename atómico.)_
- [x] 4.2 Apply all eight EXIF orientations (rotation and mirroring) in `PhotoEncoder` using a full transform matrix. Verify with fixture-based tests for orientations 1–8, including at least one mirrored case.
- [x] 4.3 Document the accepted downloaded image formats and the mirrored-orientation behavior in the Android photo docs. Verify the documented cases match the tests.

## 5. Server history, delete audit, and push idempotency

- [x] 5.1 Add an idempotent schema migration that widens `client_audit.action` to include `delete`, creates the push-idempotency table, and adds the history index. Verify existing audit rows survive the migration and a fresh database gets the new schema.
- [x] 5.2 Add `limit` and `after_id` to the history endpoint and return `next_after_id` when more entries exist; keep the no-parameter full-history response. Verify with tests for one page, next page, last page, invalid limit, and the legacy full response.
- [x] 5.3 Record delete audit entries with actor user name, key id, timestamp, action `delete`, and deleted-client details in the same transaction as the tombstone. Verify with tests for named/unnamed actors, history visibility, and no entry for a rejected delete.
- [x] 5.4 Implement optional push idempotency: same key and payload returns the stored response without re-persisting; same key with a different payload returns 409; requests without a key keep the existing per-record path. Verify with tests for duplicate audit prevention, photo non-re-upload, and rollback on a failed batch.
- [x] 5.5 Update the app's history fetch and screen to request pages and append results with a load-more action, while keeping the full-history fallback. Verify with a ViewModel test for first page, next page, empty page, and load failure.

## 6. Consistent backup and restore

- [x] 6.1 Implement the backup procedure using SQLite's online backup API, copying every referenced photo and writing a manifest with checksums and schema version. Verify with a test that backs up while writes are active and checks database integrity and photo coverage.
- [x] 6.2 Implement the restore procedure with manifest verification, integrity check, atomic swap, and rollback on failure. Verify with tests for a valid restore, corrupt checksum, missing photo, and a failure midway that leaves the original data usable.
- [x] 6.3 Document the backup/restore commands and required environment in the server docs, and add an automated backup → verify → restore round trip. Verify the documented commands run as written on a disposable data directory.

## 7. Release sync smoke gate and documentation

- [x] 7.1 Add a disposable-server sync smoke check that provisions a fresh test key and exercises signed push (with a small photo), pull, and delete, verifying the record, photo bytes, and tombstone. Verify it passes locally against a temporary data directory and prints no secrets.
- [x] 7.2 Wire the sync smoke job into the Android release workflow as a dependency of the release job for `v*` tags. Verify workflow syntax and that a simulated smoke failure prevents the release job.
- [x] 7.3 Document the new release gate and correct the stale osmdroid reference to MapLibre in the project documentation/skills. Verify the documented workflow and the actual code agree.

## 8. Integration verification

- [x] 8.1 Run `cd elisdb-android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and `elisdb-server/.venv/bin/python -m pytest -q`; verify all pass.
- [x] 8.2 Run the documented manual smoke matrix: private HTTP allowed, public HTTP blocked, secure-storage failure path, photo upload/download validation, mirrored EXIF, and paginated history. Verify each outcome matches the specs. _(Executado no emulador `medium_phone`: HTTP público rejeitado na importação e HTTP privado aceite; os restantes casos foram validados pela suite instrumentada `connectedDebugAndroidTest`, 148 testes verdes.)_
- [x] 8.3 Verify `git status` contains no `*.db`, `*.config.json`, keystore, `.env`, or other secrets before requesting a commit.

## Workflow follow-up

- Sync delta specs to the main specs with `/opsx-sync` after implementation.
- Archive the change after the project review requirements are satisfied.
