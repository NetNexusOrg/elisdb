# Spec Delta

## ADDED Requirements

### Requirement: Catálogo action in the client list menu

The client list top bar menu SHALL offer a Catálogo action between Logs and Configuração, opening the catalog screen.

#### Scenario: Open the catalog from the list menu

- **WHEN** the user opens the client list menu and taps Catálogo
- **THEN** the system SHALL open the Catálogo screen
- **AND** Catálogo SHALL appear between Logs and Configuração in the menu

## MODIFIED Requirements

### Requirement: Client detail screen
The system SHALL show name, tags, notes, installed devices and treatments, map when a location exists, photo gallery, an 'inactive' label, and actions to navigate, edit and delete. With a contract, the top bar SHALL show it next to back; the body SHALL NOT show a contract line. Order: name, tags, map, notes, services, gallery. Without a location, map and navigate SHALL NOT appear. Edit/delete SHALL be in the top-bar overflow menu, which SHALL NOT repeat the name.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, tags, notes, installed devices and treatments, map, and photo gallery SHALL be displayed

#### Scenario: Tag shown below the name
- **WHEN** the user opens the detail screen of a client with tags
- **THEN** every tag SHALL be displayed as a chip below the client's name

#### Scenario: No tag on the client
- **WHEN** the user opens the detail screen of a client without tags
- **THEN** no tag chip SHALL be displayed

#### Scenario: Client without a location shows no map
- **WHEN** the user opens a client detail screen for a client without coordinates
- **THEN** the map SHALL NOT be displayed
- **AND** the navigate action SHALL NOT be shown

#### Scenario: Top bar does not repeat the client name
- **WHEN** the user opens a client detail screen
- **THEN** the top bar SHALL NOT display the client's name

#### Scenario: Contract shown in the top bar
- **WHEN** a client with a contract reference is viewed
- **THEN** the contract reference SHALL be displayed in the top bar next to the back button
- **AND** no contract SHALL be displayed above the client's name in the body

#### Scenario: No contract on the client
- **WHEN** a client without a contract reference is viewed
- **THEN** the top bar SHALL NOT display a contract reference

#### Scenario: Inactive client is labelled
- **WHEN** the user opens the detail screen of an inactive client
- **THEN** the screen SHALL show an "inactive" label next to the client's name

#### Scenario: Sections are presented in order
- **WHEN** a client detail screen is shown for a client with coordinates and tags
- **THEN** the body sections SHALL appear in this order: name, tags, map, notes, devices and treatments, photo gallery

#### Scenario: Installed items are read-only
- **WHEN** the client detail screen shows installed devices and treatments
- **THEN** the technician SHALL NOT be able to change them from that screen
- **AND** SHALL use the edit action to change them

#### Scenario: Navigate to the client location
- **WHEN** the user taps "navigate" on the detail screen of a client with coordinates
- **THEN** the system SHALL launch an external navigation app to the client's location

#### Scenario: Edit from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps edit
- **THEN** the system SHALL open the create/edit form in edit mode for that client

#### Scenario: Delete from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps delete, and confirms
- **THEN** the client SHALL be deleted
- **AND** the user SHALL return to the client list

### Requirement: Global activity log order and entry point

The system SHALL provide a global activity log screen, reachable from the client list menu, that shows every recorded client and catalog change from newest to oldest, regardless of which client it belongs to.

#### Scenario: Activity log opens from the list menu
- **WHEN** the user taps the activity-log action in the client list menu
- **THEN** the system SHALL open the global activity log screen

#### Scenario: Entries are presented newest first
- **WHEN** the activity log contains changes recorded at different times
- **THEN** the most recent change SHALL appear first
- **AND** the oldest change SHALL appear last

#### Scenario: Catalog changes appear in the log
- **WHEN** a catalog item was created, renamed, deactivated or reactivated
- **THEN** the activity log SHALL include an entry for that change
- **AND** the entry SHALL be ordered with the client changes by timestamp

#### Scenario: Only confirmed changes are shown
- **WHEN** a client or catalog item has local changes not yet acknowledged by the server
- **THEN** those changes SHALL NOT appear in the activity log until they are synchronized

### Requirement: Activity log entry content and navigation

Each client activity log entry SHALL show the affected client, the action performed, the actor when known, the timestamp, and the recorded change details. Each catalog entry SHALL show the affected item, its category, the action, the actor and the timestamp. The view SHALL present entry timestamps with a 24-hour clock and SHALL present tags in uppercase.

#### Scenario: Entry identifies the client and the action
- **WHEN** an activity log entry for a client is shown
- **THEN** it SHALL show the client's name
- **AND** it SHALL state whether the client was created, edited or deleted
- **AND** it SHALL show the actor and the timestamp

#### Scenario: Catalog entry identifies the item and the action
- **WHEN** an activity log entry for a catalog item is shown
- **THEN** it SHALL show the item's name and category
- **AND** it SHALL state whether the item was created, renamed, deactivated or reactivated
- **AND** it SHALL show the actor and the timestamp

#### Scenario: Entry shows the changed fields
- **WHEN** a client entry carries change details with changed fields
- **THEN** the entry SHALL show each changed field's previous and new value
- **AND** tag changes SHALL be shown with the values in uppercase

#### Scenario: Entry summarizes installed-item changes
- **WHEN** a client entry carries installed-item changes
- **THEN** the entry SHALL list the items added and removed
- **AND** SHALL show quantity changes with their previous and new values

#### Scenario: Entry summarizes photo changes
- **WHEN** an entry carries photo changes
- **THEN** the entry SHALL state how many photos were added and removed and whether they were reordered
- **AND** SHALL show a thumbnail for each added photo that still exists on the device

#### Scenario: Timestamps use a 24-hour clock
- **WHEN** an entry is shown on a device whose locale would use a 12-hour clock
- **THEN** the time SHALL be presented in 24-hour format with no AM/PM marker

#### Scenario: Open an existing client from the log
- **WHEN** the user taps the client name in an entry for a client that still exists
- **THEN** the system SHALL open that client's detail screen

#### Scenario: Catalog entry is not navigable
- **WHEN** the user taps a catalog entry
- **THEN** the system SHALL NOT navigate anywhere

#### Scenario: Deleted client is not navigable
- **WHEN** an entry refers to a client that no longer exists
- **THEN** the client name SHALL be shown from the deletion record
- **AND** tapping the entry SHALL NOT navigate anywhere
