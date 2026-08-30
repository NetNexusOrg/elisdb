---
name: database
description: Guide to the ElisDB SQLite data layer (schema, migrations, LWW conflict resolution, photo storage). Use when changing tables, queries, migrations, or data handling in elisdb-server/ or the Android Room layer.
---

# Database

Base facts in `project-context`. This skill covers the data model and safe-change rules.

## Engine & schema (verified)

- Single SQLite database; WAL journal mode; `busy_timeout=5000`; foreign keys ON.
- Tables (schema string in `elisdb-server/app/database.py` `SCHEMA`):
  - `clientes` — `id` (PK), `nome` (unique `COLLATE NOCASE`), `contrato`, `latitude`, `longitude`, `notas`, `updated_at`, `created_at`.
  - `fotos` — `id` (PK), `cliente_id` (FK → clientes, CASCADE), `mime`, `caminho`, `ordem`.
  - `tombstones` — `id` (PK), `deleted_at` (soft deletes).
  - `client_audit` — `id`, `cliente_id`, `key_id`, `username`, `action` (create|edit), `ts`.
  - `keys` — `id` (PK), `public_key`, `comment`, `provisioned`, `created_at`.

## Photo storage

- Photo **binary** lives on the filesystem (`ELISDB_PHOTOS`); `fotos.caminho` stores the path only.
- Photo ids must stay free of `/`, `\`, `..` (path-traversal guard in `models.FotoIn` and `_foto`).

## Conflict / duplicate rules

- Names normalized (`normalize_nome`: trim, collapse whitespace, uppercase) for duplicate detection.
- `save_client` returns `accepted` | `duplicate` (returns canonical record) | `stale` (LWW by `updated_at`).
- Deletes are tombstones; a newer push can resurrect a tombstoned client.

## Migrations

- Server: migrations are idempotent helper functions (`_migrate_*`) run once per DB file at startup.
- Android: Room migrations (`MIGRATION_1_2` … `MIGRATION_6_7`) in `AppDatabase.kt`; Room schema JSON exported to `app/schemas/` (keep in sync, add a migration test).

## Timezone / time

- All timestamps are Unix epoch seconds (UTC). Do not store local wall-clock or ISO strings.
- Clock skew is reconciled via the server `/api/time` and `ClockOffsetStore`.

## Safety rules

- **Never run destructive operations** (`DROP`, `DELETE FROM`, `VACUUM`, removing columns/rows,
  deleting `.db` files or photo files) **without explicit user confirmation.**
- Never commit `*.db` files, `ELIS.csv`, or `memento-import.db` (real data).
- Back up the `.db` (and photos dir) before any manual schema/data migration.
- Prefer additive migrations; avoid rebuilding tables unless required (as `_migrate_clients_nullable_coords` does, carefully).
