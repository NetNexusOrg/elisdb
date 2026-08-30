## Context

See proposal.md - Why for the bug. Root cause is confirmed, not hypothesized:

- Navigation3 `1.0.1` `NavDisplay` never provides a per-entry `LocalViewModelStoreOwner` (verified in `navigation3-ui`/`navigation3-runtime` bytecode: no reference to `LocalViewModelStoreOwner` anywhere).
- Therefore every `viewModel { }` call inside `entry<...>` resolves `LocalViewModelStoreOwner` to the enclosing `ComponentActivity`, so `ClientFormViewModel`, `ClientDetailViewModel`, `PhotoViewerViewModel` and `ClientListViewModel` are all Activity-scoped and shared for the app's lifetime.
- When the user leaves the form and later opens a new `ClientForm`, the *same* `ClientFormViewModel` instance is returned (its `_uiState` still holds the previous client's data), so create mode shows stale values. Reproduced on emulator-5554: after creating "Ana"/"111", a fresh FAB tap rendered the form with Nome="Ana", Telefone="111" while the title correctly said "Novo cliente".

The same defect silently affects detail, photo viewer, and edit mode (a reused VM runs `init`/`loadCliente` only once with the first client id).

## Goals / Non-Goals

**Goals**
- Give every back-stack entry its own `ViewModelStore` so each navigation creates fresh, correctly-loaded ViewModels.
- Guarantee the create form is empty and the edit form loads only the selected client, for every sequence of navigations.
- Add a deterministic regression test.

**Non-Goals**
- Re-architect the navigation setup or the ViewModels themselves.
- Change `client-screens` behavior beyond the form-state guarantee (no new fields, screens, or UI changes).

## Decisions

### D1: Register `ViewModelStoreNavEntryDecorator` in `NavDisplay`

Wire the official lifecycle decorator (already a dependency: `androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0`) into `MainNavigation`:

```kotlin
val viewModelStoreOwner = LocalViewModelStoreOwner.current
val entryDecorator = rememberViewModelStoreNavEntryDecorator(
  viewModelStoreOwner = viewModelStoreOwner,
  removeViewModelStoreOnPop = { true },
)
NavDisplay(
  backStack = backStack,
  decorators = listOf(entryDecorator),
  onBack = { backStack.removeLastOrNull() },
  entryProvider = entryProvider { /* unchanged */ },
)
```

The decorator wraps each entry's content in `CompositionLocalProvider(LocalViewModelStoreOwner provides <per-entry child owner>)`, so the existing `viewModel { }` calls automatically bind to the entry and no `Navigation.kt` VM wiring changes are needed.

Rationale: this is the intended Navigation3 scoping mechanism. Alternatives considered:
- **Keying the VM per route instance** (`viewModel(key = ...)`) — keeps VMs in the shared Activity store, never clears them (leaks per navigation) and fixes only the form, not the sibling detail/photo viewer defects.
- **Manually resetting `_uiState` in `ClientFormViewModel`** — band-aid that treats the symptom, does not fix stale detail/photo viewer state, and risks resetting in-flight state.

### D2: Pass the Activity as the parent `ViewModelStoreOwner`

Read `LocalViewModelStoreOwner.current` at `MainNavigation` scope (there it is the `ComponentActivity`). The decorator creates one child store per entry under it and clears each child store when its entry is popped (`removeViewModelStoreOnPop = { true }`), so no VM state survives a navigation back.

### D3: Keep `ClientFormViewModel`/`ClientDetailViewModel` as-is

With per-entry scoping, a fresh `ClientFormViewModel` is created for every form navigation: `init` runs and, in edit mode (`clienteId != null`), `loadCliente(clienteId)` populates the correct client. No ViewModel code changes required.

## Risks / Trade-offs

- [VMs no longer survive a pop + re-add navigation] → Intended; create mode must start empty. Screens load authoritative data from the repository on init.
- [Process-death + state restoration yields fresh (empty) VMs instead of previous form state] → Acceptable and consistent with the create-mode-must-be-empty requirement; forms re-populate from the repository in edit mode.
- [`LocalViewModelStoreOwner.current` accidentally resolves to a child owner inside `NavDisplay`] → `MainNavigation` reads it before `NavDisplay`, so it is always the Activity. Asserted in the regression test by behavior, not by code.

## Migration Plan

Single-file change (`Navigation.kt`) plus tests; no data migration, no schema change, no dependency changes. Rollback is reverting the `decorators` wiring.

## Open Questions

None.
