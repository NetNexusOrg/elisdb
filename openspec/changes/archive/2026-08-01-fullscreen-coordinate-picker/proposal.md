## Why

The coordinate selector embeds a small, directly draggable map inside the client form, which is awkward to operate in a 220dp box and easy to mis-tap. Selecting a precise point deserves a fullscreen map. The map box on the client detail page is also currently a static display, and it must stay that way (not open any picker).

## What Changes

- The client form's map box becomes a non-interactive preview of the currently selected coordinates that, when tapped, opens a fullscreen interactive map.
- A new fullscreen map picker lets the user choose a coordinate point by panning and dragging the marker (and tapping to place the marker), then confirm or cancel.
- Confirming the picker returns the chosen coordinates to the form; cancelling leaves the form's selection unchanged.
- The map box on the client detail page stays non-interactive and display-only: tapping it does NOT open the picker.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `client-location`: the "Select a location" requirement changes from dragging a marker inline on the form to choosing a point in a fullscreen picker opened from the form's map preview; the detail page map remains non-interactive.

## Impact

- `app/src/main/java/com/example/elisdb/ui/form/ClientFormScreen.kt`: replace the inline draggable `OsmMap` with a tappable, non-interactive preview plus a fullscreen picker.
- `app/src/main/java/com/example/elisdb/ui/map/OsmMap.kt`: unchanged public API; may need support for a tap-to-place marker and a clickable-but-non-interactive mode.
- New fullscreen picker composable in `ui/map/` (e.g. `MapPicker.kt`).
- No dependency changes (osmdroid already in use).
