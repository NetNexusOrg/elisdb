# Tasks

## 1. Server

- [x] 1.1 Add `app/updates.py`: filename parsing, `find_release()`, SHA-256 cache by file identity, 200 MB limit, `apk_dir()` from `ELISDB_APK_DIR` (default `apk/latest` next to the database).
- [x] 1.2 Add `AppReleaseOut` to `models.py`.
- [x] 1.3 Add authenticated `GET /api/app/latest` (404 `no release available`) and `GET /api/app/download` (APK media type, no path parameter).
- [x] 1.4 Create and chown the release directory in `docker-entrypoint.sh`; declare `ELISDB_APK_DIR` in `compose.yaml`.
- [x] 1.5 Server tests: parsing, newest by version code, malformed names, empty directory, digest/size, metadata endpoint, download bytes, 401 without signature.

## 2. CI

- [x] 2.1 Rename the release APK to `elisdb-<versionName>-<versionCode>.apk` (values from `gradle.properties`) before upload and GitHub release.

## 3. App

- [x] 3.1 Add `AppReleaseDto` and `ApiClient.fetchLatestRelease` / `downloadRelease` (stream to `cacheDir`, SHA-256 + size check, progress callback).
- [x] 3.2 Add `UpdateViewModel` with phases Idle/Checking/UpToDate/NoRelease/Available/Downloading/Ready/Failed and NotConfigured.
- [x] 3.3 Add the "Atualizações" card to `AboutScreen` with "Procurar atualizações", "Transferir" (with progress) and "Instalar".
- [x] 3.4 Add `ApkInstaller` + `FileProvider` (`res/xml/file_paths.xml`), `REQUEST_INSTALL_PACKAGES`, and the install-permission prompt.
- [x] 3.5 Wire the ViewModel in `Navigation` with the container's client and cache directory.

## 4. Verification

- [x] 4.1 Unit tests for the ViewModel (newer, same, no release, failure, progress, checksum mismatch, not configured).
- [x] 4.2 Instrumented test for the About card states.
- [x] 4.3 `pytest`, `:app:testDebugUnitTest`, `:app:lintDebug` and the instrumented tests green.
- [x] 4.4 End-to-end on the emulator against a local server: publish an APK with a higher `versionCode`, check, download, verify, open the installer.
- [x] 4.5 `openspec validate app-update --strict`.

## 5. Docs

- [x] 5.1 `elisdb-android/docs/uso.md`: how to check for and install an update.
- [x] 5.2 `elisdb-server/docs/execucao.md`: how to publish a release file.
