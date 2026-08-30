## 1. OsmMap support

- [x] 1.1 Add an optional `onMapTapped: (GeoPoint) -> Unit = {}` parameter to `OsmMap` and wire it to osmdroid's `setOnSingleTapConfirmed`; existing callers (detail screen) keep default behavior
- [x] 1.2 Keep `interactive = false` swallowing touches so a non-interactive map never pans or drags inline

## 2. Fullscreen map picker

- [x] 2.1 Create a `MapPicker` composable in `ui/map/` as a full-screen `Dialog` (`DialogProperties(usePlatformDefaultWidth = false)`): top bar with title and cancel, full-size interactive `OsmMap` (draggable + tap-to-place), and a confirm action
- [x] 2.2 Picker state: local picked `GeoPoint` initialized from the provided position (map default center when none), marker always visible, updated on `onMarkerDrag`, `onMarkerDragEnd` and `onMapTapped`
- [x] 2.3 Confirm closes and returns the picked coordinates via `onConfirm(lat, lon)`; cancel (close button, system back, `onDismissRequest`) closes without returning

## 3. Form integration

- [x] 3.1 Replace the inline draggable `OsmMap` in `ClientFormScreen`'s "Localização" box with a non-interactive preview (`interactive = false`, `draggable = false`) showing the current coordinates
- [x] 3.2 Add a clickable overlay filling the preview box that opens `MapPicker` with the form's current position
- [x] 3.3 Wire picker confirm to `onLocationChange(lat, lon)` so the preview marker updates; cancel leaves the form state untouched

## 4. Detail screen

- [x] 4.1 Confirm `ClientDetailScreen` keeps `OsmMap(interactive = false, draggable = false)` with no picker wiring, so tapping the box does nothing

## 5. Verification

- [x] 5.1 Run `./gradlew assembleDebug lintDebug` and confirm a green build
- [x] 5.2 Run `./gradlew testDebugUnitTest` and confirm all unit tests pass
- [x] 5.3 Add an instrumented Compose test: tapping the form map preview opens the picker and cancelling returns without changing the selection
- [x] 5.4 Manual smoke-test on the emulator: the form map box is not directly interactive, tapping it opens the fullscreen picker, dragging and tapping the map move the marker, confirm returns the point to the form preview, and the detail page map box ignores taps
