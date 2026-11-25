# Spec Delta

## ADDED Requirements

### Requirement: Catalog API and storage

The server SHALL store catalog items and expose an authenticated catalog pull and push. The pull SHALL return every item, active and inactive, with a completeness indicator. The push SHALL accept a bounded batch and report a result per item.

#### Scenario: Pull every catalog item

- **WHEN** an authenticated client requests the catalog
- **THEN** the server SHALL return every catalog item, active and inactive
- **AND** SHALL indicate that the response is complete

#### Scenario: Push a catalog batch

- **WHEN** an authenticated client pushes catalog items
- **THEN** the server SHALL process each item
- **AND** SHALL return accepted, unchanged, stale or invalid for each one

#### Scenario: Catalog push is bounded

- **WHEN** a catalog push contains more than the supported batch size
- **THEN** the server SHALL reject the request
- **AND** SHALL NOT store any item from it

#### Scenario: Catalog requires authentication

- **WHEN** an unauthenticated request reaches a catalog endpoint
- **THEN** the server SHALL reject it with an authentication error

### Requirement: Catalog item validation and conflict resolution

The server SHALL enforce the catalog item fields, categories and normalized-name uniqueness per category. It SHALL resolve conflicts by modification timestamp, SHALL treat identical content and timestamp as unchanged, and SHALL reject a category change for an existing item.

#### Scenario: Invalid field is rejected

- **WHEN** an item has an empty name, an overlong name, an invalid category or an invalid identifier
- **THEN** the server SHALL report that item as invalid
- **AND** SHALL NOT store it

#### Scenario: Duplicate name is rejected

- **WHEN** an item's normalized name duplicates another item in the same category
- **THEN** the server SHALL report it as invalid with a duplicate-name reason
- **AND** SHALL NOT store it

#### Scenario: Category cannot change

- **WHEN** an existing item is pushed with a different category
- **THEN** the server SHALL report it as invalid
- **AND** SHALL keep the stored category

#### Scenario: Newer item wins

- **WHEN** a pushed item has a newer modification timestamp than the stored item
- **THEN** the server SHALL store the pushed values

#### Scenario: Older item is stale

- **WHEN** a pushed item has an older modification timestamp than the stored item
- **THEN** the server SHALL report it as stale
- **AND** SHALL keep the stored values

#### Scenario: Identical item is unchanged

- **WHEN** a pushed item has the same content and timestamp as the stored item
- **THEN** the server SHALL report it as unchanged
- **AND** SHALL NOT duplicate it

#### Scenario: Invalid item does not abort the batch

- **WHEN** one item in a batch is invalid
- **THEN** the server SHALL continue processing the other items
- **AND** SHALL return the per-item results together

### Requirement: Client services storage and validation

The server SHALL store client service associations linked to a client and a catalog item. It SHALL validate the item, the category-dependent quantity rule, duplicates and the per-client limit, and SHALL store the client fields and the association set atomically.

#### Scenario: Store associations with a client

- **WHEN** a client is pushed with associations
- **THEN** the server SHALL store the client and the complete set in one transaction

#### Scenario: Unknown item rejects the client

- **WHEN** an association references a catalog item that does not exist
- **THEN** the server SHALL reject the client record
- **AND** SHALL NOT store any of its associations

#### Scenario: Device quantity is validated

- **WHEN** a device association has a quantity outside 1–9999
- **THEN** the server SHALL reject the client record

#### Scenario: Treatment has no quantity

- **WHEN** a treatment association carries a quantity
- **THEN** the server SHALL reject the client record

#### Scenario: Duplicate association is rejected

- **WHEN** a client push repeats a catalog item
- **THEN** the server SHALL reject the client record

#### Scenario: Absent associations are preserved

- **WHEN** a client push omits the associations field
- **THEN** the server SHALL preserve the client's stored associations

#### Scenario: Empty associations clear the set

- **WHEN** a client push supplies an empty association set
- **THEN** the server SHALL remove the client's associations
- **AND** SHALL keep the client

#### Scenario: Client summary excludes associations

- **WHEN** the server returns the client list
- **THEN** each summary SHALL NOT include its associations
- **AND** SHALL remain sufficient to list clients

#### Scenario: Deleting a client deletes its associations

- **WHEN** a client is deleted
- **THEN** the server SHALL delete its associations
- **AND** SHALL keep the catalog items

### Requirement: Catalog and service history

The server SHALL record catalog changes and client service changes in the existing history. Catalog entries SHALL identify the item, its category and the action. Client service entries SHALL identify the client and the items added, removed or changed, with the item names captured at that time. The global history SHALL retain a single chronological order.

#### Scenario: Catalog change is recorded

- **WHEN** a catalog item is created, renamed, deactivated or reactivated
- **THEN** the server SHALL record an entry with the item name, category and action

#### Scenario: Client service change is recorded

- **WHEN** a client push changes its associations
- **THEN** the server SHALL record the change in that client's history
- **AND** the entry SHALL identify the added, removed and quantity-changed items

#### Scenario: Client history excludes catalog entries

- **WHEN** a client's history is requested
- **THEN** the server SHALL return only that client's entries
- **AND** SHALL NOT include unrelated catalog entries

#### Scenario: Global history mixes both kinds

- **WHEN** the global history is requested
- **THEN** the server SHALL return client and catalog entries in one chronological order
- **AND** SHALL allow paging through them with the existing cursor

#### Scenario: Old history remains readable

- **WHEN** the history contains entries recorded before catalog support
- **THEN** the server SHALL return them with their existing content
- **AND** SHALL identify them as client entries

### Requirement: Catalog migration preserves existing data

An existing server database SHALL be migrated in place to add catalog, association and history support. Every existing client, photo, tombstone and history entry SHALL be preserved.

#### Scenario: Existing database is upgraded

- **WHEN** the server starts with a database created before this feature
- **THEN** the schema SHALL be migrated
- **AND** every existing client, photo, tombstone and history entry SHALL remain readable

#### Scenario: Migration is repeatable

- **WHEN** the server starts again after the migration
- **THEN** the migration SHALL NOT run destructively
- **AND** the stored data SHALL remain unchanged
