## Why

Clients may be associated with a contract reference (e.g. a contract number), which is business-relevant when reviewing a client. Today the app stores no such field, so this information cannot be kept with the client record nor shown in the detail screen.

## What Changes

- Add an optional free-text "Contrato" field to the client record, persisted in the database (additive, non-breaking).
- Make the field editable in the create/edit form.
- Show the contract value in the client detail screen, above the client's name.
- When the contract is empty, the detail screen shows no contract placeholder.

## Capabilities

### New Capabilities

<!-- None - this change modifies existing behavior only -->

### Modified Capabilities

- `client-data`: The client record schema gains an optional contract field, and create/update requirements cover persisting it.
- `client-screens`: The create/edit form gains a contract field, and the client detail screen shows the contract above the name.

## Impact

- Room database: `ClienteEntity` gains a nullable `contrato` column; database version bumped with a migration (`ALTER TABLE ADD COLUMN`).
- Data layer: `ClienteDraft`/repository mappings carry the contract value.
- UI: create/edit form (new optional field) and client detail screen (contract rendered above the name).
- Tests: existing schema/detail tests updated; migration round-trip covered.
