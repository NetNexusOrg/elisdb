## 1. Data layer

- [x] 1.1 Add `contrato: String? = null` to the `Cliente` entity
- [x] 1.2 Bump `AppDatabase.version` to 2, add `MIGRATION_1_2` executing `ALTER TABLE clientes ADD COLUMN contrato TEXT`, and wire `.addMigrations(MIGRATION_1_2)` in `AppContainer`
- [x] 1.3 Add `contrato: String? = null` to `ClienteDraft` and persist it in `ClienteRepository.save()` (store null when blank, matching `telefone`/`notas` handling)
- [x] 1.4 Add an instrumented migration test: build the DB at version 1, insert a client, reopen at version 2 with `MIGRATION_1_2`, assert the row survives and `contrato` is null

## 2. Create/edit form

- [x] 2.1 Add `contrato: String` to `ClientFormUiState`; populate it in `loadCliente`, add `onContratoChange`, and pass it into the `ClienteDraft` in `save()`
- [x] 2.2 Add an optional "Contrato" `OutlinedTextField` directly above the `Nome` field in `ClientFormContent`
- [x] 2.3 Extend `ClientFormViewModelTest`: new mode defaults to empty contract, edit mode loads the stored contract, and save includes the contract in the draft (blank stored as null)

## 3. Detail screen

- [x] 3.1 Render the contract above the name in `ClientDetailContent` when non-blank (e.g. `bodyMedium`/`onSurfaceVariant` above the `headlineSmall` name row)
- [x] 3.2 Add an instrumented Compose test for the detail screen (fake repository-backed VM) asserting the contract text renders above the name, and that a blank contract renders nothing above the name
- [x] 3.3 Extend `ClientDetailViewModelTest` if the contract field affects existing assertions

## 4. Verification

- [x] 4.1 Run `./gradlew assembleDebug lintDebug` and confirm a green build
- [x] 4.2 Run `./gradlew testDebugUnitTest` and confirm all unit tests pass
- [x] 4.3 Run `./gradlew connectedDebugAndroidTest` on the emulator and confirm the migration and detail-screen tests pass
- [x] 4.4 Manual smoke-test on the emulator: create a client with a contract and confirm the detail screen shows it above the name; edit the contract; clear it and confirm nothing renders above the name; force-stop and relaunch to confirm the contract persists
