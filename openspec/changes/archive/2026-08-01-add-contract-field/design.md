## Context

See `proposal.md` — Why. The client record (`Cliente` entity, table `clientes`) currently stores `id`, `nome`, `telefone`, `latitude`, `longitude`, `notas`, `dataCriacao`. The database is version 1 with no migrations wired in `AppContainer` (`Room.databaseBuilder(...).build()`), and `exportSchema = false`. The create/edit form (`ClientFormScreen` + `ClientFormViewModel`) and detail screen (`ClientDetailScreen`) are Compose screens; saving flows through `ClienteRepository.save(draft: ClienteDraft)`.

## Goals / Non-Goals

**Goals:**
- Persist an optional free-text contract reference on the client record, with a non-destructive DB migration for existing installs.
- Expose it in the create/edit form and render it above the name in the detail screen.

**Non-Goals:**
- No validation or constraints on the contract format (free text).
- No changes to the list screen, photos, location, or theme behavior.
- No changes to existing stored data semantics.

## Decisions

### 1. Nullable column, additive migration
Add `contrato: String? = null` to the `Cliente` entity, bump `AppDatabase.version` to 2, and add `MIGRATION_1_2` executing `ALTER TABLE clientes ADD COLUMN contrato TEXT`. Wire it via `.addMigrations(MIGRATION_1_2)` in `AppContainer`.

- Rationale: additive nullable column keeps existing rows valid; ALTER TABLE is the minimal, reversible migration.
- Alternative rejected: `fallbackToDestructiveMigration()` — would wipe existing client data.

### 2. Blank contract stored as null
`ClienteDraft` gains `contrato: String?`; `ClienteRepository.save()` normalizes it with `.takeIf { it.isNotBlank() }`, mirroring the existing `telefone`/`notas` handling.

- Rationale: keeps storage consistent and avoids empty-string rows.
- Alternative considered: storing trimmed empty string — rejected for consistency with existing optional fields.

### 3. Form field above Nome
`ClientFormUiState` gains `contrato: String`, loaded on edit, updated via `onContratoChange`, passed into the draft on save. The UI adds an optional `OutlinedTextField("Contrato")` placed directly above the `Nome` field, matching the detail screen's order (contract above name).

- Rationale: user-visible placement mirrors the detail screen; one ordering everywhere reduces confusion.
- Alternative: placing it after Nome/Telefone — consistent with form-field grouping, but would disagree with the requested detail ordering.

### 4. Detail renders contract above the name
In `ClientDetailContent`, when `cliente.contrato` is non-blank, render it in a `Text` above the name `Row`, styled with `MaterialTheme.typography.bodyMedium` and `onSurfaceVariant` color so it reads as a secondary label above the headline name.

- Rationale: satisfies the "above the name" requirement without disturbing the rest of the layout.
- Alternative: showing a "Contrato" label always — rejected; an empty contract should render nothing (see spec scenario "No contract on the client").

### 5. Migration verified by an instrumented test
Add an instrumented test that builds the database at version 1, inserts a client, closes, then reopens at version 2 with `MIGRATION_1_2` and asserts the row survives with `contrato == null`.

- Rationale: `exportSchema = false` makes Room's `MigrationTestHelper` (which needs exported schemas) heavier to introduce; an open-v2-and-assert approach validates the real migration path with minimal setup.
- Alternative rejected: enabling schema export just for a migration test — more moving parts than the value here.

## Risks / Trade-offs

- [Existing installs crash on upgrade if migration is missing] → `MIGRATION_1_2` is wired in `AppContainer` before release.
- [Column name drift between migration SQL and entity] → single name `contrato` used in both the entity property and the ALTER TABLE, verified by the migration test.
- [Detail content shifts when a contract is present] → the content column is scrollable; layout impact is minimal and expected.
- [Migration test only covers fresh v1→v2] → acceptable; the runtime migration covers existing user data on device.
