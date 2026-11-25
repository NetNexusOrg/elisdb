# Tasks

## 1. OpenSpec change

- [x] 1.1 Write `proposal.md`, spec deltas, `design.md` and `tasks.md` for `add-catalog-client-services`; verify with `openspec validate add-catalog-client-services`.
- [x] 1.2 Confirm the change declares exactly `device-catalog`, `client-services`, `client-data`, `client-screens`, `client-sync`, `server-sync` and `duplicate-reconciliation`; verify with `openspec show add-catalog-client-services`.

## 2. Server catalog schema and migration

- [x] 2.1 Add `catalogo_itens` and its unique `(categoria, nome_normalizado)` index to the server schema; verify a fresh database creates the table and index.
- [x] 2.2 Add an idempotent in-place migration for existing databases; verify it runs twice without data loss on a copy of a v2.2.0 database.
- [x] 2.3 Implement the shared name normalization (trim, collapse whitespace, strip diacritics, uppercase) and category constants; verify unit tests for accents, case and whitespace.
- [x] 2.4 Extend `client_audit` with `kind`, nullable `cliente_id`, `item_id`, `item_nome` and the widened action check; verify the rebuild preserves every existing row and id.

## 3. Server catalog API and history

- [x] 3.1 Add `GET /api/catalog` returning all items plus `complete: true`; verify inactive items are included and authentication is required.
- [x] 3.2 Add `POST /api/catalog/push` with the 200-item bound and per-item `accepted|unchanged|stale|invalid` results; verify invalid items do not abort a batch.
- [x] 3.3 Enforce catalog validation and LWW: name limits, identifier pattern, immutable category, duplicate normalized name, stale/unchanged handling; verify each case in tests.
- [x] 3.4 Record catalog create/rename/deactivate/reactivate entries in `client_audit` as `kind='catalog'`; verify the global history query returns them in the same cursor order.
- [x] 3.5 Keep `/api/clients` and `/api/pull` unaffected by catalog fields; verify existing server tests still pass.
- [x] 3.6 Add catalog endpoint, schema and migration tests under `elisdb-server/tests/`; verify `python -m pytest` passes.
- [x] 3.7 Document the catalog tables, endpoints and push result codes in `docs/referencia/`; verify the documented examples match the tests.

## 4. Android catalog storage and sync

- [x] 4.1 Add Room database version 14 with the `catalogo_itens` entity and migration from 13 (services land in version 15); verify the exported schema JSON matches the entity.
- [x] 4.2 Add `CatalogoDao`, domain model, repository interface and implementation; verify DAO tests for insert, update, pending and alphabetical reads.
- [x] 4.3 Add catalog DTOs and mappers; verify unit tests for accepted, unchanged, stale and invalid results.
- [x] 4.4 Add catalog push/pull to `SyncCoordinator` in the agreed order, including 404-as-unsupported and failure behavior; verify sync unit tests for success, 404, other push failure and pull failure.
- [x] 4.5 Add catalog pending/stale reconciliation and protect pending items during pull; verify unit tests for each branch.
- [x] 4.6 Wire the catalog repository and sync dependencies in `AppContainer` and `Navigation`; verify the app builds with `:app:assembleDebug`.

## 5. Catálogo screen

- [x] 5.1 Add the Catálogo navigation destination and the menu entry between Logs and Configuração; verify the menu opens the screen and back returns to the list.
- [x] 5.2 Implement the always-expanded category list, pt-PT alphabetical ordering, inactive label and empty state; verify with Compose UI tests.
- [x] 5.3 Implement create/edit dialogs with name, category (fixed on edit), duplicate feedback and pending indication; verify Compose UI tests for create, edit and duplicate rejection.
- [x] 5.4 Implement deactivate/reactivate from the row menu; verify the row label and actions change and persistence survives restart.
- [x] 5.5 Follow the project's existing literal-text style for catalog screens (the current UI does not use `strings.xml`); verify the wording matches the specs.

## 6. Phase 1 verification

