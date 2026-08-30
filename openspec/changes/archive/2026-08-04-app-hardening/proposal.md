## Why

A code review of the app surfaced robustness gaps, data-integrity issues, UX rough edges, and maintainability nits. All fix the app without changing its intended behavior — saves/delete/location/map work the same, just more reliably — so this change opts out of specs (`skip_specs: true`).

## What Changes

- Wrap save/delete/photo-staging operations in error handling so failures surface a message instead of leaving the UI stuck or crashing.
- Add a timeout/fallback to current-location lookup so "Usar localização atual" cannot hang when GPS is unavailable.
- Wrap the client+photos save in a single database transaction.
- Delete orphaned staged photos when the form is cancelled; downscale gallery images when staging them.
- Enable Room schema export for future migration validation.
- Show loading indicators instead of blank screens on the list and detail screens.
- Allow scrolling past the non-interactive detail map; avoid a perpetual spinner in the photo viewer.
- Rename the repository interface to the Portuguese convention, use stable photo identity instead of `indexOf`, and guard the navigation intent.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Every change is an implementation-level fix or polish; the spec'd behaviors (list, create/edit/save, detail, delete, use current location, non-interactive detail map, photo viewing) are preserved. Opts out of specs via `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/form/ClientFormViewModel.kt` — save error handling, `onCleared` cleanup.
- `app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailViewModel.kt` — delete error handling.
- `app/src/main/java/com/example/elisdb/location/LocationService.kt` — timeout/fallback.
- `app/src/main/java/com/example/elisdb/data/ClienteRepository.kt` — transactional save, photo downscale, staged-photo cleanup.
- `app/src/main/java/com/example/elisdb/data/ClientRepository.kt` — rename to `ClienteRepository` (interface).
- `app/src/main/java/com/example/elisdb/data/local/AppDatabase.kt` — `exportSchema = true` + schema location.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt` — loading indicator.
- `app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailScreen.kt` — loading indicator, scrollable map, stable photo click, guarded intent.
- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt` — no infinite spinner.
- `app/build.gradle.kts` — Room schema export directory.
- Tests: repository/ViewModel tests updated/added where behavior changed.
