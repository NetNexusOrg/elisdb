# Proposal

## Why

Technicians need to record which pest-control devices and treatments are installed at a client, with the installed quantity, and see that information when consulting the client on site. Today the app only stores free-form notes, so this information is inconsistent and cannot be shared through sync.

## What Changes

- Add a shared **Catálogo** of device models and treatments, organized in four fixed categories: Roedores, Insetos Rastejantes, Insetos Voadores and Tratamentos. The catalog starts empty, is managed by any authenticated technician, and items are only deactivated/reactivated (never hard-deleted).
- Add a per-client set of catalog items: devices carry an installed quantity (1–9999); treatments are presence-only. The same item cannot be associated twice with a client.
- Add a read-only "Aparelhos e Tratamentos" section to the client detail screen, after the notes and before the photos, with totals per category and the installed quantity per device.
- Add a "Aparelhos e tratamentos" section to the client create/edit form, where the technician picks active catalog models per category and enters the quantity for devices.
- Add a **Catálogo** screen reachable from the client-list menu, with always-expanded category lists, create/edit dialogs and deactivate/reactivate actions.
- Synchronize catalog items and client services using the existing offline-first push/pull flow, with atomic client + services persistence, last-write-wins conflict resolution and duplicate reconciliation.
- Record catalog changes in the global activity log and service changes in the client history.
- Keep progressive compatibility: a new app keeps syncing clients against an old server without catalog endpoints, and an old app ignores the new service data.

## Capabilities

### New Capabilities

- `device-catalog`: shared catalog of device models and treatments, including categories, naming rules, activation state, local persistence and sync protocol.
- `client-services`: catalog items installed at a client, quantities, validation, detail/form presentation and per-client sync behavior.

### Modified Capabilities

- `client-data`: local schema gains catalog items and client service records.
- `client-screens`: client list menu, client detail section, create/edit form and global activity log gain catalog/service behavior.
- `client-sync`: sync cycle pushes and pulls catalog items and client services, with old-server fallback.
- `server-sync`: server exposes catalog endpoints, stores catalog items and client services atomically, and records catalog/service audit entries.
- `duplicate-reconciliation`: merging a duplicate client also merges its services.

## Impact

- Server: `elisdb-server/elisdb/database.py`, `main.py`, `models.py`, new catalog module, schema migration, pytest suite.
- Android: Room entities/DAOs/migrations (v13 → v14 → v15), repository, sync coordinator, remote DTOs, navigation, Catálogo screen, client form/detail/log, unit and instrumented tests.
- Docs: server API/database docs and Android user-facing documentation.
- No production deployment, release or version bump is part of this change.
