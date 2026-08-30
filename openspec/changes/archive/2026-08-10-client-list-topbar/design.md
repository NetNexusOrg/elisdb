## Context

The client list screen (`ClientListScreen.kt`) currently has no app bar: the create-client action is a `FloatingActionButton` and the settings action is an `IconButton` inside the search field's trailing row. Other screens (detail, form, settings, map picker) all use a Material 3 `TopAppBar` inside a `Scaffold`. See `proposal.md` - Why for motivation.

Navigation wiring (`Navigation.kt`) already passes `onAddClick` and `onSettingsClick` to the screen; only the placement of those actions changes. The screen's `internal ClientListScreen` overload is stateless and directly testable; its `viewModel` overload delegates to it.

## Goals / Non-Goals

- Goals: give the list screen a `TopAppBar` with a title; host the add-client and settings actions in the top bar; remove the `FloatingActionButton`.
- Non-Goals: changing navigation arguments, changing screen titles/content, moving the search field, changing the sync status bar (out of scope, tracked by the `move-sync-to-settings` change), or introducing app-wide theming changes.

## Decisions

### 1. Use the existing `TopAppBar` pattern from sibling screens
- **Chosen**: A Material 3 `TopAppBar` passed as the `Scaffold.topBar` slot, matching `SettingsScreen.kt` / `ClientFormScreen.kt`.
- **Why**: zero new dependencies; consistent look with the rest of the app.
- **Alternative considered**: `CenterAlignedTopAppBar` — rejected, other screens use the default left-aligned `TopAppBar`.

### 2. Title "Clientes" in the top bar
- **Chosen**: `title = { Text("Clientes") }`.
- **Why**: the app's Portuguese UI labels the feature "clientes"; the top bar gives the screen an identity, which was previously missing.
- **Alternative considered**: empty title — rejected, a title is the point of adding a top bar and matches the proposal's "title" requirement.

### 3. Keep the add action as an `IconButton` in `actions`
- **Chosen**: `IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, contentDescription = "Adicionar cliente") }`.
- **Why**: same icon and content description as today, so existing UI tests (`onNodeWithContentDescription("Adicionar cliente")`) keep passing unchanged; the action moves from `floatingActionButton` slot to `actions`.
- **Alternative considered**: FAB-style extended button in the top bar — rejected, non-standard and inconsistent with sibling screens.

### 4. Move the settings action from the search field into `actions`
- **Chosen**: `IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Definições") }` in the top bar's `actions`.
- **Why**: settings is a global screen-level action, not a search-scoped one; placing it in the top bar declutters the search field and matches Material guidance.
- **Impact**: the search field's `trailingIcon` Row loses the settings `IconButton`, leaving only the clear button (shown when `state.query.isNotEmpty()`).

### 5. Action order in the top bar
- **Chosen**: settings action first, then add action, per the screen's existing visual order (search field had settings; FAB add was bottom-right).
- **Why**: no strong convention in the app; preserves the closest mapping to today's layout.
- **Alternative considered**: add first — minor, not spec-relevant.

## Risks / Trade-offs

- **Existing tests locate actions by content description** → Content descriptions `"Adicionar cliente"` and `"Definições"` are preserved verbatim, so `ClientListScreenTest` and `NewClientFormNotPrefilledTest` continue to pass; tests that assert geometry/placement of the FAB (if any) must be updated to the top bar. No geometry assertions exist today.
- **Top bar reduces vertical space for the search field and list** → standard `Scaffold` top bar padding is handled by `innerPadding`; the list is lazy so no functional impact.
- **The sync status bar (`move-sync-to-settings`) still lives on this screen** → this change only touches the top bar / FAB / search trailing icon; it does not interact with the sync bar. Both changes are independent.

## Migration Plan

No data migration. This is a pure UI relocation within one file. Rollback: revert the single-file edit; no state or schema impact.

## Open Questions

None.
