# Design

## Context

The Android app is signed and distributed outside Play. The server already authenticates every data request with ed25519 signatures and stores its state in a Docker volume (`elisdb-data:/app/data`). The operator drops the APK into a directory of that volume; the app must find it, verify it and hand it to the system installer.

## Decisions

### Filesystem as the source of truth

There is no database table for releases: the presence of a file named `elisdb-<versionName>-<versionCode>.apk` in the release directory **is** the release. Copying a new file publishes it, deleting it withdraws it, and there is no state to keep in sync.

The name carries the version, so the server never parses the APK manifest. Files that do not match the pattern are ignored, and the highest `versionCode` wins when several are present — the operator can stage the next build without deleting the current one.

### Authenticated metadata and download

Both endpoints require the usual request signature. Updates are not secret, but an authenticated endpoint keeps the server's surface uniform (same auth dependency, same rate limiter) and avoids publishing build metadata to anonymous scanners. The download has **no filename parameter**: it always serves the file that `find_release()` discovered, so a crafted request cannot read arbitrary paths.

### SHA-256 computed on demand, cached by file identity

Hashing a ~15 MB file per metadata request is wasteful, so the digest is cached under `(path, mtime_ns, size)`. Replacing the file changes the key and the digest is recomputed; nothing has to be invalidated by hand. Files above 200 MB are refused, which bounds memory and time.

### Explicit user action, not a background check

The app never checks for updates on its own. The About screen is the only entry point, and the flow is visible: check → download with progress → verify → install. On a field device, an unexpected download would burn mobile data.

### Verification before installation

The download goes to `cacheDir/updates/`, then the SHA-256 and size are compared with the metadata. A mismatch deletes the file and reports a distinct error — a truncated download must never reach the installer.

### System installer, not silent install

Installing without user interaction requires `DevicePolicyManager` (device owner) or a system signature. The app instead declares `REQUEST_INSTALL_PACKAGES`, shares the file through a `FileProvider` and launches `ACTION_VIEW` with the APK media type. If the "install unknown apps" permission is missing, the app opens that settings page instead of failing silently.

## Risks / trade-offs

- **Signature mismatch** (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`): the release APK and the installed one must use the same signing key. A device with a debug build installed cannot update to a release build; the installer reports the failure.
- **Forgotten `versionCode`**: the update is invisible even after publishing. `gradle.properties` is the single place to bump.
- **Old server**: `/api/app/latest` returns 404, which the app shows as "sem atualizações disponíveis", not as a network error.
- **No resume**: `FileResponse` does not support `Range`, so an interrupted download restarts. Acceptable for a file this size on a local network.
- **Downgrade by mistake**: publishing a lower `versionCode` is ignored by clients (they compare), but the served release is still the highest file present — the operator must remove the wrong file.

## Alternatives considered

- **Check GitHub Releases directly**: requires internet access to GitHub from the device and couples the app to the release host; the server is the only endpoint the app already trusts.
- **Play Store / internal app sharing**: not available for this distribution model.
- **Store release metadata in SQLite**: adds a schema and provisioning step for data that the filesystem already expresses.
