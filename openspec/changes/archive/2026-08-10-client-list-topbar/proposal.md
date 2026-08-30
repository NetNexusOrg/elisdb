## Why

The client list screen has no app bar: the "create client" action floats over the list (FAB) and the settings action sits inside the search field's trailing icon, which is easy to miss and inconsistent with the other screens (detail, form, settings all use a `TopAppBar`). A top bar gives the list a consistent header and moves both primary actions to a predictable place.

## What Changes

- **Add a top app bar to the client list screen**, showing a title for the screen.
- **Move the "create client" action ("+") from the floating action button into the top bar** as an icon action. The FAB SHALL be removed.
- **Move the settings action from the search field's trailing icon into the top bar** as an icon action.
- The search field stays as the list's filter control and SHALL no longer host the settings action.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the client list screen gains a top app bar that hosts the add-new-client and settings actions; the floating action button is removed.

## Impact

- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt` — add a `TopAppBar` (title + add/settings `IconButton`s) to the `Scaffold`; remove the `floatingActionButton` and the settings action from the search field's trailing row.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreenTest.kt` — update UI tests that reference the FAB ("Adicionar cliente" content description) and the settings action location.
- Existing `onAddClick`/`onSettingsClick` callbacks in `Navigation.kt` stay unchanged (same actions, new placement).