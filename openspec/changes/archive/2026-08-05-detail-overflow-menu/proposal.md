## Why

The detail screen top bar is cluttered and redundant: it shows the client's name (already displayed in the content) plus two raw action icons (pencil and trash). Grouping the actions under a dropdown menu cleans the top bar and frees the title area, matching Material 3 conventions.

## What Changes

- Replace the two action icons (edit/delete) in the detail screen top bar with a single overflow menu ("⋮") that opens a dropdown with "Editar" and "Apagar" items.
- Remove the client name from the top bar title, since the name is already shown in the screen content.
- The back navigation arrow remains unchanged.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `client-screens`: the client detail screen requirement changes — edit and delete actions are offered through an overflow menu instead of directly visible icons, and the top bar no longer repeats the client name.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailScreen.kt` — top bar actions become an overflow `DropdownMenu`; the title stops showing the client name.
- No data, dependency or navigation changes. No test currently references the removed icons.
