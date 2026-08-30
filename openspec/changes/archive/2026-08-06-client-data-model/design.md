## Context

The app stores clients/photos in Room (`client-data`), currently using autoincrement `Long` ids (`Cliente.id`, `Foto.id`) and no `updated_at`. The server identifies records by stable TEXT ids and resolves conflicts by LWW on `updated_at`. See proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Make room ids server-compatible (UUID text) and stable across restarts and sync.
- Add `updatedAt` to clients; refresh on edit.
- Add pending/delete-pending markers and queries for the sync engine.
- Migrate existing data without loss.

**Non-Goals:**
- Triggering sync, network I/O, or conflict resolution (those are `client-server-api` / `client-sync-engine`).
- Changing what the server stores.

## Decisions

**1. Switch primary keys to UUID text generated on the device.**
Server ident/bests require a TEXT id and the server already uses UUID strings. Rationale: keeps the device the source for new ids, matches `server-sync` "Unique client identifiers". Alternative: server-allocated ids — rejected, breaks offline-first (no id until online).

**2. Add `updatedAt: Long` maintained by the repository.**
Default to creation, refresh on update. Matches server LWW key. Rationale: enables `save_client` staleness detection without local conflict logic.

**3. Pending/delete state via boolean columns rather than a separate outbox table.**
Simpler for the current model and lets DAOs expose "dirty" queries directly. Alternative: dedicated outbox table — rejected as more moving parts for the same behavior.

**4. Soft delete with a `deletePending` flag instead of immediate row removal.**
The should engine hides it from normal lists and removes it only after server ack. Rationale: transmits offline deletes.

## Risks / Trade-offs

- **Migration from Long→UUID primary keys** → Room `Migration` reconstructs tables (create new, copy, drop); data loss is not acceptable, so a row-preserving migration is verified against export tracks.
- **UI assumes `Long` ids** → the repo interfaces and screens read the new type; addressed by updating call sites.
- **All data, all pending if not acked** → Sync engine (other change) acks; model only tracks.