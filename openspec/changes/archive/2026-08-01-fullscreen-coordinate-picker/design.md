## Context

See `proposal.md` for motivation. Current state that shapes the approach:

- The create/edit form (`ClientFormScreen.kt`) embeds a directly draggable `OsmMap` (`draggable = true`) in a 220dp box; dragging a marker inline on the small map is the only way to set coordinates.
- The client detail screen already renders a static map (`OsmMap(interactive = false, draggable = false)`).
- `OsmMap` (`ui/map/OsmMap.kt`) has three relevant knobs: `interactive`, `draggable`, `onMarkerMoved`. When `interactive = false` it installs `setOnTouchListener { _, _ -> true }`, which swallows all touches.
- The form's coordinates live in `ClientFormUiState` and are written through `onLocationChange(lat, lon)`.

## Goals / Non-Goals

**Goals:**
- Selecting a point happens on a fullscreen interactive map opened from the form's map box.
- The form's map box becomes a non-interactive preview that opens the picker on tap.
- The detail page map remains non-interactive and never opens a picker.

**Non-Goals:**
- No new navigation routes in the Nav3 graph; the picker is a transient overlay on the form screen.
- No change to the "use current location" action or to coordinate persistence.
- No change to the mapping library (stays osmdroid, no API key).

## Decisions

**1. Fullscreen picker as a full-screen `Dialog`, not a Nav3 route.**
The picker must hand a chosen point back to the form. A Nav3 route would require a result channel or shared state between two `viewModel()` scopes. A full-screen `Dialog` (`DialogProperties(usePlatformDefaultWidth = false)`) renders above the form and returns the coordinates through a simple callback, keeping all state in the form screen. Alternative considered: a dedicated `MapPicker` route — rejected for the added result-passing plumbing it needs.

**2. Form preview: non-interactive map + clickable overlay.**
Because a non-interactive `OsmMap` swallows touches, a `Modifier.clickable` on the wrapping `Box` would never fire. Instead the preview is a `Box` that contains the `OsmMap(interactive = false, draggable = false)` plus a transparent `Modifier.matchParentSize().clickable` overlay that opens the picker. Alternative considered: giving `OsmMap` an `onClick` parameter and not swallowing touches when set — rejected to keep `OsmMap`'s API and the detail screen untouched.

**3. Tap-to-place in the picker.**
osmdroid's `setOnSingleTapConfirmed` distinguishes a tap from a pan, so adding it does not interfere with map movement. `OsmMap` gains an optional `onMapTapped: (GeoPoint) -> Unit = {}` that moves the marker to the tapped point; the picker wires it to update the selected coordinates. Alternative considered: drag-only — rejected because tapping is a faster, less error-prone way to choose a point.

**4. Picker confirm/cancel semantics.**
The picker keeps its picked point in local `remember` state, initialized from the form's current coordinates (or the map default when none selected — a marker is always present in the picker). **Confirm** calls `onLocationChange(picked.lat, picked.lon)` and closes; **Cancel** (top-bar close or system back) just closes. No ViewModel changes needed.

**5. Detail screen unchanged.**
It already renders `OsmMap(interactive = false)`; nothing new is wired, which satisfies "the box on the detail page is not interactive".

## Risks / Trade-offs

- [System back inside the dialog applying an accidental change] → `Dialog.onDismissRequest` is treated as cancel (no callback); only the explicit confirm button applies the selection.
- [Marker drag updates only on drag end, so confirm may read a stale position] → in the picker, update the local picked point on `onMarkerDrag` as well as `onMarkerDragEnd`.
- [Picked point lost on configuration change] → local `remember` state resets to the form's coordinates; acceptable for a transient picker (a lost unconfirmed pick). Not mitigated.
- [Overlay click target size on the form preview] → the overlay fills the map box, so the whole box is tappable; no small-hit-target risk.
