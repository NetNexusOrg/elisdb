# Spec Delta

## ADDED Requirements

### Requirement: Catalog and services in the sync cycle

A synchronization cycle SHALL include catalog and client-service data. The cycle SHALL resolve the clock, push pending catalog items, push pending clients with their services, pull the catalog, and pull clients with their services.

#### Scenario: Full sync includes the catalog

- **WHEN** a synchronization cycle runs
- **THEN** it SHALL push local catalog changes
- **AND** it SHALL pull the server catalog
- **AND** it SHALL synchronize clients and their installed items

#### Scenario: Clients still sync when the catalog is unchanged

- **WHEN** no catalog item is pending
- **THEN** the cycle SHALL skip the catalog push
- **AND** SHALL still push and pull clients and their services

### Requirement: Catalog sync failure handling

A catalog push failure SHALL NOT corrupt local data. When the server reports the catalog endpoint as not found, the app SHALL treat the catalog as unsupported by that server, keep the local catalog, warn the technician and continue synchronizing clients. Any other catalog push failure SHALL stop the client push for that cycle and SHALL be reported as a sync failure. A catalog pull failure SHALL keep the local catalog and SHALL NOT prevent the client pull.

#### Scenario: Server without catalog support

- **WHEN** the catalog push or pull returns not found
- **THEN** the app SHALL keep the local catalog
- **AND** SHALL show that the server does not support the catalog
- **AND** SHALL continue with the client push and pull

#### Scenario: Catalog push fails

- **WHEN** the catalog push fails for a reason other than not found
- **THEN** the cycle SHALL report a sync failure
- **AND** SHALL NOT push clients in that cycle
- **AND** SHALL keep every pending catalog item

#### Scenario: Catalog pull fails

- **WHEN** the catalog pull fails
- **THEN** the local catalog SHALL remain available
- **AND** the client pull SHALL still run

### Requirement: Pending catalog items are preserved during pull

A catalog pull SHALL NOT overwrite an item that has local pending changes. For an item reported as stale by the push, the app SHALL adopt the server version only when the local item has not changed since that push attempt.

#### Scenario: Pending item survives a pull

- **WHEN** the catalog pull returns an item that is pending locally
- **THEN** the local values SHALL be kept
- **AND** the item SHALL remain pending

#### Scenario: Stale item is reconciled

- **WHEN** a push reports an item as stale and the local item has not changed since
- **THEN** the app SHALL apply the server's item locally
- **AND** SHALL clear the pending state

#### Scenario: Locally edited item is not lost

- **WHEN** a push reports an item as stale but the local item changed again after that push
- **THEN** the app SHALL keep the newer local values
- **AND** SHALL keep the item pending for the next push

### Requirement: Client services are synchronized atomically

A client and its service associations SHALL be pushed and pulled as one unit. A pull that carries associations SHALL replace the local set in the same transaction as the client record. A pull that omits associations SHALL preserve the local set. A client with pending service changes SHALL NOT have its associations replaced by a pull.

#### Scenario: Push sends the complete set

- **WHEN** a pending client with services is pushed
- **THEN** the request SHALL carry the client's complete association set
- **AND** the server SHALL store it together with the client

#### Scenario: Pull replaces the set

- **WHEN** a pull returns a client with associations
- **THEN** the app SHALL replace the client's local associations in the same transaction
- **AND** SHALL NOT leave a partially updated set

#### Scenario: Pull omits associations

- **WHEN** a pull returns a client without an associations field
- **THEN** the app SHALL keep the associations already stored locally

#### Scenario: Pending services are protected

- **WHEN** a client has local service changes pending synchronization
- **THEN** a pull SHALL NOT replace that client's associations
- **AND** the pending set SHALL be pushed in a later cycle

### Requirement: Service sync validation failure is reported

When the server rejects a client because of its associations, the app SHALL report the failure and SHALL keep the client and its associations locally, pending for a later retry.

#### Scenario: Server rejects a service association

- **WHEN** the server rejects a pushed client because an association is invalid
- **THEN** the app SHALL surface the synchronization error
- **AND** SHALL keep the client and its associations locally
- **AND** SHALL NOT mark the client as synchronized

### Requirement: Catalog sync status is visible

The synchronization screen and the client list status SHALL report catalog-specific outcomes without hiding the client synchronization result.

#### Scenario: Unsupported catalog is reported

- **WHEN** the server does not support the catalog
- **THEN** the app SHALL show a warning that the catalog was not synchronized
- **AND** SHALL still show the client synchronization outcome

#### Scenario: Catalog failure is reported

- **WHEN** catalog synchronization fails
- **THEN** the app SHALL show the failure
- **AND** SHALL keep the last successful client synchronization information
