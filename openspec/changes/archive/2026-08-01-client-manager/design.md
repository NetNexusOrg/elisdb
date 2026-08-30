## Context

`elisdb` is a freshly scaffolded Android project: Kotlin 2.3.20, AGP 9.0.1, Compose BOM 2026.03.01, Material 3, Navigation 3 (`navigation3-runtime`/`ui`) already wired with a single `Main` route. Current code is a placeholder: `MainScreen` shows "Hello Android!", `DataRepository` is a stub returning a fixed list, `MainScreenViewModel` just maps that flow. There is no Room, no permissions declared, no DI. `minSdk 24`, `targetSdk 36`. See `proposal.md` for the motivation; the required behavior is defined in `specs/*`.

## Goals / Non-Goals

**Goals:**
- A vertical slice: list → create/edit → detail with working Room persistence, photos, and map interactions.
- Keep the existing Navigation 3 architecture and MVVM style; no new architectural framework.
- No Google Maps API key anywhere.

**Non-Goals:**
- No auth, cloud sync, offline maps, or advanced map features (see proposal "Fora de âmbito").
- No dependency injection framework (Hilt/Koin) in this change — manual DI keeps the setup small.
- No storage-permission plumbing: photos live in app-private storage, gallery input uses the system Photo Picker.

## Decisions

### 1. Room for persistence (with KSP)
- **Choice**: Room with KSP codegen. Entities `Cliente` and `Foto` (1-N), `FotoDao`/`ClienteDao` as `interface` DAOs returning `Flow` for reads and `suspend` for writes. Client deletion cascades to photos at the repository layer (delete photo files, then delete photo rows and the client row).
- **Alternatives**: raw SQLite (too much boilerplate), SQLDelight (foreign to a pure-Android app), Realm (heavy, different model).
- **Rationale**: Room is the platform-standard ORM; the project already plans KSP (needed for Room) — must pick a KSP version compatible with Kotlin 2.3.20.

### 2. OSMDroid for maps
- **Choice**: OSMDroid (user-confirmed). Shows raster OSM tiles with no API key and no external style URL; works offline only in the sense tiles are cached, but we accept network dependency (per scope).
- **Alternatives**: MapLibre (vector maps, but needs a style JSON/tile provider), Google Maps (requires API key — rejected by requirements).
- **Details**: set `osmdroid.config.Configuration` user agent in an `Application` subclass's `onCreate`; use a draggable marker for location selection and a non-draggable marker on the detail mini-map. No Play Services needed for the map itself.

### 3. FusedLocationProviderClient for "use current location"
- **Choice**: Google Play Services `FusedLocationProviderClient.getCurrentLocation()` as the primary source, requested via runtime location permissions (`ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`).
- **Alternatives**: platform `LocationManager` (older, GPS/GPS_PROVIDER heuristics; kept as a fallback only if no Play Services — noted as risk, not implemented in this change).
- **Rationale**: standard, fused, low-battery; devices without Play Services get a graceful "unavailable" message rather than a crash.

### 4. Photos: CameraX + system Photo Picker, stored in `filesDir`
- **Choice**: CameraX `ImageCapture` writes captured JPEGs directly to `filesDir/photos/<uuid>.jpg`; gallery pick uses `ActivityResultContracts.PickVisualMedia` and copies the returned URI into `filesDir`. The DB stores only the file path.
- **Alternatives**: delegating to the system camera via `TakePicture` (needs a `FileProvider` + more lifecycle plumbing), or saving `content://` URIs (permission leaks across restarts). Direct-to-file capture avoids both.
- **Rationale**: no `FileProvider`, no storage permission, no URI persistence problems. Coil loads thumbnails/fullscreen from `File` paths.

### 5. Navigation 3 with typed keys
- **Choice**: extend the existing `Navigation.kt`/`NavigationKeys.kt` with `@Serializable` `NavKey`s: `ClientList`, `ClientForm(clientId: Long?)`, `ClientDetail(clientId: Long)`, `PhotoViewer(clientId: Long, index: Int)`.
- **Alternatives**: Navigation 2 (adds a second navigation library), plain state hoisting (no back/forward stack).
- **Rationale**: already present in the template; typed keys make arguments type-safe.

### 6. MVVM with manual DI
- **Choice**: an `AppContainer` (simple object graph) created in a custom `Application` class holding the Room DB, repository, and location service; ViewModels receive it through a factory. Follows the template's existing ViewModel-per-screen pattern.
- **Alternatives**: Hilt (extra plugin/processor + facade classes), view-model factory boilerplate per screen.
- **Rationale**: keeps dependencies explicit with minimal machinery for a single-activity app.

### 7. Permissions via Compose activity-result APIs
- **Choice**: `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions)` for camera + location, requested on demand from the form screen (camera before opening the camera, location before "use current location").
- **Rationale**: permission only when needed (least-privilege), consistent with Photo Picker needing no permission.

## Risks / Trade-offs

- **OSMDroid tiles need network** → Accepted by scope (offline maps out of scope); tiles are cached by OSMDroid on disk by default.
- **KSP version must match Kotlin 2.3.20** → Pin the KSP version at the start of implementation (Task 1) and verify with a build before writing code; if the catalog version is wrong, the build fails fast at task 1.
- **No Play Services on some devices** → `FusedLocationProviderClient` wraps failure; show a friendly message and leave the last selected location. Documented fallback (platform `LocationManager`) deferred to a follow-up.
- **OSMDroid crashes without a user agent set** → Set `Configuration.getInstance().userAgentValue` in `Application.onCreate` (Task 1) so all map screens are safe.
- **Fullscreen photo over a camera-shot photo is heavy** → Coil handles downsampling; photos are capped at a reasonable size when captured.
- **CameraX requires lifecycle-aware setup** → The camera is a self-contained composable with its own `LifecycleOwner` scoped to the composable, released on dispose.

## Migration Plan

Greenfield app — no pre-existing data. The Room DB is created on first run; versioned schema for future changes. Rollback to the template state is `git checkout` of the affected files (nothing is destructive outside `filesDir`, which is app-scoped).
