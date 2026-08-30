## Why

The fullscreen coordinate picker shows the osmdroid built-in `+` / `-` zoom buttons, which clutter the map, and its "Guardar localização" button floats as a small FAB, inconsistent with the rest of the app's UI (full-width filled buttons with icon + label, e.g. "Usar localização atual", "Guardar", "Foto"). This change removes the zoom buttons and makes the confirm button match the app's button style.

## What Changes

- Hide the osmdroid built-in zoom controls (`+` / `-`) on the map picker so the map surface is clean. The zoom controls are off by default in `OsmMap` (also removing them from the non-interactive detail and form-preview maps, where they are inert).
- Replace the picker's floating action button with a full-width filled button anchored at the bottom of the picker, styled like the other UI buttons (check icon + "Guardar localização" label).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Pure UI polish of the `client-location` coordinate picker: every spec'd behavior (open picker, drag/tap to place marker, confirm, cancel, preview non-interactive) is preserved. The change opts out of specs via `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/map/MapPicker.kt` — confirm button moved from FAB to a full-width bottom button.
- `app/src/main/java/com/example/elisdb/ui/map/OsmMap.kt` — built-in zoom controls disabled (new parameter, default off).
- `app/src/androidTest/java/com/example/elisdb/ui/form/MapPickerFormTest.kt` — unchanged; verify it still passes.
- Behavior: zoom buttons no longer appear on any map; picker confirm button spans the picker width at the bottom.
