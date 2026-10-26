# Proposal

## Why

The app is distributed as a signed APK and installed by hand: someone downloads the artefact from GitHub, copies it to the server, and each device has to be updated by transferring the file manually. Nothing in the app tells the user that a newer build exists, so devices run old versions for weeks — including versions with bugs in synchronization that are already fixed.

The About screen is the natural place to answer "estou atualizado?" but it only shows the installed version and the developer contact.

## What Changes

- The server publishes the current APK from a directory inside its data volume (`/app/data/apk/latest/`), following the name convention `elisdb-<versionName>-<versionCode>.apk`.
- Two authenticated endpoints: `GET /api/app/latest` reports the available version (name, code, size, SHA-256, filename) and `GET /api/app/download` serves the file. No filename parameter, so the served path can only come from the discovered release.
- The About screen gains an "Atualizações" card with a **"Procurar atualizações"** action. When a newer `versionCode` exists it shows the version and size, downloads with progress, verifies the SHA-256 and offers **"Instalar"**, which opens the system package installer (one confirmation from the user — silent install would require device owner/MDM).
- The release workflow publishes the artefact with the versioned name, so the file copied to the server needs no renaming.

## Capabilities

### New Capabilities

- `client-update`: checking for, downloading and installing a newer build from inside the app.
- `server-app-updates`: publishing the current APK and serving its metadata and bytes over an authenticated endpoint.

### Modified Capabilities

<!-- none -->

## Impact

- **Server**: new `app/updates.py` (release discovery, SHA-256 cache), `AppReleaseOut` model, two routes in `main.py`, `ELISDB_APK_DIR` in `compose.yaml` and `docker-entrypoint.sh`.
- **Android**: `AppReleaseDto`, `ApiClient.fetchLatestRelease` / `downloadRelease`, new `UpdateViewModel`, `ApkInstaller` with a `FileProvider`, `REQUEST_INSTALL_PACKAGES` permission, `res/xml/file_paths.xml`, an "Atualizações" card in `AboutScreen`, wiring in `Navigation`.
- **CI**: `android-release.yml` renames the APK to `elisdb-<versionName>-<versionCode>.apk` before uploading and attaching it to the release.
- **Docs**: `docs/uso.md` (how to update) and `elisdb-server/docs/execucao.md` (how to publish a version).
- **Not affected**: record format, sync protocol, authentication scheme.
