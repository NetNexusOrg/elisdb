## 1. Project setup

- [x] 1.1 Add Room, CameraX, OSMDroid, Google Play Services Location and Coil dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`, with the KSP plugin (KSP version compatible with Kotlin 2.3.20)
- [x] 1.2 Declare `CAMERA`, `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions in `app/src/main/AndroidManifest.xml`
- [x] 1.3 Create an `Application` subclass (`ElisdbApp`) that sets the OSMDroid user agent (`osmdroid.config.Configuration`) and builds an `AppContainer`; register it in the manifest
- [x] 1.4 Verify the project builds: `./gradlew assembleDebug`

## 2. Data layer (Room schema, DAOs, repository)

- [x] 2.1 Create the `Cliente` Room entity (`id` autogen PK, `nome`, `telefone?`, `latitude`, `longitude`, `notas?`, `dataCriacao`)
- [x] 2.2 Create the `Foto` Room entity (`id` autogen PK, `clienteId` FK to `Cliente`, `uriLocal`) with a 1-N relation from `Cliente`
- [x] 2.3 Create `ClienteDao`: observe-all flow, client-by-id (with photos), insert/update, delete
- [x] 2.4 Create `FotoDao`: observe-by-client flow, insert, delete-by-ids
- [x] 2.5 Create `AppDatabase` (Room, versioned, singleton via `AppContainer`)
- [x] 2.6 Create `ClientRepository` implementing CRUD plus photo-file management (copy captured/picked photos into `filesDir/photos/`, delete files when a photo or its client is removed)
- [x] 2.7 Remove the placeholder `DataRepository` and wire the container into the app

## 3. Client list screen

- [x] 3.1 Replace the placeholder `MainScreen` with a `ClientListScreen`: `LazyColumn` showing each client's name and, when present, a thumbnail of the first photo (Coil), empty-state message and a FAB to add
- [x] 3.2 Create `ClientListViewModel` exposing the clients-with-first-photo flow from the repository
- [x] 3.3 Define the `ClientList` `NavKey` and register the route in `MainNavigation`
- [x] 3.4 Update the existing `MainScreenViewModelTest` / `MainScreenTest` to cover the new list screen

## 4. Create/edit client form screen

- [x] 4.1 Create `ClientFormViewModel`: holds name, phone, notes, selected coordinates and photo list state; validates that name is non-empty; loads an existing client when in edit mode; persists via the repository
- [x] 4.2 Build the form layout: name (required), phone and notes fields, and a save action with validation feedback
- [x] 4.3 Add the location section: OSMDroid map with a draggable marker, plus a "use current location" action (runtime location permission via activity-result API, `FusedLocationProviderClient`, centering the map and moving the marker; graceful message when location is unavailable)
- [x] 4.4 Add the photo section: "take photo" via CameraX `ImageCapture` (camera permission) and "choose from gallery" via `ActivityResultContracts.PickVisualMedia`, showing a grid of thumbnails with remove buttons
- [x] 4.5 Wire form save/back navigation, route to the list after save, and route the camera/gallery composables with proper lifecycle handling

## 5. Client detail screen

- [x] 5.1 Create `ClientDetailViewModel` loading a client and its photos by id
- [x] 5.2 Build `ClientDetailScreen`: name, phone, notes, a static mini-map centered on the client's coordinates, and a photo gallery grid
- [x] 5.3 Add the "navigate here" action launching `Intent.ACTION_VIEW` with a `geo:` URI, resolving the intent first and showing a friendly message when no navigation app exists
- [x] 5.4 Add edit and delete actions (with delete confirmation), returning to the list after delete and to the form on edit
- [x] 5.5 Add a `PhotoViewer` fullscreen screen and its route (opens on thumbnail tap)

## 6. Navigation wiring and cleanup

- [x] 6.1 Update `MainNavigation` and `NavigationKeys` with all routes (`ClientList`, `ClientForm(clientId?)`, `ClientDetail(clientId)`, `PhotoViewer(clientId, index)`) and their callbacks
- [x] 6.2 Remove leftover placeholder code and verify the manifest/`Application` wiring
- [x] 6.3 Run the full build and test suite (`./gradlew build`) and smoke-test the flow on an emulator: add client, edit, photos, detail, navigate — build+lint+unit tests+2 instrumented tests pass; emulator smoke-test done (add/edit/delete with location, external navigation to Maps, photo picker→grid→save→detail→viewer, back navigation). Two real bugs found and fixed during smoke-test: (1) `<queries>` missing for `geo` intent so `resolveActivity` returned null on Android 11+ and external navigation wrongly fell back to a snackbar; (2) `LazyVerticalGrid` nested inside `Column(verticalScroll)` crashed with "infinity maximum height constraints" when adding/displaying photos — replaced with chunked `Column`/`Row` grid in both the form and detail screens.
