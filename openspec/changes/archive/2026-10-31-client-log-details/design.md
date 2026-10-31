# Design

## Context

`client_audit(cliente_id, key_id, username, action, ts)` is written by `Database.save_client` when a push is accepted. At that moment the server has everything needed to describe the change: the stored row (`nome`, `contrato`, `latitude`, `longitude`, `notas`), the stored photo rows (`id`, `ordem`) and the incoming record with its photo list. After the update those previous values are gone, so the diff has to be computed and persisted then.

Photos are uploaded separately but the push already declares the client's canonical photo set, and `_replace_fotos` removes rows/files no longer listed — so added/removed/reordered is derivable inside the same transaction.

## Decisions

### Details live in `client_audit`, as JSON

`ALTER TABLE client_audit ADD COLUMN details TEXT` (idempotent migration, following the existing `_migrate_keys_*` pattern). The column stores the same object the API returns:

```json
{
  "fields": [
    {"field": "nome", "old": "JOAO", "new": "JOAO SILVA"},
    {"field": "notas", "old": null, "new": "Entregar até sexta", "truncated": false}
  ],
  "photos": {"added": ["foto_a.jpg"], "removed": [], "reordered": false}
}
```

- **`old`/`new` are always strings or null**, formatted server-side (coordinates with 6 decimals). This keeps the client DTO simple and the rendering locale-independent.
- `photos` is omitted when nothing changed in the photo set.
- `fields` is empty for a create (creation is not a change).

Rejected alternatives: a separate `client_audit_changes` table (more joins and migrations for a payload that is always read as a whole), storing the whole previous row (unbounded and exposes data nobody asked for), computing the diff in the app (only knows its own copy; misses duplicate merges and can be wrong after conflicts).

### What counts as a change

| Field | Compared as |
| --- | --- |
| `nome` | normalized (the stored value is already uppercase) |
| `contrato`, `notas` | `null` and `""` are the same; long text truncated to 200 characters with `truncated: true` |
| `latitude`, `longitude` | rounded to 6 decimals (≈0.1 m), so float round-trips do not create noise |
| photos | id lists: `added` = incoming kept ids not stored; `removed` = stored ids not incoming; `reordered` = same set in a different order |

An accepted push whose diff is empty (same values, same photos) records **no** audit entry: the entry would say "edited" without saying what, which is exactly the noise this change removes. The record is still persisted with the bumped delivery timestamp, so delivery semantics are untouched.

### Truncation

Notes and contracts allow 100 000 characters. Storing old and new in full on every edit would bloat the audit table and the history response. Text values are cut at 200 characters and flagged `truncated: true`; the app shows the preview with an ellipsis, which is enough to recognise the edit.

### Creation

A create entry carries no `fields` — the whole record is new — but carries `photos.added` so the log can show that the client was created with photos.

### Deletes stay out of scope

The delete endpoint removes the client and its photos; the app then removes the client from the list, and there is no screen left from which to open its log. Recording `delete` would also require loosening the `CHECK(action IN ('create','edit'))` constraint (a table rebuild). Deletion is already observable through the tombstone and is out of scope here.

### API shape and compatibility

`AuditEntryOut` gains `details: AuditDetailsOut | None = None`; `client_history` parses the stored JSON. Entries written before this change have `details = NULL` and are returned as `null`. Old app builds ignore the unknown field (`ignoreUnknownKeys = true`); the new app renders plain entries when `details` is null.

### Client rendering

`ClientLogScreen` turns each entry into:

- **Field lines**: label in `bodySmall` (`Nome`, `Contrato`, `Notas`, `Localização`), then the change: previous value struck through, arrow, new value. `latitude`/`longitude` are merged into one "Localização" line (`lat, lon → lat, lon`); text values are capped at 3 lines with ellipsis.
- **Photos**: `Adicionou N fotos` with up to four small thumbnails from the local copy (the `fotos` row for that id, if present — no network in the log page), `Removeu N fotos` with a generic icon, `Reordenou as fotos`.
- **Legacy entries** (`details == null`): unchanged, "Criou o cliente" / "Editou o cliente".

The ViewModel loads the local `Foto` rows for all photo ids in one `getByIds` query and exposes `photoUris: Map<String, String>`; a missing id falls back to a placeholder tile.

### Displayed identity

The actor remains the allowlist comment (user name) already stored on the entry. The key id is not shown: two devices of the same person are the same actor for the people reading the log, and the key id is not meaningful to them.

## Risks / trade-offs

- **Diff cost**: one extra `SELECT` of the previous row and photo ids per accepted push. Both are indexed reads inside the transaction already open.
- **Audit volume**: existing databases keep their rows; the new rows are smaller than a full row snapshot and only exist for real changes.
- **Truncation**: a change deep inside a long note is not visible in full; the preview plus the timestamp is the intended trade-off.
- **Photo ids in details are not URLs**: a removed photo no longer exists, so the app must tolerate unresolvable ids (placeholder).
