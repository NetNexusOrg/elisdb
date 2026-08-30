## Context

See proposal.md - Why. The osmdroid `MapView` enables its built-in zoom controls (`+` / `-`) by default, so they render on every map; the picker also places "Guardar localização" in a `floatingActionButton`, unlike the app's other full-width filled buttons.

## Goals / Non-Goals

**Goals**

- Hide the `+` / `-` zoom buttons on the map picker.
- Style "Guardar localização" as a full-width filled button at the bottom of the picker, matching the look of buttons like "Guardar" and "Usar localização atual".
- Keep the picker's spec'd behavior unchanged (open, drag/tap to place, confirm, cancel).

**Non-Goals**

- Not changing the marker, map panning, or coordinate logic.
- Not adding/removing other controls or changing other screens' behavior.

## Decisions

**1. Disable built-in zoom controls in `OsmMap`, with a parameter.**

- osmdroid's `MapView` shows `+` / `-` unless `setBuiltInZoomControls(false)` is called. Add `showZoomControls: Boolean = false` to `OsmMap` and call `setBuiltInZoomControls(showZoomControls)` in the factory. Default `false` hides the buttons everywhere — the picker is the only interactive map, and the detail/preview maps are non-interactive, so zoom buttons are inert there too.
- Rationale: one consistent behavior with an escape hatch; alternatives considered: toggling only in the picker call site (more code, still needs the parameter) and hard-coding `false` (no flexibility if a screen later needs zoom buttons).

**2. Confirm button becomes a full-width bottom button instead of a FAB.**

- Replace the `Scaffold` `floatingActionButton` with a bottom-aligned full-width `Button` (check icon + `Spacer(8.dp)` + "Guardar localização"), matching the icon+label pattern used by "Guardar", "Usar localização atual", "Foto"/"Galeria".
- Layout: `Column(fillMaxSize)` with `OsmMap` taking `weight(1f)` and the button below it padded 16.dp, so the button spans the picker width at the bottom ("ocupar na vertical" read as full-width, consistent with the rest of the UI).
- Rationale: consistent with the app's other primary actions; a FAB reads as a secondary/floating action. Alternatives considered: a `bottomBar` slot (also fine, but the `Column` keeps the button visually attached to the map without an extra bar surface).

## Risks / Trade-offs

- [Some users rely on the zoom buttons in the picker] → The map supports pinch-to-zoom via `setMultiTouchControls(true)` on the interactive picker, so zooming remains available; the detail/preview maps were non-interactive anyway.
- [The picker's instrumented test relies on the confirm button's position] → `MapPickerFormTest` only interacts with the title, "Cancelar", and `map_preview`, so the layout change is safe; the test is re-run to confirm.

## Migration Plan

- Single change across `MapPicker.kt` and `OsmMap.kt`; no data/schema changes.
- Rollback: revert the two files; behavior returns to the pre-change state.

## Open Questions

None.
