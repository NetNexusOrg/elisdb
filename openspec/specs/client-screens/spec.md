## Purpose

Provides the user-facing Compose screens for managing clients: a list of clients, a create/edit form, a detail view, and fullscreen photo viewing, with navigation between them.

## Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name, the contract reference when present, the tag when present, and a thumbnail: the client's first photo when present, otherwise a generic profile image. Inactive clients SHALL be labelled. The system SHALL allow filtering the list by a search term, by status and by tag. The screen SHALL have a top bar with a title and a menu holding the create-client, synchronization, server and about actions.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Contract reference under the name
- **WHEN** a listed client has a contract reference
- **THEN** the row SHALL display the contract reference below the client's name
- **AND** rows for clients without a contract SHALL NOT leave an empty line

#### Scenario: Tag shown on the contract line, aligned right
- **WHEN** a listed client has a tag
- **THEN** the row SHALL display the tag on the same line as the contract reference and the inactive label, aligned to the right side of the card
- **AND** rows for clients without a tag SHALL NOT leave an empty placeholder

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
- **THEN** the list SHALL show only clients whose tag matches the chosen tag
- **AND** the menu SHALL mark the chosen tag as selected
- **AND** the filter control SHALL indicate that a filter is applied

#### Scenario: Available tags come from the stored clients
- **WHEN** the user opens the filter menu
- **THEN** it SHALL list the distinct tags present in the stored clients, in uppercase and sorted
- **AND** clients without a tag SHALL NOT add an option

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

### Requirement: Add a new client

The system SHALL provide an entry point to create a new client from the menu in the top bar of the list screen.

#### Scenario: Start creating a client
- **WHEN** the user opens the menu in the top bar of the list screen and taps the add action
- **THEN** the system SHALL open the create/edit form in create mode

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), contract (optional), tag (optional), location selection (optional), notes, an active switch, and a photo management section, plus a save action. A form opened in create mode SHALL start with all fields empty, no location selected, and the active switch on. A form opened in edit mode SHALL load the data of the selected client only, regardless of which forms were opened previously.

#### Scenario: Required name validation
- **WHEN** the user tries to save with an empty name
- **THEN** the save SHALL be prevented and the name field SHALL be flagged as required

#### Scenario: Save a new client
- **WHEN** the user fills valid data and taps save in create mode
- **THEN** a new client SHALL be created
- **AND** the user SHALL return to the client list

#### Scenario: Save a new client without a location
- **WHEN** the user fills a name but no location and taps save in create mode
- **THEN** a new client SHALL be created without coordinates
- **AND** the user SHALL return to the client list

#### Scenario: Save a client with a tag
- **WHEN** the user fills a name and a tag and taps save in create mode
- **THEN** the new client SHALL be stored with that tag
- **AND** the user SHALL return to the client list

#### Scenario: Tag field is the fourth input, below the notes
- **WHEN** the create/edit client form is shown
- **THEN** the tag field SHALL be the fourth input field, placed below the notes

#### Scenario: Save edits to a client
- **WHEN** the user edits a client and taps save in edit mode
- **THEN** the client SHALL be updated
- **AND** the user SHALL return to the client detail screen

#### Scenario: Edit removes a client's location
- **WHEN** the user edits a client and clears its location before saving
- **THEN** the client SHALL be updated without coordinates
- **AND** the user SHALL return to the client detail screen

#### Scenario: Create form starts empty after another form was opened
- **WHEN** the user opens a create/edit form, leaves it, and opens the create form again
- **THEN** every field SHALL be empty, including the tag
- **AND** no location SHALL be selected
- **AND** the active switch SHALL be on

#### Scenario: Edit form loads only the selected client
- **WHEN** the user opens the edit form for a client after having previously opened a form for a different client
- **THEN** the form SHALL show the name, contract, tag, notes, location and status of the selected client only

#### Scenario: Toggle the client status
- **WHEN** the user turns the active switch off and saves
- **THEN** the client SHALL be stored as inactive
- **AND** when the switch is on the client SHALL be stored as active

### Requirement: Client detail screen

The system SHALL show a client's name, tag, notes, a map with the client's location when one exists, a photo gallery, an "inactive" label when the client is inactive, and actions to navigate to the location, edit the client, and delete the client. When the client has a contract reference, the top bar SHALL display it next to the back button; the body SHALL NOT show a contract line above the name. The body sections SHALL be presented in the following order: name, tag (when present), map (when the location exists), notes, photo gallery. When the client has no location, the map and the navigate action SHALL NOT be shown. The edit and delete actions SHALL be offered through an overflow menu in the top bar, and the top bar SHALL NOT repeat the client's name.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, notes, map, and photo gallery SHALL be displayed

#### Scenario: Tag shown below the name
- **WHEN** the user opens the detail screen of a client with a tag
- **THEN** the tag SHALL be displayed as a label below the client's name

