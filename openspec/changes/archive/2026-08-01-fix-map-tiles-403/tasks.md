## 1. User-Agent Configuration

- [x] 1.1 Update `ElisdbApp.onCreate` to set a descriptive, policy-compliant User-Agent (e.g. `ElisDB/<versionName> (Android)`) using `PackageManager.getPackageInfo(packageName, 0).versionName`, replacing the bare package-name assignment
- [x] 1.2 Confirm the User-Agent is assigned before any `MapView` is created (no map initialized before `Application.onCreate`)

## 2. Build and Unit Verification

- [x] 2.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm all pass

## 3. Emulator Smoke Test

- [x] 3.1 Boot the emulator, install the debug build, and confirm map tiles render (no `403 Access blocked`) on the client detail screen, the fullscreen coordinate picker, and the form map preview
- [x] 3.2 Confirm `cache.db` grows with tile bytes on the device (same signal as the `fix-blank-map` change)