- [x] 6.1 Run `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:connectedDebugAndroidTest` and the server pytest suite; all pass.
- [x] 6.2 Manually create catalog items on two devices, sync offline/online, deactivate one and verify both devices converge.
- [x] 6.3 Manually run the new app against a server without catalog endpoints; verify the warning appears and clients still sync.
- [x] 6.4 Update Android and server documentation for the catalog; verify the documented flows match the app.

## 7. Server client services

- [x] 7.1 Add `cliente_servicos` with the client cascade and item restrict foreign keys; verify a fresh database and the migration create it.
- [x] 7.2 Add `ServicoIn` and nullable `servicos` to `ClienteIn`/`ClienteOut`; verify absent, empty and non-empty semantics in model tests.
- [x] 7.3 Validate item existence, device quantity 1–9999, treatment null quantity, duplicates and the 200 limit before writing; verify each rejection leaves the client unchanged.
- [x] 7.4 Store client and services atomically on push, include services on pull, exclude them from `/api/clients`; verify with endpoint tests.
- [x] 7.5 Compute and record the service diff in client history with snapshotted item names; verify added, removed and quantity-change entries.
- [x] 7.6 Ensure client deletion cascades associations and tombstones keep working; verify with tests.
- [x] 7.7 Add server service tests and update API/database docs; verify `python -m pytest` passes and documented examples match.

## 8. Android service storage and sync

- [x] 8.1 Add Room version 15 with the `cliente_servicos` entity, composite key and client cascade; verify the 13→14→15 migration tests.
- [x] 8.2 Add DAO methods and repository methods to read, replace and delete associations; verify atomic replacement and client-delete cascade tests.
- [x] 8.3 Add `ServicoDto` to client DTOs and mappers; verify null preserves, empty clears and a list replaces.
- [x] 8.4 Push services with pending clients and pull services in the client transaction; verify sync tests for replace, null-preserve, pending protection and server rejection.
- [x] 8.5 Merge services on duplicate reconciliation by item with the greater device quantity, then mark the canonical client pending; verify unit tests.
- [x] 8.6 Include catalog and service data in the destructive local reset and full pull; verify the reset test.

## 9. Client form and detail

- [x] 9.1 Add the "Aparelhos e tratamentos" form section after tags and before location, with the four category pickers and active-item filtering; verify Compose UI tests for each category and empty states.
- [x] 9.2 Add device quantity fields and treatment rows without quantity, plus row removal and duplicate prevention; verify form validation tests.
- [x] 9.3 Save client and services atomically from the form and reload them in edit mode; verify the save and edit UI tests.
- [x] 9.4 Add the read-only "Aparelhos e Tratamentos" detail section after notes and before photos, with group totals and fixed category order; verify UI tests for totals, empty groups, inactive items and hidden unknown items.
- [x] 9.5 Add the empty state "Sem aparelhos ou tratamentos." and quantity formatting; verify the detail UI test.

## 10. Logs and history UI

- [x] 10.1 Parse catalog entries from the global history and render item, category and action without navigation; verify UI tests.
- [x] 10.2 Render service added/removed/quantity changes in the client log; verify UI tests including old entries without service details.
- [x] 10.3 Ensure catalog entries do not break paging, grouping or retry states; verify the activity log tests.

## 11. Phase 2 verification

- [x] 11.1 Run the full Android unit, lint and instrumentation suites plus the server pytest suite; verify all pass.
- [x] 11.2 Run `openspec validate add-catalog-client-services --strict`; verify it passes with no errors.
- [x] 11.3 Manually associate devices and treatments on two devices, sync offline/online, edit quantities, deactivate an installed item and verify the detail totals and logs.
- [x] 11.4 Verify an old app against the new server and the new app against an old server both keep client sync working.
- [x] 11.5 Update Android and server documentation for client services and the catalog history; verify the documented flows match the app.

## Workflow follow-up

- Sync the spec deltas into `openspec/specs/` and archive the change after review.
- Do not create a release, tag, or production deployment as part of this change.
