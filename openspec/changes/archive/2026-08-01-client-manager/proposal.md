## Why

The `elisdb` app is a bare Android template with no domain functionality. We need a simple client-management app: store each client's name, location (coordinates), notes and photos, and let the user navigate to a client's location from a detail screen. This establishes the first vertical slice of real domain behavior.

## What Changes

- Add Room persistence with `Cliente` (client) and `Foto` (photo) entities, DAOs and a repository.
- Add an MVVM layer (ViewModel per screen) and a `ClientRepository` facade.
- Add a client list screen (LazyColumn with name + first-photo thumbnail, FAB to add).
- Add a create/edit client form screen: name, phone, notes, location picker on a map with draggable pin and "use current location" via GPS, plus photo section (capture with camera, pick from gallery, remove).
- Add a client detail screen: name, phone, notes, mini-map with the location, photo gallery (tap for fullscreen), "Navigate here" button opening an external navigation app via `Intent.ACTION_VIEW` with a `geo:` URI.
- Add photo persistence: photos stored in app-private storage (`filesDir`), only the file path stored in the Room DB.
- Add runtime permissions: `CAMERA` and location permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`).
- Add dependencies: Room, CameraX, OSMDroid or MapLibre, Google Play Services Location (FusedLocationProviderClient), Navigation Compose (already present: Navigation 3).
- Update the project's dependency catalog (`gradle/libs.versions.toml`) and `app/build.gradle.kts`.

## Capabilities

### New Capabilities
- `client-data`: Room schema for Cliente and Foto (1-N), DAOs, and a repository exposing CRUD operations.
- `client-location`: map display and interaction (OSMDroid or MapLibre) with draggable pin, "use current location" via GPS, and launching an external navigation app with a `geo:` intent.
- `client-photos`: capturing photos with the camera, picking them from the gallery (Photo Picker), storing them in app-private storage, and removing them.
- `client-screens`: Compose screens (list, create/edit form, detail, fullscreen photo) and navigation between them.

### Modified Capabilities
<!-- No existing capabilities. `openspec/specs/` is empty. -->

## Impact

- **Code**: `app/src/main/java/com/example/elisdb/` gains `data/` (entities, DAOs, DB, repository) and new `ui/` packages (client list, form, detail). Existing `MainActivity`, `Navigation.kt` and `NavigationKeys.kt` are replaced/restructured to host the new screens.
- **Gradle**: `gradle/libs.versions.toml` gains Room, CameraX, OSMDroid/MapLibre, Play Services Location, Coil (image loading). `app/build.gradle.kts` adds KSP plugin for Room and the new dependencies.
- **Manifest**: `AndroidManifest.xml` gains `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` permissions. No `FileProvider` needed: CameraX captures directly into app-private storage and the Photo Picker returns read-only URIs that are copied into app storage.
- **Build**: KSP plugin required for Room; requires KSP version compatible with Kotlin 2.3.20.
- **Tests**: Existing `MainScreenTest` / `MainScreenViewModelTest` will be updated to reflect the new list screen.
