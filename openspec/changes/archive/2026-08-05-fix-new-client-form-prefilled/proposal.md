## Why

When the user taps "add client" (FAB) on the list screen, the create form is shown pre-filled with the name, phone and other data of the previously opened client instead of empty fields. The bug is reproducible: create a client, go back, open the create form again — the old values are still there. A "Novo cliente" title with stale field values is confusing and can cause saving data into the wrong client.

## What Changes

- Ensure each navigation into the create/edit form starts with a **fresh, empty form** (create mode) or **loads only that client's data** (edit mode), never reusing the state of a previously opened form.
- Register a per-navigation-entry `ViewModelStoreOwner` decorator so every back-stack entry owns its ViewModels, instead of all screens sharing the Activity's ViewModel store.
- Add an instrumented regression test proving a freshly opened create form starts empty after another client was created, and that editing a client loads only that client's data.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `client-screens`: the create/edit form requirement gains the guarantee that a form opened in create mode SHALL start with empty fields and a form opened in edit mode SHALL load the data of the selected client only, regardless of previously opened forms.

## Impact

- `app/src/main/java/com/example/elisdb/Navigation.kt` — wire `rememberViewModelStoreNavEntryDecorator` into `NavDisplay` (fixes VM scoping for all entries: list, form, detail, photo viewer).
- Root cause: Navigation3 1.0.1 `NavDisplay` does not scope ViewModels per entry; without the lifecycle `ViewModelStoreNavEntryDecorator`, every `viewModel { }` resolves to the Activity's store, so `ClientFormViewModel` is shared across navigations and keeps the last filled state.
- Dependencies: `androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0` is already a direct dependency; no version changes.
- Tests: new instrumented tests under `app/src/androidTest/java/com/example/elisdb/ui/form/`.
