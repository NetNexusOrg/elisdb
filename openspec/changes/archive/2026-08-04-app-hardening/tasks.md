## 1. Error Handling

- [x] 1.1 Wrap `ClientFormViewModel.save()` and `deleteCliente()` (in `ClientDetailViewModel`) in `runCatching`, resetting `saving` and surfacing a message on failure
- [x] 1.2 Guard `openInMap`'s `startActivity` in try/catch, falling back to the existing "no navigation app" snackbar

## 2. Location Robustness

- [x] 2.1 Add a timeout (e.g. 10s) and last-known-location fallback to `LocationService.getCurrentLocation` so it cannot hang

## 3. Data Integrity

- [x] 3.1 Wrap client + photos save in a single Room `@Transaction`
- [x] 3.2 Enable Room schema export (`exportSchema = true` + schema location in `app/build.gradle.kts`) and commit the generated schema

## 4. Photo Lifecycle

- [x] 4.1 Track staged photos in `ClientFormViewModel` and delete orphans in `onCleared()` when the form was not saved
- [x] 4.2 Downscale/compress gallery images in `stagePhoto` (max ~1280px, JPEG re-encode)

## 5. Loading / UX

- [x] 5.1 Show a `CircularProgressIndicator` for `Loading` on the client list and for `isLoading` on the detail screen (with a placeholder while the map loads)
- [x] 5.2 Keep the detail map non-interactive but allow scrolling past it (don't swallow parent scroll)
- [x] 5.3 Photo viewer: show a message instead of an infinite spinner when there are no photos

## 6. Maintainability

- [x] 6.1 Rename `ClientRepository` interface to `ClienteRepository` (update references and tests)
- [x] 6.2 Pass the tapped `Foto`/index directly to the photo-click handler instead of `data.fotos.indexOf(foto)`

## 7. Build and Verification

- [x] 7.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm all pass
- [x] 7.2 Run the instrumented tests (`connectedDebugAndroidTest`) and confirm they pass
- [x] 7.3 Emulator smoke test: create + edit + delete a client, use-current-location fallback, save-failure message, loading indicators, and scroll past the detail map
