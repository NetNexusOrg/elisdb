## Why

A client's phone number is no longer needed in the app. Removing the "Telefone" field simplifies the create/edit form, the stored client record, the detail screen and the list search — one less piece of data to collect, store and display.

## What Changes

- Remove the `telefone` field from the client record: `Cliente` entity, `ClienteDraft` and `ClienteRepository.save()` no longer carry a phone number. **BREAKING** for the stored schema — a Room migration drops the `telefone` column.
- Remove the "Telefone" input from the create/edit form (`ClientFormUiState`, `ClientFormScreen`).
- Remove the phone display from the client detail screen.
- Remove phone from the client list search match fields (only name, contract and notes are searched).
- Update affected tests (form, repository, migration, search) to drop phone references.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `client-data`: the client record schema no longer includes a phone number; create and update of clients no longer accept a phone number.
- `client-screens`: the create/edit form no longer has a phone field; the detail screen no longer shows a phone; the list search no longer matches against phone.

## Impact

- Data: `Cliente` entity and `ClienteDraft` (remove `telefone`); `ClienteRepositoryImpl.save()` mapping; `AppDatabase` version 2→3 with a new `MIGRATION_2_3` that drops the `telefone` column, wired in `AppContainer`.
- UI: `ClientFormScreen`/`ClientFormViewModel` (remove field, state, change handler); `ClientDetailScreen` (remove phone block); `ClientListViewModel` (remove phone from search fields).
- Tests: `EditFormLoadsSelectedClientOnlyTest`, `ClienteRepositoryImplTest`, `AppDatabaseMigrationTest`, `MatchesQueryTest` updated to drop phone references.
- Dependencies: none.