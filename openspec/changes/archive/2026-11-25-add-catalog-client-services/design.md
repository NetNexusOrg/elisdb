# Design

## Context

See `proposal.md`. The starting point is the stable v2.2.0 code: Room database version 13, server schema with `clientes`, `fotos`, `tombstones`, `client_audit`, `push_idempotency`, `keys` and `server_meta`, and a client push/pull protocol that already uses last-write-wins and a single audit cursor. The feature must remain additive and must not repeat the previous attempt's broad refactor.

Constraints:

- Offline-first: every catalog and service operation must work without a server.
- One shared catalog across technicians; no per-user ownership.
- No hard deletes; no seed data.
- Server deployed before the app; old app/new server and new app/old server must not break client sync.
- Existing client UI, map and photo behavior must not change.

## Goals / Non-Goals

**Goals:**

- Add catalog and client-service storage, sync, validation and UI in two independently verifiable phases.
- Keep client + services persistence atomic on both sides.
- Preserve a single chronological activity log with one paging cursor.
- Keep the client push/pull protocol tolerant of missing fields and unknown fields.

**Non-Goals:**

- Stock control, billing, scheduling, reports or dashboards.
- Administrative roles, approval flows or catalog ownership.
- Hard deletion of catalog items.
- Changes to the client list, map or photo pipeline.
- Release, version bump or production deployment.

## Decisions

### Two capabilities, one change, two phases

`device-catalog` owns catalog lifecycle and sync; `client-services` owns associations, quantities, form/detail presentation and service sync. Phase 1 delivers the catalog end-to-end and stops at a manual two-device checkpoint. Phase 2 adds associations on top. This keeps each phase reviewable and avoids the previous "everything at once" failure mode.

### Server schema

Add two tables:

- `catalogo_itens(id TEXT PRIMARY KEY, categoria TEXT NOT NULL, nome TEXT NOT NULL, nome_normalizado TEXT NOT NULL, ativo INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)` with a unique index on `(categoria, nome_normalizado)`.
- `cliente_servicos(cliente_id TEXT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE, item_id TEXT NOT NULL REFERENCES catalogo_itens(id) ON DELETE RESTRICT, quantidade INTEGER, PRIMARY KEY (cliente_id, item_id))`.

Categories are stored as stable identifiers (`roedores`, `insetos_rastejantes`, `insetos_voadores`, `tratamentos`) and mapped to Portuguese labels in the UI. `nome_normalizado` is computed server-side and stored so uniqueness is enforced by SQLite, not only by application code. Normalization: trim, collapse whitespace, strip diacritics, uppercase.

Rationale: a separate catalog table keeps client records small; a join table models presence and quantity naturally and lets a treatment store `NULL`. The `ON DELETE RESTRICT` on items is safe because items are never deleted; it prevents accidental orphan associations.

### Audit: extend `client_audit` instead of a second table

The existing audit table becomes the single history source with:

- `kind TEXT NOT NULL DEFAULT 'client' CHECK (kind IN ('client','catalog'))`
- nullable `cliente_id` (catalog entries have none)
- `item_id TEXT`, `item_nome TEXT`
- widened action check: `create|edit|delete|rename|deactivate|reactivate`

SQLite cannot alter a CHECK constraint, so the migration rebuilds the table (create new, copy, drop, rename) inside a transaction, preserving `id` values. The global `before_id` cursor and the per-client `after_id` query keep working. Client queries add `kind='client'`; catalog entries are returned only by the global query.

Rationale: one monotonic cursor and one screen; avoids the previous implementation's `activity_seq` union and its parsing bugs. Alternative considered: a separate `catalog_audit` table merged in the API, rejected because it complicates paging and ordering.

### Catalog API

- `GET /api/catalog` returns `{ "items": [...], "complete": true }`, including inactive items.
- `POST /api/catalog/push` accepts `{ "items": [...] }` with at most 200 items and returns `{ "results": [{ "id", "status": "accepted|unchanged|stale|invalid", "reason"?, "item"? }] }`.
- Each item is processed independently inside the request transaction; an invalid item does not abort the batch.
- LWW by `updated_at`; identical content and timestamp is `unchanged`; older is `stale` and returns the canonical item; changing the category of an existing item is `invalid`; duplicate normalized names are `invalid` with `duplicate_name`.
- Retries are naturally idempotent by item id + timestamp, so no `Idempotency-Key` is required.
- Limits: 1–64 identifier characters (`A-Za-z0-9_-`), name 1–80, at most 200 items per push; there is no hard total catalog cap.

Alternative considered: pushing catalog items through `/api/push`. Rejected because catalog is global, has different validation, and would make the client batch semantics harder to reason about.

### Client API and atomic services

`ClienteIn` and `ClienteOut` gain `servicos: list[ServicoIn] | None`. `ServicoIn` is `{ item_id, quantidade }`, with `quantidade` absent/null for treatments. The field default is `None`:

- absent/`null` → preserve stored associations (old app compatibility),
- `[]` → clear associations,
- non-empty list → replace the complete set.

Validation: item exists; device quantity 1–9999; treatment quantity null; no duplicate `item_id`; at most 200 associations per client. Validation runs before any write; the client upsert and the association replacement happen in one transaction, so a failure leaves both untouched.

`/api/clients` returns summaries without associations to avoid N+1 queries. `/api/pull` includes associations for each returned client.

