## Context

See proposal.md - Why. Review findings map to concrete fixes across the data, location, and UI layers; all are implementation-level, so no spec changes.

## Goals / Non-Goals

**Goals**

- Failures are visible (message) and never leave `saving`/UI stuck.
- Location lookup cannot hang indefinitely.
- Client + photos persist atomically; no orphaned files.
- Loading states are explicit; the detail map stays non-interactive but scrollable; the viewer never spins forever.
- Remove naming/intent/index pitfalls.

**Non-Goals**

- Not changing the data model, navigation graph, or spec'd behaviors.
- Not adding new screens or flows.

## Decisions

**1. Error handling (items 1, 12).** Wrap `repository.save`/`deleteCliente`/photo staging in `runCatching` and map failures to the existing `message` field; always reset `saving`. Wrap the `startActivity` in `openInMap` in try/catch (falling back to the existing "no navigation app" snackbar).

**2. Location timeout/fallback (item 2).** In `LocationService.getCurrentLocation`, race the FusedLocationProvider call against a `withTimeout` (e.g. 10s) and, on timeout/failure, fall back to the last known location before returning null. Rationale: emulators and indoor devices return null from `getCurrentLocation`; the form already handles a null result with a message.

**3. Transactional save (item 3).** Move the client insert/update + photo reconciliation into a Room `@Transaction` (a DAO method annotated `@Transaction` that performs the work, or a suspend function using `database.withTransaction`). The repository calls that single transaction. Rationale: atomicity between the client row and its photos.

**4. Photo lifecycle (items 4, 6).** Track staged-photo files in the ViewModel and delete them in `onCleared()` if the form was never saved (a boolean flips on successful save). Downscale gallery images in `stagePhoto` by decoding with an inSampleSize/`BitmapFactory` and re-encoding to JPEG (max ~1280px), consistent with camera `setJpegQuality(80)`.

**5. Room schema export (item 5).** Set `exportSchema = true`, add a `room.schemaLocation` argument in `build.gradle.kts` pointing to `app/schemas`, and commit the generated schema so `AppDatabaseMigrationTest` can validate against a reference schema.

**6. Loading/UX (items 7, 8, 9).** Add `CircularProgressIndicator` when `Loading`/`isLoading`; on the detail screen the loading map shows a placeholder box while loading. Make the detail map scroll-through by keeping it non-interactive but removing the blanket `setOnTouchListener` swallow only where needed — instead keep `interactive=false` (no zoom/pan) while letting the parent `verticalScroll` consume drags. Photo viewer: if `fotos` is empty, show a "Sem fotos" message instead of an infinite spinner.

**7. Maintainability (items 10, 11, 13).** Rename `ClientRepository` interface → `ClienteRepository` (impl already named so; update references/tests). Pass the tapped `Foto` (or its index) through the click callback instead of `data.fotos.indexOf(foto)`. 

## Risks / Trade-offs

- [A transaction wrapper changes the repository's failure mode] → `runCatching` at the ViewModel converts any throwable into a visible message; the single-transaction guarantee prevents partial writes.
- [Adding schema export generates files] → new `app/schemas/<version>/...json` committed once; required only for future migration safety.
- [Downscaling changes photo quality/size] → bounded to a max dimension with JPEG re-encode; acceptable for app-local thumbnails/fullscreen viewing.
- [Location timeout increases perceived latency] → 10s cap with last-known fallback; existing message path still covers the null case.

## Migration Plan

- No data migration: schema version stays 2; only schema *export* is enabled. New downscaled photos apply to newly staged images.
- Rollback: revert per-file changes; no irreversible data impact beyond the code change.

## Open Questions

None.
