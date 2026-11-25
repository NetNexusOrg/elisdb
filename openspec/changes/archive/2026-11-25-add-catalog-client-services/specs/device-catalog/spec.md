# Spec Delta

## Purpose

Provides a shared, synchronized catalog of device models and treatments that technicians can associate with clients, organized by pest category.

## ADDED Requirements

### Requirement: Catalog item model

The system SHALL store each catalog item with a stable identifier, a category, a display name, an active flag, and creation/modification timestamps. The four categories SHALL be Roedores, Insetos Rastejantes, Insetos Voadores and Tratamentos. The category SHALL be immutable after creation.

#### Scenario: Create a catalog item

- **WHEN** a technician saves a new item with a name and one of the four categories
- **THEN** the item SHALL be stored with an active flag and timestamps
- **AND** it SHALL appear in the catalog under its category

#### Scenario: Reject an invalid category

- **WHEN** an item is submitted with a category outside the four supported categories
- **THEN** the system SHALL reject the item
- **AND** SHALL NOT store it

#### Scenario: Name limits

- **WHEN** an item name is empty or longer than 80 characters
- **THEN** the system SHALL reject the item
- **AND** SHALL inform the technician that the name is invalid

#### Scenario: Category cannot be changed

- **WHEN** a technician edits an existing item
- **THEN** the category SHALL be presented as fixed
- **AND** the saved item SHALL keep its original category

### Requirement: Catalog lifecycle

Catalog items SHALL only be deactivated or reactivated; they SHALL never be hard-deleted. Inactive items SHALL remain stored and SHALL still be shown for clients where they are already installed, but SHALL NOT be offered for new associations.

#### Scenario: Deactivate an item

- **WHEN** a technician deactivates an item
- **THEN** the item SHALL remain stored as inactive
- **AND** it SHALL stop being offered when adding items to a client

#### Scenario: Reactivate an item

- **WHEN** a technician reactivates an inactive item
- **THEN** the item SHALL become available again for new client associations

#### Scenario: Inactive item already installed

- **WHEN** a client has an item that is later deactivated
- **THEN** the item SHALL remain visible in that client's detail
- **AND** its quantity SHALL still count towards the client's totals

#### Scenario: Any authenticated technician manages the catalog

- **WHEN** an authenticated technician opens the catalog
- **THEN** the technician SHALL be able to create, edit, deactivate and reactivate items
- **AND** no additional administrator role SHALL be required

### Requirement: Unique catalog names per category

The system SHALL reject a new or edited item whose name duplicates another item in the same category after normalization. Normalization SHALL trim surrounding whitespace, collapse repeated whitespace, ignore letter case and ignore diacritics. Items in different categories MAY share a name.

#### Scenario: Duplicate name is blocked

- **WHEN** a technician saves an item whose normalized name already exists in the same category
- **THEN** the system SHALL reject the item
- **AND** SHALL show that an item with that name already exists in the category

#### Scenario: Accent and case variants are duplicates

- **WHEN** "Ratoeira" exists and a technician tries to add "  ratoeira " or "RATÓEIRA" in the same category
- **THEN** the system SHALL treat the name as a duplicate and reject it

#### Scenario: Same name in another category is allowed

- **WHEN** a technician adds a name that exists only in a different category
- **THEN** the item SHALL be accepted

### Requirement: Catalog screen

The system SHALL provide a Catálogo screen reachable from the client list menu. It SHALL list every item grouped by the four categories in fixed order, with categories always expanded and items sorted alphabetically ignoring case and diacritics. Each row SHALL show the item name and an actions menu; inactive rows SHALL carry a discreet "Inativo" label. The top bar SHALL offer an add action.

#### Scenario: Open the catalog

- **WHEN** the technician chooses Catálogo in the client list menu
- **THEN** the system SHALL open the Catálogo screen
- **AND** the client list menu SHALL place Catálogo between Logs and Configuração

#### Scenario: Categories are always expanded

- **WHEN** the Catálogo screen is shown
- **THEN** the four categories SHALL be visible in the order Roedores, Insetos Rastejantes, Insetos Voadores, Tratamentos
- **AND** each category's items SHALL be listed without an expand/collapse step

#### Scenario: Items are ordered alphabetically

- **WHEN** a category contains several items
- **THEN** its rows SHALL be ordered alphabetically by name
- **AND** the ordering SHALL ignore letter case and diacritics

#### Scenario: Inactive items are identifiable

- **WHEN** the catalog contains an inactive item
- **THEN** its row SHALL show an "Inativo" label
- **AND** its actions menu SHALL offer reactivation