Rationale: reusing the client record keeps the sync protocol and pending model unchanged; an explicit `None` distinguishes "old client, do not touch" from "user removed everything".

### Server history for services

On an accepted client push, the server loads the previous association set, computes added/removed/quantity changes, and writes a client audit entry with `details.services = { added: [...], removed: [...], changed: [{ item_id, item_nome, from, to }] }`. Item names are snapshotted into the detail so history survives renames. Catalog actions write `kind='catalog'` entries with `item_nome` and category in details. No audit entry is written when nothing changed.

### Android local schema

- Room **v14** adds `catalogo_itens` (`id`, `categoria`, `nome`, `ativo`, `pending`, `syncError`, `createdAt`, `updatedAt`, `revision`), with a migration that creates the table.
- Room **v15** adds `cliente_servicos` (`clienteId`, `itemId`, `quantidade`) with a composite primary key and a foreign key to `clientes` with `ON DELETE CASCADE`. There is deliberately **no** foreign key to the catalog, so an association can arrive before its item and is hidden by the UI until the item is known.
- No new column on `clientes`. Changing associations updates `pending=true`, `updatedAt` and `revision`, and saves client + associations in one Room transaction. The existing pull rule that skips pending clients protects the association set until the push is acknowledged.

Rationale: a separate `servicosPending` flag would duplicate state already represented by `pending`; hiding unknown items avoids dropped associations and preserves future catalog data.

### Android architecture

- `CatalogoItem` entity + `CatalogoDao`; `ClienteServico` entity + methods on the existing client DAO.
- `CatalogoRepository` (interface + implementation) for catalog CRUD, pending marking and sync application; `ClienteRepository` gains association read/save methods. Both are wired in `AppContainer`.
- DTOs: `CatalogoItemDto`, `CatalogoPullResponse`, `CatalogoPushRequest/Response`, `ServicoDto`; `ClienteDto` gains nullable `servicos`.
- `SyncCoordinator` order: clock → catalog push → client push → catalog pull → client pull. A 404 on catalog endpoints sets an "unsupported" warning and continues with clients; any other catalog push failure aborts the client push for that cycle; a catalog pull failure keeps local data and still pulls clients.
- Catalog pull skips pending local items. A `stale` result applies the canonical item only if its `revision` is unchanged since the push. Client pull replaces associations in the same transaction as the client; a `null` field preserves them.

### UI

- Navigation gains a catalog destination, opened from the client-list overflow menu between Logs and Configuração, with a `DevicesOther` icon.
- Catálogo screen: top bar with back/title/add; four always-expanded sections in fixed order; items sorted with a pt-PT collator ignoring case/diacritics; row name, discreet "Inativo" label and a three-dot menu with Editar/Desativar or Reativar; create/edit dialog with name and category (category fixed on edit); duplicate error inline; pending indicator.
- Client form: "Aparelhos e tratamentos" after the tags and before the location. Four category buttons open a scrollable list of active items not already associated; choosing appends a fixed row. Device rows use a numeric field (1–9999); treatment rows have no quantity. Each row has a remove action; empty categories explain that no models are available.
- Client detail: "Aparelhos e Tratamentos" after notes and before photos. Groups in fixed category order, empty groups omitted. Header totals: sum of quantities for devices, count for treatments. Rows show name and quantity. Inactive items are shown without a label and counted. Unknown items are hidden and not counted. Empty state: "Sem aparelhos ou tratamentos."
- Log: catalog entries show item, category and action and are not clickable; client entries add a compact added/removed/quantity section. Existing entries render unchanged.

### Compatibility and rollout

Deploy the server first. It accepts old clients (no `servicos` → preserve). The new app against an old server treats catalog 404 as "unsupported", keeps the local catalog, warns and continues client sync. The old app against the new server ignores `servicos` on pull and omits it on push. No production release is part of this change.

## Risks / Trade-offs

- **Audit table rebuild can lose history** → rebuild in one transaction, copy all rows and ids, test migration against a v2.2.0-shaped database, and back up the SQLite file before deploying.
- **Normalization differences between app and server** → keep the canonical normalization on the server, mirror it in Kotlin, and test accents, case and whitespace.
- **Unknown catalog item hidden in the detail** → association is preserved and becomes visible after the catalog pull; accepted undercount until then.
- **Pending client blocks all pull updates for that client** → existing behavior; services inherit it, which is preferable to silently overwriting local edits.
- **Catalog push failure stops client push** → keeps catalog references valid before clients are pushed; the next cycle retries both.
- **Old server reports 404 for an unrelated reason** → treat 404 only on the two catalog paths as "unsupported"; any other status remains a failure.
- **Association limits** → enforce 200 on both sides so a client record cannot grow unbounded.
- **Deactivated item remains installed** → intentional; it stays visible for existing clients but is not offered for new ones.

## Migration Plan

1. Back up the server database and deploy the server with the additive schema migration; verify old clients still push/pull.
2. Release the Phase 1 app (catalog only) and verify catalog sync on two devices; old app/new server remains valid.
3. Release the Phase 2 app (services) after the Phase 2 checkpoint.
4. Rollback: the server's new tables and columns are ignored by the previous server binary; the Android Room migrations are forward-only, so a rollback requires reinstalling the previous app version, which keeps client data but ignores catalog tables.

## Open Questions

None.