#### Scenario: No tag on the client
- **WHEN** the user opens the detail screen of a client without a tag
- **THEN** no tag label SHALL be displayed

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
- **WHEN** a client detail screen is shown for a client with coordinates and a tag
- **THEN** the body sections SHALL appear in this order: name, tag, map, notes, photo gallery

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

### Requirement: Fullscreen photo viewer

The system SHALL display a selected photo fullscreen. When the client has more than one photo, the user SHALL be able to switch to the previous or next photo by swiping horizontally. The user SHALL be able to zoom the displayed photo in or out with a pinch gesture, and SHALL be able to pan the photo by dragging it when it is zoomed in.

#### Scenario: View a photo fullscreen
- **WHEN** the user taps a photo thumbnail in a gallery
- **THEN** the photo SHALL be displayed fullscreen

#### Scenario: Close the fullscreen viewer
- **WHEN** the user closes the fullscreen photo viewer
- **THEN** the system SHALL return to the previous screen

#### Scenario: Swipe to the next photo
- **WHEN** the client has more than one photo and the user swipes left on the fullscreen photo viewer
- **THEN** the next photo SHALL be displayed
- **AND** when the current photo is the last one, swiping left SHALL keep the last photo displayed

#### Scenario: Swipe to the previous photo
- **WHEN** the client has more than one photo and the user swipes right on the fullscreen photo viewer
- **THEN** the previous photo SHALL be displayed
- **AND** when the current photo is the first one, swiping right SHALL keep the first photo displayed

#### Scenario: Pinch to zoom in
- **WHEN** the user performs a pinch-out gesture on the displayed photo
- **THEN** the photo SHALL zoom in

#### Scenario: Pinch to zoom out
- **WHEN** the user performs a pinch-in gesture on a zoomed photo
- **THEN** the photo SHALL zoom out

#### Scenario: Pan a zoomed photo
- **WHEN** the photo is zoomed in and the user drags on it
- **THEN** the photo SHALL pan to follow the drag
- **AND** the photo SHALL NOT move beyond its edges

#### Scenario: Zoom resets when changing photo
- **WHEN** the user zooms a photo and then switches to another photo
- **THEN** the new photo SHALL be displayed without the previous zoom level applied

### Requirement: Client name presented in uppercase

The system SHALL present the client's name in uppercase letters in the client list, the detail screen and the create/edit form, regardless of the casing the user types.

#### Scenario: Name uppercased while typing
- **WHEN** the user types a client name in the create/edit form using lowercase or mixed-case letters
- **THEN** the name field SHALL display the entered text in uppercase letters as it is typed

#### Scenario: Name shown uppercased in the list
- **WHEN** the client list screen is shown
- **THEN** each client's name SHALL be displayed in uppercase letters

#### Scenario: Name shown uppercased in the detail screen
- **WHEN** a client detail screen is shown
- **THEN** the client's name SHALL be displayed in uppercase letters

### Requirement: Log action on the client detail screen

The system SHALL offer a "Log" action on the client detail screen that shows who created the client and who edited it, when, and what changed: the fields whose values differ (previous and new value) and the photos added, removed or reordered. Entry timestamps SHALL be presented with a 24-hour clock regardless of the device locale. The view SHALL distinguish a fetch failure from genuinely empty history, SHALL note when a client has local pending changes whose history will only appear after synchronization, and SHALL still present entries recorded without change details.

#### Scenario: Log action opens the history view
- **WHEN** the user taps "Log" on a client detail screen
- **THEN** the app SHALL show the client's edit history: the create entry and the edit entries
- **AND** each entry SHALL show the actor (user name when known) and the timestamp

#### Scenario: Log is reachable from the detail screen
- **WHEN** the client detail screen is shown
- **THEN** a "Log" action SHALL be available to the user

#### Scenario: Empty history is presented clearly
- **WHEN** a client has no recorded history (e.g. migrated data)
- **THEN** the log view SHALL indicate that no history is available

#### Scenario: Entry timestamps use a 24-hour clock
- **WHEN** the log view shows an entry timestamp on a device whose locale would use a 12-hour clock
- **THEN** the time SHALL be presented in 24-hour format (no AM/PM marker)

#### Scenario: History fetch failure is reported
- **WHEN** the history fetch fails (network, authentication, or server error)
- **THEN** the log view SHALL show that the history could not be loaded, distinct from the "no history" state

#### Scenario: Pending client notes history after sync
- **WHEN** a client has local pending changes (created or edited but not yet acknowledged by the server) and the log has no server entries
- **THEN** the log view SHALL indicate that the history will appear after synchronization, rather than only saying no history is available

#### Scenario: Entry lists the changed fields
- **WHEN** an entry carries change details with changed fields
- **THEN** the log view SHALL show each changed field's name, its previous value and its new value
- **AND** SHALL still show the actor and the timestamp of the entry

