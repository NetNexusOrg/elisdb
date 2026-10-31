# Tasks

## 1. Server: record the change

- [x] 1.1 Add the idempotent `client_audit.details` migration (TEXT, nullable).
- [x] 1.2 In `save_client`, read the stored row (all fields) and photo ids before updating; compute field changes (null/"" equivalence, 6-decimal coordinates, 200-character text truncation) and photo changes (added/removed/reordered).
- [x] 1.3 Persist the details JSON on edit; store `photos.added` on create; skip the audit entry when the diff is empty.
- [x] 1.4 Keep the diff inside the existing transaction so concurrent pushes cannot interleave with the read.

## 2. Server: expose the change

- [x] 2.1 Add `AuditFieldOut`, `AuditPhotosOut`, `AuditDetailsOut` and `AuditEntryOut.details` in `models.py`.
- [x] 2.2 Parse `details` in `Database.client_history`; return `null` for legacy rows.
- [x] 2.3 Server tests: field diff, coordinates, photo added/removed/reordered, create photos, no-op push records nothing, NULL details survives the endpoint.

## 3. App: consume and render

- [x] 3.1 Extend `AuditEntryDto` with `details` (`AuditDetailsDto`, `AuditFieldDto`, `AuditPhotosDto`), tolerant of unknown fields.
- [x] 3.2 `ClientLogViewModel` resolves photo ids to local URIs from the observed client (no extra query) and exposes them alongside the entries.
- [x] 3.3 `ClientLogScreen`: field lines (label, struck-through old value, arrow, new value; merged "Localização"), photo lines (added with up to four thumbnails, removed, reordered), legacy entries unchanged.
- [x] 3.4 Keep the 24-hour timestamp, day grouping and the loading/error/empty states intact.

## 4. Verification

- [x] 4.1 `pytest` in `elisdb-server/` green.
- [x] 4.2 `:app:testDebugUnitTest` and `:app:lintDebug` green.
- [x] 4.3 Instrumented `ClientLogScreenTest` (rich entry, photos, legacy entry) green.
- [x] 4.4 Exercise a real edit with fields and photos on the emulator against the dev server and inspect the log page.
- [x] 4.5 `openspec validate client-log-details --strict`.

## 5. Documentation

- [x] 5.1 `elisdb-android/docs/uso.md`: the log lists the fields and photos changed by each edit.
