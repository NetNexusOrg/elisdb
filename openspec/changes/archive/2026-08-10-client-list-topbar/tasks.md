## 1. Add the top bar to the client list screen

- [x] 1.1 In `ClientListScreen.kt` (`ui/cliente`), add a `TopAppBar` to the `Scaffold.topBar` slot with title `Text("Clientes")` (see `SettingsScreen.kt` for the existing pattern)
- [x] 1.2 Add a `TopAppBarActions` section: an `IconButton` (settings, `Icons.Default.Settings`, contentDescription "Definições") calling `onSettingsClick`, followed by an `IconButton` (add, `Icons.Default.Add`, contentDescription "Adicionar cliente") calling `onAddClick`
- [x] 1.3 Remove the `FloatingActionButton` from the `Scaffold.floatingActionButton` slot

## 2. Clean up the search field

- [x] 2.1 Remove the settings `IconButton` from the search field's `trailingIcon` Row, keeping only the clear-search button when `state.query.isNotEmpty()`
- [x] 2.2 Remove now-unused imports if any (e.g. `FloatingActionButton` stays available but unused references, `Icons.Default.Settings` is still used in the top bar)

## 3. Update tests

- [x] 3.1 In `app/src/androidTest/java/com/example/elisdb/ui/cliente/ClientListScreenTest.kt`, verify the "Definições" contentDescription assertion still targets the top bar action, and add a test asserting the top bar exists
- [x] 3.2 In `NewClientFormNotPrefilledTest.kt`, confirm the "Adicionar cliente" contentDescription click still opens the form from the top bar action (should pass unchanged, verify)
- [x] 3.3 Run `./gradlew :app:testDebugUnitTest` and the connected UI tests (`:app:connectedDebugAndroidTest`) to confirm nothing breaks

## 4. Build and verification

- [x] 4.1 Run `./gradlew :app:compileDebugKotlin` and fix any compilation errors
- [x] 4.2 Run `./gradlew :app:assembleDebug` to build the APK