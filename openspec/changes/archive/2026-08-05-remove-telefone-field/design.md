## Context

See proposal.md - Why. The client record currently has `telefone: String?`. It is stored in the `clientes` table (`telefone` column), carried in `ClienteDraft`, mapped in `ClienteRepositoryImpl.save()`, shown as an `OutlinedTextField` in `ClientFormScreen`, rendered in `ClientDetailScreen`, and used as a search field in `ClientListViewModel`. The DB is at version 2 with `MIGRATION_1_2` (adds `contrato`) wired in `AppContainer`.

## Goals / Non-Goals

**Goals**
- Remove the phone field end-to-end: from the stored record, CRUD mapping, form UI/state, detail UI, and list search.
- Migrate existing installs cleanly so an upgraded DB has no `telefone` column and no data loss.
- Keep all existing instrumented/unit tests green, updated to no longer reference phone.

**Non-Goals**
- No new fields or behaviors.
- No change to photos, contract, location, notes, or search semantics beyond dropping phone.

## Decisions

### D1: Drop the `telefone` column via a Room migration

Bump `AppDatabase` version 2→3 and add `MIGRATION_2_3` running `ALTER TABLE clientes DROP COLUMN telefone`; wire it into `AppContainer`'s migration list next to `MIGRATION_1_2`.

Rationale: removing the field from the schema keeps the DB honest and small. The alternative (leaving the stale column but ignoring it via the ORM) avoids a migration but leaves dead data and a mismatched schema. SQLite supports `ALTER TABLE ... DROP COLUMN` for this plain, non-indexed nullable column, and Room emits the migration normally. `exportSchema = true` is already set, so the schema diff validates the migration.

Migration risk: if any index/view/trigger references `telefone`, SQLite rejects the drop — currently none do; verified at implementation.

### D2: Remove `telefone` from the data layer

- `Cliente` entity: delete `telefone` property.
- `ClienteDraft`: delete `telefone` field.
- `ClienteRepositoryImpl`: remove the `telefone = draft.telefone?...` mapping in both the insert and update branches.

### D3: Remove phone from the UI layer

- `ClientFormViewModel`: drop `telefone` from `ClientFormUiState`, drop `onTelefoneChange()`, drop the `telefone` read in `loadCliente()` and the `telefone = state.telefone` write in `save()`.
- `ClientFormScreen`: remove the "Telefone" `OutlinedTextField` and the `onTelefoneChange` parameter/callback.
- `ClientDetailScreen`: remove the block that renders the phone (around `cliente.telefone`).
- `ClientListViewModel`: remove `cliente.cliente.telefone` from the search-match fields (name, contract, notes remain).

### D4: Update tests

- `MatchesQueryTest`: remove the case asserting phone matching.
- `EditFormLoadsSelectedClientOnlyTest`, `ClienteRepositoryImplTest`: drop `telefone = ...` from the seeded `ClienteDraft`.
- `AppDatabaseMigrationTest`: add a 2→3 case asserting the `telefone` column is gone (and existing rows survive).

## Risks / Trade-offs

- [SQLite refuses the `DROP COLUMN` if a dependent schema object exists] → Confirmed none reference `telefone`; the migration test validates it.
- [Devices upgrading from v1 or v2] → Migrations are chained (`1→2`, `2→3`), so every path is covered; the migration test covers `2→3`.
- [A R8 keep rule references `telefone`] → The `-keep class com.example.elisdb.data.**` rule is wholesale (not field-specific), so it needs no change.

## Migration Plan

Ship as part of the next release. Existing installs run `MIGRATION_2_3` on first launch after upgrade; the column is dropped and no client data is lost. No backfill or API changes.

## Open Questions

None, assuming the migration test passes on the 2→3 boundary (verified in tasks).