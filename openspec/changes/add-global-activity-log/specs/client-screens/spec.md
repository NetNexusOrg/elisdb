# Spec Delta

## MODIFIED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name, the contract reference when present, all the client's tags when present, and a thumbnail: the client's first photo when present, otherwise a generic profile image. Inactive clients SHALL be labelled. The system SHALL allow filtering the list by a search term, by status and by tag. The screen SHALL have a top bar with a title and a menu holding the create-client, synchronization, server, activity-log and about actions.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Contract reference under the name
- **WHEN** a listed client has a contract reference
- **THEN** the row SHALL display the contract reference below the client's name
- **AND** rows for clients without a contract SHALL NOT leave an empty line

#### Scenario: Tag shown on the contract line, aligned right
- **WHEN** a listed client has one or more tags
- **THEN** the row SHALL display every tag
- **AND** when the tags do not fit on one line they SHALL wrap to the following lines, aligned to the right side of the card
- **AND** rows for clients without tags SHALL NOT leave an empty placeholder

#### Scenario: Generic image for clients without a photo
- **WHEN** the client list screen is shown and a client has no photo
- **THEN** the row SHALL display a generic profile image in place of the photo thumbnail

#### Scenario: Inactive clients are identifiable in the list
- **WHEN** the client list screen shows a client whose status is inactive
- **THEN** the row SHALL carry a visible "inactive" label
- **AND** the row SHALL be visually dimmed compared with active clients

#### Scenario: Open a client from the list
- **WHEN** the user taps a client row
- **THEN** the system SHALL open that client's detail screen

#### Scenario: Empty list
- **WHEN** the client list screen is shown and no clients exist
- **THEN** the system SHALL indicate that no clients are stored yet

#### Scenario: Filter the list by a search term
- **WHEN** the user types a search term in the search field
- **THEN** the list SHALL show only clients whose name, contract or notes contain the term
- **AND** the comparison SHALL ignore letter case and diacritics

#### Scenario: Filter the list by status
- **WHEN** the user opens the status filter from the search field and chooses active or inactive
- **THEN** the list SHALL show only clients with that status
- **AND** the filter control SHALL indicate that a filter is applied
- **AND** the default selection SHALL show every client regardless of status

#### Scenario: Filter the list by tag
- **WHEN** the user opens the filter menu and chooses one of the available tags
- **THEN** the list SHALL show only clients whose set of tags includes the chosen tag
- **AND** the menu SHALL mark the chosen tag as selected
- **AND** the filter control SHALL indicate that a filter is applied

#### Scenario: Available tags come from the stored clients
- **WHEN** the user opens the filter menu
- **THEN** it SHALL list the distinct tags present in the stored clients, in uppercase and sorted
- **AND** clients without tags SHALL NOT add an option

#### Scenario: Filter groups are collapsible
- **WHEN** the user opens the filter menu
- **THEN** the status, tag and sort groups SHALL be presented as collapsed headers
- **AND** choosing a header SHALL reveal its options and collapse it again when chosen a second time

#### Scenario: Clear the tag filter
- **WHEN** the user chooses "Todas as tags" in the filter menu
- **THEN** the list SHALL show clients with any tag again

#### Scenario: Tag filter matches no client
- **WHEN** the chosen tag filter matches no client
- **THEN** the system SHALL indicate that no clients correspond to the filter

#### Scenario: Search matches no client
- **WHEN** the search term does not match any client
- **THEN** the system SHALL indicate that no clients were found

#### Scenario: Clear the search
- **WHEN** the user clears the search field
- **THEN** the full list of clients SHALL be displayed again

#### Scenario: Top bar hosts the screen actions
- **WHEN** the client list screen is shown
- **THEN** the screen SHALL show a top bar with a title for the screen
- **AND** the top bar SHALL host a menu that contains the add-a-new-client action
- **AND** the top bar SHALL host a menu that contains the action to open the synchronization screen
- **AND** the top bar SHALL host a menu that contains the action to open the server screen
- **AND** the top bar SHALL host a menu that contains the action to open the activity log
- **AND** the top bar SHALL host a menu that contains the action to open the about screen
- **AND** the create-client action SHALL NOT be shown as a floating action button

#### Scenario: Show current sync status
- **WHEN** the client list screen is shown and a sync is in progress
- **THEN** the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just failed, the screen SHALL indicate a sync error
- **AND** when a sync has just succeeded, the screen SHALL indicate the last successful sync

#### Scenario: No sync activity shown when idle
- **WHEN** the client list screen is shown and no sync has run or been triggered
- **THEN** the screen SHALL NOT show a "not synchronised" message for a missing server configuration

#### Scenario: Open the activity log from the menu
- **WHEN** the user opens the menu in the top bar and taps the activity-log action
- **THEN** the system SHALL open the global activity log screen

## ADDED Requirements

### Requirement: Global activity log order and entry point

The system SHALL provide a global activity log screen, reachable from the client list menu, that shows every recorded client change from newest to oldest, regardless of which client it belongs to.

#### Scenario: Activity log opens from the list menu
- **WHEN** the user taps the activity-log action in the client list menu
- **THEN** the system SHALL open the global activity log screen

#### Scenario: Entries are presented newest first
- **WHEN** the activity log contains changes recorded at different times
- **THEN** the most recent change SHALL appear first
- **AND** the oldest change SHALL appear last

#### Scenario: Only confirmed changes are shown
- **WHEN** a client has local changes not yet acknowledged by the server
- **THEN** those changes SHALL NOT appear in the activity log until they are synchronized

### Requirement: Activity log entry content and navigation

Each activity log entry SHALL show the affected client, the action performed, the actor when known, the timestamp, and the recorded change details. The view SHALL present entry timestamps with a 24-hour clock and SHALL present tags in uppercase.

#### Scenario: Entry identifies the client and the action
- **WHEN** an activity log entry is shown
- **THEN** it SHALL show the client's name
- **AND** it SHALL state whether the client was created, edited or deleted
- **AND** it SHALL show the actor and the timestamp

#### Scenario: Entry shows the changed fields
- **WHEN** an entry carries change details with changed fields
- **THEN** the entry SHALL show each changed field's previous and new value
- **AND** tag changes SHALL be shown with the values in uppercase

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

#### Scenario: Deleted client is not navigable
- **WHEN** an entry refers to a client that no longer exists
- **THEN** the client name SHALL be shown from the deletion record
- **AND** tapping the entry SHALL NOT navigate anywhere

### Requirement: Activity log grouping, pagination and states

The activity log SHALL group entries under a date heading for each day, loading older entries on demand. It SHALL distinguish a fetch failure from genuinely empty history and SHALL offer a retry after a failure.

#### Scenario: Entries are grouped by day
- **WHEN** the activity log shows entries from more than one day
- **THEN** entries SHALL be grouped under a date heading for each day
- **AND** the most recent day SHALL appear first

#### Scenario: Load older entries
- **WHEN** the log has older entries than the ones currently shown
- **THEN** the screen SHALL offer a "load more" action
- **AND** choosing it SHALL append the next page without repeating entries

#### Scenario: All entries loaded
- **WHEN** no older entries remain
- **THEN** the screen SHALL NOT offer the "load more" action

#### Scenario: Empty activity log
- **WHEN** no audit entries exist
- **THEN** the screen SHALL indicate that no activity is recorded yet

#### Scenario: Fetch failure is reported
- **WHEN** the activity fetch fails (network, authentication, or server error)
- **THEN** the screen SHALL show that the activity could not be loaded, distinct from the empty state
- **AND** SHALL offer a retry action
