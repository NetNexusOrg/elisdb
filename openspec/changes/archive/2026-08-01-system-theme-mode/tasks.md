## 1. Window theme

- [x] 1.1 Add `app/src/main/res/values-night/themes.xml` with `Theme.Elisdb` parented to `android:Theme.Material.NoActionBar`, keeping `values/themes.xml` (light) unchanged
- [x] 1.2 Verify `./gradlew assembleDebug` compiles and lint passes

## 2. Testable color scheme selection

- [x] 2.1 Extract `resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean, dynamicScheme: ((Boolean) -> ColorScheme)?): ColorScheme` in `theme/Theme.kt`, and make `ElisdbTheme` call it (falling back to the static dark/light schemes when the dynamic provider is null or `dynamicColor` is false)
- [x] 2.2 Add `ThemeTest` (JVM unit test) covering: dark→`DarkColorScheme`, light→`LightColorScheme`, dynamic provider invoked with the correct flag when `dynamicColor=true`, and fallback to the static scheme when the provider is null
- [x] 2.3 Run `./gradlew testDebugUnitTest` and confirm the theme tests pass

## 3. Automated dark/light verification

- [x] 3.1 Add an instrumented Compose test (`ThemeModeTest`) that launches `MainActivity`, switches the device night mode (`UiModeManager.setApplicationNightMode(MODE_NIGHT_YES/NO)`), and asserts the in-use `MaterialTheme.colorScheme` matches the expected mode (e.g. `background` luminance); restore the original mode in teardown
- [x] 3.2 Run `./gradlew connectedDebugAndroidTest` on the emulator and confirm the theme test passes

## 4. Manual smoke-test

- [x] 4.1 Smoke-test on the emulator in light and dark mode: launch with no light flash in dark mode, verify list, form, detail, dialogs and photo viewer render correctly, and system bar icons adapt; capture a screenshot per mode
- [x] 4.2 Update this tasks file to record the smoke-test result

### Smoke-test result (2026-08-01, emulator-5554, API 36)

Dark mode (`adb shell cmd uimode night yes`):
- Launch: window background `#303030` (`Theme.Material.NoActionBar` night parent) with the splash icon — no light flash; final Compose background `(18,19,24)`.
- List `(18,19,24)`, detail `(18,19,24)`, form chrome `(18,19,24)` (map tiles render their own light tiles — intentional exception), delete dialog `(41,42,47)`.
- Photo viewer: black `(0,0,0)` background + image (intentional dark exception).
- System bar icons: light (white `255,255,255`) on the dark background.

Light mode (`adb shell cmd uimode night no`):
- Launch: white window background; final Compose background `(250,248,255)`.
- List, detail, delete dialog `(232,231,239)` and form all light.
- Photo viewer: still black `(0,0,0)` (intentional exception).
- System bar icons: dark `(100,99,102)` on the light background.

Also re-verified end-to-end in dark mode: create client "Ana Silva" with mock GPS location, edit → add photo via Photo Picker → save (row in `fotos`, file under `files/photos/`), detail shows Fotos grid, photo viewer opens.

Screenshots: `/tmp/opencode/dark_list.png`, `dark_form.png`, `dark_detail.png`, `dark_dialog.png`, `dark_photoviewer.png`, `light_list.png`, `light_detail.png`, `light_dialog.png`, `light_form.png`, `light_photoviewer.png`.