#### Scenario: Entry summarizes photo changes
- **WHEN** an entry carries photo changes
- **THEN** the log view SHALL state how many photos were added and removed and whether the photos were reordered
- **AND** SHALL show a thumbnail for each added photo that still exists on the device

#### Scenario: Entry without details keeps the actor-only view
- **WHEN** an entry was recorded without change details
- **THEN** the log view SHALL present it with the actor and the timestamp, as before
- **AND** SHALL NOT show an empty or broken change section

#### Scenario: Entries are grouped by day
- **WHEN** the log page shows entries from more than one day
- **THEN** entries SHALL be grouped under a date heading for each day, in chronological order

### Requirement: Client status changes appear in the log

The system SHALL show an entry in the client's log when a client is activated or deactivated, so the change of status is explained alongside the other edits.

#### Scenario: Deactivation is logged
- **WHEN** a client with recorded history is saved as inactive
- **THEN** the log view SHALL show that the client was deactivated
- **AND** the entry SHALL show the actor and the timestamp like the other edits

#### Scenario: Activation is logged
- **WHEN** a client with recorded history is saved as active after having been inactive
- **THEN** the log view SHALL show that the client was activated

### Requirement: Client list ordering

The client list SHALL offer an alphabetical ordering, a most-recent-first
ordering and a last-edited-first ordering, selectable from the same menu as the
status filter. The alphabetical ordering SHALL be the default. Ordering SHALL be
applied after the status filter and the search term, and SHALL NOT change the
stored data.

#### Scenario: Alphabetical order by default
- **WHEN** the client list is shown without any ordering choice
- **THEN** the clients SHALL be listed in alphabetical order by name

#### Scenario: Most recent first
- **WHEN** the user selects the most-recent-first ordering
- **THEN** the clients SHALL be listed with the most recently created client first
- **AND** clients created at the same time SHALL be ordered alphabetically

#### Scenario: Most recently edited first
- **WHEN** the user selects the last-edited-first ordering
- **THEN** the clients SHALL be listed with the most recently modified client first
- **AND** clients modified at the same time SHALL be ordered by creation date and then alphabetically

#### Scenario: Ordering respects the active filter and search
- **WHEN** the user changes the ordering while a status filter or a search term is active
- **THEN** the list SHALL keep only the matching clients
- **AND** SHALL present them in the chosen order

#### Scenario: Ordering options are reachable from the filter menu
- **WHEN** the user opens the filter menu
- **THEN** the menu SHALL show the status options and the ordering options
- **AND** the current ordering SHALL be marked as selected

### Requirement: Remember the client list filter

The system SHALL remember the last selected client status filter across app restarts. When no saved preference exists, the default SHALL be to show all clients.

#### Scenario: Restore the saved status filter
- **WHEN** the client list screen opens and a saved status filter exists
- **THEN** the list SHALL show only clients with that status
- **AND** the filter control SHALL indicate the active filter

#### Scenario: Persist a status filter change
- **WHEN** the user selects a status filter
- **THEN** the selection SHALL be saved for the next app launch

#### Scenario: Default to all clients without a saved filter
- **WHEN** the client list screen opens and no saved status filter exists
- **THEN** the list SHALL show all clients

### Requirement: Remember the client list sort order

The system SHALL remember the last selected client list sort order across app restarts. When no saved preference exists, the default SHALL be alphabetical order.

#### Scenario: Restore the saved sort order
- **WHEN** the client list screen opens and a saved sort order exists
- **THEN** the list SHALL use that sort order

#### Scenario: Persist a sort order change
- **WHEN** the user selects a different sort order
- **THEN** the list SHALL be reordered
- **AND** the selection SHALL be saved for the next app launch

#### Scenario: Default to alphabetical order without a saved preference
- **WHEN** the client list screen opens and no saved sort order exists
- **THEN** the list SHALL be ordered alphabetically by client name

### Requirement: Client tag presented in uppercase

The system SHALL present the client tag in uppercase letters in the client list, the detail screen, the create/edit form and the edit history, regardless of the casing stored or typed.

#### Scenario: Tag uppercased while typing
- **WHEN** the user types a tag in the create/edit form using lowercase or mixed-case letters
- **THEN** the tag field SHALL display the entered text in uppercase letters as it is typed

#### Scenario: Tag shown uppercased in the list
- **WHEN** the client list screen is shown and a client has a tag
- **THEN** the tag SHALL be displayed in uppercase letters

#### Scenario: Tag shown uppercased in the detail screen
- **WHEN** a client detail screen is shown and the client has a tag
- **THEN** the tag SHALL be displayed in uppercase letters

#### Scenario: Historical tag values shown uppercased in the log
- **WHEN** the edit history shows a changed tag value
- **THEN** the previous and new tag values SHALL be displayed in uppercase letters
