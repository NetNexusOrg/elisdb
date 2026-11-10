---
name: project-context
description: Authoritative reference for the ElisDB project's real stack, architecture, layout and conventions. Use when starting any task in this repo, or when another skill needs project facts (paths, versions, naming, secrets model). Read this before android-development, api-backend, database, gradle-build, or android-release.
---

# ElisDB — Project Context

Single source of truth for the repo. Other skills in `.opencode/skills/` assume
this file and must not restate it. If anything here looks stale, verify against
the code rather than trusting memory.

## Repo layout (monorepo, one Git repo, branch `main`)

- `elisdb-android/` — Android app (Kotlin + Jetpack Compose). Gradle project root.
- `elisdb-server/` — Python backend (FastAPI + SQLite). Docker via `compose.yaml`.
- `elisdb-memento/` — one-off Python migration scripts (legacy "Memento" CSV → ElisDB SQLite).
- `openspec/` — OpenSpec specs (`openspec/specs/`) and change history (`openspec/changes/`).
- `.opencode/` — OpenCode config: `commands/`, `skills/`.
- `.github/workflows/android-ci.yml` — CI/CD.

## Android app

- Package / namespace / applicationId: `com.example.elisdb`. Single module `:app`.
- Kotlin 2.4.20, Jetpack Compose (BOM 2026.09.00) + Material 3, Navigation 3 (`androidx.navigation3`).
- minSdk 24, compileSdk/targetSdk 37, Java 17 (`jvmToolchain(17)`).
- Dependency versions in `elisdb-android/gradle/libs.versions.toml` (version catalog).
- App version in `elisdb-android/gradle.properties`: `app.versionCode`, `app.versionName`.

## Android architecture

- **MVVM**: `ui/<feature>/` holds `*Screen` + `*ViewModel`. ViewModels expose
  `StateFlow` (`MutableStateFlow` / `stateIn(viewModelScope, WhileSubscribed(5000), ...)`).
- **Repository**: `data/ClienteRepository` (interface) + `data/ClienteRepositoryImpl`;
  Room DAOs and entities in `data/local/`.
- **Manual DI**: `AppContainer` (no Hilt); `ui/AppDependencies.kt` exposes `rememberAppContainer()`.
- **Networking**: Ktor client (`remote/ApiClient.kt`) + kotlinx-serialization.
- **Sync**: offline-first; `sync/SyncCoordinator` + `sync/SyncManager` (manual,
  single-flight), incremental paginated pull/push, tombstone deletes,
  clock-offset tracking.
- **Persistence**: Room (SQLite), schema JSON exported to `app/schemas/` (KSP).
- Maps: MapLibre Native · Photos: CameraX + Coil 3 · Location: Play Services.

## Server (FastAPI)

- Python 3.14, FastAPI, uvicorn, Pydantic v2, `cryptography` (ed25519), SQLite.
- One SQLite file (`ELISDB_DB`, default `elisdb.db`); photo binaries on disk (`ELISDB_PHOTOS`).
- **Auth**: ed25519 request signing — headers `X-Elisdb-Timestamp`, `X-Elisdb-Signature`,
  `X-Elisdb-Key`; per-process replay cache; ±300s clock window (`app/auth.py`).
- Endpoints in `app/main.py`: `/health`, `/api/time`, `/api/latest`, `/api/push`,
  `/api/pull`, `/api/clients`, `/api/photos/{id}`, `/api/clients/{id}`,
  `/api/clients/{id}/history`, `/api/keys/me`.
- Key provisioning: `docker compose exec elisdb-server python -m app.keys issue <user> --server-url <url>` (writes
  `<user>.config.json`); startup auto-provision via `ELISDB_USERS` (`app/provision.py`).

## Database (SQLite)

- Tables: `clientes`, `fotos`, `tombstones`, `client_audit`, `keys`. Schema string in `app/database.py`.
- Duplicate detection by normalized name (`COLLATE NOCASE`, uppercased); LWW by `updated_at`; tombstones for deletes.
- WAL mode + `busy_timeout=5000`. All timestamps are Unix epoch seconds (UTC).

## Secrets model (never commit)

- Device config `<user>.config.json` = `server_url` + `user_name` + `private_seed` (ed25519) → git-ignored (`*.config.json`). The app shows `user_name` as the associated user directly from the file; the `/api/keys/me` lookup is only a fallback for files without it.
- `*.db`, migration data `ELIS.csv` / `memento-import.db` → git-ignored.
- Android signing keystore/passwords live outside the repo (`~/.keystores/` or GitHub Secrets).

## Conventions

- Docs in Portuguese under `*/docs/`; root `README.md` is the project overview.
- Tests: Android unit tests `app/src/test/`, instrumented `app/src/androidTest/`;
  server `pytest` suite in `elisdb-server/tests/` (CI: `.github/workflows/server-ci.yml`).
- Commits: Conventional Commits (`feat:`, `fix:`, `ci:`, `docs:`, `chore:`).
- No app code lives outside `com.example.elisdb`.

## Command reference

The verified command cheat-sheet (build, test, run server, keys, git, release,
openspec) lives in `references/commands.md` — consult it instead of guessing flags.
