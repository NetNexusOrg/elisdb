## 1. Data layer

- [x] 1.1 Remove `val telefone: String? = null` from the `Cliente` entity (`app/src/main/java/com/example/elisdb/data/local/Cliente.kt`).
- [x] 1.2 Remove `val telefone: String? = null` from `ClienteDraft` (`app/src/main/java/com/example/elisdb/data/ClienteRepositoryImpl.kt`).
- [x] 1.3 Remove the `telefone = draft.telefone?.takeIf { it.isNotBlank() }` mapping from both the insert and update branches of `ClienteRepositoryImpl.save()`.
- [x] 1.4 In `AppDatabase.kt`, bump `version = 2` to `version = 3` and add `MIGRATION_2_3` running `ALTER TABLE clientes DROP COLUMN telefone`.
- [x] 1.5 Wire `MIGRATION_2_3` into `AppContainer`'s `Room.databaseBuilder(...).addMigrations(...)` list.

## 2. UI layer

- [x] 2.1 `ClientFormViewModel.kt`: remove `telefone` from `ClientFormUiState`, remove `onTelefoneChange()`, and remove the `telefone` read in `loadCliente()` and the `telefone = state.telefone` write in `save()`.
- [x] 2.2 `ClientFormScreen.kt`: remove the `onTelefoneChange` parameter (screen + content), the callback wiring, and the "Telefone" `OutlinedTextField`.
- [x] 2.3 `ClientDetailScreen.kt`: remove the block rendering `cliente.telefone`.
- [x] 2.4 `ClientListViewModel.kt`: remove `cliente.cliente.telefone` from the search-match fields (keep name, contract, notes).

## 3. Tests

- [x] 3.1 `MatchesQueryTest`: remove the case asserting that searching matches a client by phone.
- [x] 3.2 `EditFormLoadsSelectedClientOnlyTest` and `ClienteRepositoryImplTest`: drop `telefone = ...` from the seeded `ClienteDraft` values.
- [x] 3.3 `AppDatabaseMigrationTest`: add a `2→3` migration case asserting the `telefone` column no longer exists and existing rows survive.

## 4. Verification

- [x] 4.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm BUILD SUCCESSFUL.
- [x] 4.2 Run `./gradlew :app:connectedDebugAndroidTest` on the emulator and confirm all instrumented tests pass.
- [x] 4.3 Manual check on the emulator: upgrade path (install app over an install with an existing DB), the create form has no "Telefone" field, the detail screen shows no phone, and searching by the old phone value no longer matches.
