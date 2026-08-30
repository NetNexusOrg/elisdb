## 1. Map Zoom Controls

- [x] 1.1 Add `showZoomControls: Boolean = false` parameter to `OsmMap` and call `setBuiltInZoomControls(showZoomControls)` in the `MapView` factory
- [x] 1.2 Confirm no `+` / `-` zoom buttons render on the picker, detail, and form-preview maps

## 2. Confirm Button Styling

- [x] 2.1 In `MapPicker.kt`, replace the `floatingActionButton` with a full-width bottom `Button` (check icon + "Guardar localização") inside a `Column` with the map taking `weight(1f)`

## 3. Build and Verification

- [x] 3.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm all pass
- [x] 3.2 Run the `MapPickerFormTest` instrumented test (open picker from preview, cancel) and confirm it passes
- [x] 3.3 Emulator smoke test: open the picker and confirm there are no `+` / `-` buttons and that "Guardar localização" is a full-width button at the bottom; confirm confirm/cancel still work
