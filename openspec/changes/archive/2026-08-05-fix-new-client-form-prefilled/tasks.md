## 1. Fix ViewModel scoping

- [x] 1.1 In `app/src/main/java/com/example/elisdb/Navigation.kt`, add imports `androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner` and `androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator`.
- [x] 1.2 In `MainNavigation()`, before `NavDisplay`, read `val viewModelStoreOwner = LocalViewModelStoreOwner.current` and create `val entryDecorator = rememberViewModelStoreNavEntryDecorator(viewModelStoreOwner = viewModelStoreOwner, removeViewModelStoreOnPop = { true })`.
- [x] 1.3 Pass `decorators = listOf(entryDecorator)` to `NavDisplay` (import `androidx.navigation3.runtime.NavEntryDecorator` if needed). Leave the `entryProvider { }` blocks and all `viewModel { }` calls unchanged.

## 2. Regression tests

- [x] 2.1 Add `app/src/androidTest/java/com/example/elisdb/ui/form/CreateFormStartsEmptyTest.kt` following the `EditFormSaveTest` pattern (real Room DB + `createAndroidComposeRule` + `ClientFormViewModel`): create a client via `ClienteRepositoryImpl.save(...)`, compose `ClientFormScreen` with `clienteId = null`, assert the name/phone/notes fields contain no text; then compose a second `ClientFormScreen` for a new client and assert the fields are empty again.
- [x] 2.2 In the same file (or `EditFormLoadsSelectedClientOnlyTest.kt`): seed two clients via the repository, compose the form for client B after the form for client A was already composed, and assert the name field shows client B's name and not client A's.
- [x] 2.3 Add a full-app instrumented navigation test `NewClientFormNotPrefilledTest.kt` using `createAndroidComposeRule<ComponentActivity>` with the real `MainNavigation`: seed a client via the repository, tap the add (FAB) action, assert the create form title "Novo cliente" shows and the name field is empty; leave, open the create form again and assert it is still empty. Use `onNodeWithText(...)`/semantics matchers for field text (no reliance on hardware IME input).

## 3. Verification

- [x] 3.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm BUILD SUCCESSFUL.
- [x] 3.2 Run `./gradlew :app:connectedDebugAndroidTest` on the emulator and confirm the new tests plus the existing 13 instrumented tests pass.
- [x] 3.3 Manual regression on the emulator: create a client, go back, tap "add client" again — the create form must start with empty fields; open an existing client's edit form and confirm it shows only that client's data.
