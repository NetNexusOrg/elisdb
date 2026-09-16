---
name: android-development
description: Guidance for developing the ElisDB Android app (Kotlin, Compose, Room, Ktor, MVVM). Use when adding, changing, or fixing features in elisdb-android/ — screens, ViewModels, repositories, sync, or data layer.
---

# Android Development

Base facts live in `project-context`. This skill covers how to write code here.

## Stack (verified)

- Kotlin 2.4.20, Jetpack Compose + Material 3, Navigation 3 (`androidx.navigation3`).
- Room 2.8.5 (KSP), Ktor 3.6 client, kotlinx-serialization, WorkManager, CameraX,
  Coil 3, osmdroid, Play Services Location, `EncryptedSharedPreferences`.
- No Hilt/Koin: manual DI via `AppContainer`. No DataStore: SharedPreferences.

## Architecture rules

- **MVVM, unidirectional**: Screen (`ui/<feature>/*Screen.kt`) reads `StateFlow` from
  a `*ViewModel`; user actions call ViewModel methods; ViewModel mutates state and
  delegates data work to the repository. Never put business logic in Composables.
- **StateFlow over LiveData**: expose `StateFlow`, build with `stateIn(viewModelScope, WhileSubscribed(5000), initial)`.
- **Repository pattern**: `ClienteRepository` (interface in `data/`) is the only
  entry point from ViewModels; `ClienteRepositoryImpl` wires Room DAOs + sync.
- **Dependency wiring**: new dependencies are created in `AppContainer` and passed
  down from `Navigation.kt` / `rememberAppContainer()`; do not scatter singletons.
- **Packages**: UI in `ui/`, models/entities in `data/local/`, remote in `remote/`,
  sync logic in `sync/`, helpers in `util/`. Keep the `com.example.elisdb` namespace.

## Doing a feature change

1. Read `Navigation.kt` to find the screen/entry you are touching.
2. Identify the ViewModel + repository methods involved.
3. If data model changes, update the Room entity/DAO **and** the exported schema
   (run a build; schema JSON in `app/schemas/` must stay in sync), adding a `Migration`.
4. If sync protocol changes, change both the Android `remote/`+`sync/` code and the
   server (`app/main.py`, `app/models.py`) consistently — never one side only.
5. Prefer small, scoped diffs; do not refactor unrelated code.

## Sync constraints

- Offline-first: the app must keep working with no server. Sync errors surface as
  a `SyncFault`/`SyncStatus`, never as crashes.
- Signing is mandatory: requests are ed25519-signed via `util/Signer.kt`. A blank
  signing key is a config fault (`ApiClientProvider` throws `ConfigException`), not a fallback.
- Do not add implicit default server URL or key (see `app-build` spec).

## Conventions

- Follow existing Kotlin formatting (2-space indent, trailing commas, explicit `public`? no — match the file).
- Strings go in `res/values/strings.xml`; do not hardcode user-facing text in Composables.