#### Scenario: Empty catalog

- **WHEN** no catalog items exist
- **THEN** the screen SHALL show "O catálogo está vazio. Adicione o primeiro modelo."

#### Scenario: Create an item from the screen

- **WHEN** the technician taps the add action
- **THEN** a dialog SHALL collect the name and category
- **AND** saving SHALL add the item to the catalog

#### Scenario: Edit an item from its actions menu

- **WHEN** the technician chooses Editar in an item's actions menu
- **THEN** a dialog SHALL collect the name with the category fixed
- **AND** saving SHALL update the item's name

#### Scenario: Deactivate from the actions menu

- **WHEN** the technician chooses Desativar in an active item's actions menu
- **THEN** the item SHALL be marked inactive
- **AND** the row SHALL show the "Inativo" label

### Requirement: Offline-first catalog management

Catalog items SHALL be persisted locally and SHALL be usable without connectivity. Creating, editing, deactivating and reactivating an item SHALL take effect locally immediately and SHALL be marked pending until the server acknowledges it. A failed sync SHALL NOT discard or roll back the local change.

#### Scenario: Create an item offline

- **WHEN** the technician creates an item with no connectivity
- **THEN** the item SHALL be stored locally and shown in the catalog
- **AND** it SHALL be marked as pending synchronization

#### Scenario: Pending item is not overwritten

- **WHEN** a catalog pull returns an item that is pending locally
- **THEN** the local pending values SHALL be preserved

#### Scenario: Sync failure keeps local changes

- **WHEN** catalog synchronization fails
- **THEN** the local catalog SHALL remain available
- **AND** the pending changes SHALL remain queued

### Requirement: Catalog synchronization protocol

The system SHALL synchronize catalog items through a dedicated pull and a bounded push. The server SHALL return every item, including inactive ones, with a completeness indicator. Push results SHALL be reported per item as accepted, unchanged, stale or invalid, and one invalid item SHALL NOT abort the rest of the batch.

#### Scenario: Pull the catalog

- **WHEN** the app pulls the catalog
- **THEN** it SHALL receive every item, active and inactive
- **AND** it SHALL know that the returned catalog is complete

#### Scenario: Push local catalog changes

- **WHEN** the app pushes pending catalog items
- **THEN** the server SHALL acknowledge each item individually
- **AND** accepted items SHALL stop being pending locally

#### Scenario: Last-write-wins

- **WHEN** a pushed item has an older modification timestamp than the stored item
- **THEN** the server SHALL report it as stale
- **AND** the app SHALL adopt the server's current item when the local item has not changed again

#### Scenario: Invalid item does not block the batch

- **WHEN** a push batch contains an invalid item, such as a duplicate name
- **THEN** the server SHALL report that item as invalid with a reason
- **AND** SHALL still process the remaining items

#### Scenario: Retries are safe

- **WHEN** the app retries a catalog push after a lost response
- **THEN** the server SHALL NOT duplicate the item
- **AND** SHALL return the same outcome for the same content

### Requirement: Catalog compatibility with older servers

A new app SHALL keep working when the server does not provide catalog endpoints. A missing catalog endpoint SHALL be treated as an unsupported capability, SHALL keep the local catalog available, and SHALL NOT prevent client synchronization. Any other catalog sync failure SHALL NOT delete local items.

#### Scenario: Old server without catalog endpoints

- **WHEN** the catalog pull or push receives a not-found response
- **THEN** the app SHALL keep the local catalog
- **AND** SHALL show that the catalog is not supported by the server
- **AND** SHALL continue synchronizing clients

#### Scenario: Other catalog failure does not lose data

- **WHEN** the catalog sync fails for a reason other than an unsupported endpoint
- **THEN** the local items SHALL remain stored
- **AND** the failure SHALL be surfaced as a sync warning

### Requirement: Catalog changes in the global activity log

The system SHALL record catalog creation, rename, deactivation and reactivation in the global activity log. A catalog entry SHALL identify the item, the category, the action, the actor and the timestamp, and SHALL NOT navigate to a client.

#### Scenario: Catalog entry is shown

- **WHEN** the global activity log contains a catalog change
- **THEN** the entry SHALL show the item name and the action
- **AND** it SHALL show the actor and the timestamp
- **AND** tapping it SHALL NOT navigate anywhere

#### Scenario: Old client entries remain unchanged

- **WHEN** the global activity log shows entries recorded before catalog support
- **THEN** those entries SHALL keep their existing content and navigation
