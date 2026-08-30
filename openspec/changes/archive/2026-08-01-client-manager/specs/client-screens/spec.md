## Purpose

Provides the user-facing Compose screens for managing clients: a list of clients, a create/edit form, a detail view, and fullscreen photo viewing, with navigation between them.

## ADDED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name and, when present, a thumbnail of the client's first photo.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Open a client from the list
- **WHEN** the user taps a client row
- **THEN** the system SHALL open that client's detail screen

#### Scenario: Empty list
- **WHEN** the client list screen is shown and no clients exist
- **THEN** the system SHALL indicate that no clients are stored yet

### Requirement: Add a new client

The system SHALL provide an entry point to create a new client from the list screen.

#### Scenario: Start creating a client
- **WHEN** the user taps the add action on the list screen
- **THEN** the system SHALL open the create/edit form in create mode

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), phone (optional), location selection, notes, and a photo management section, plus a save action.

#### Scenario: Required name validation
- **WHEN** the user tries to save with an empty name
- **THEN** the save SHALL be prevented and the name field SHALL be flagged as required

#### Scenario: Save a new client
- **WHEN** the user fills valid data and taps save in create mode
- **THEN** a new client SHALL be created
- **AND** the user SHALL return to the client list

#### Scenario: Save edits to a client
- **WHEN** the user edits a client and taps save in edit mode
- **THEN** the client SHALL be updated
- **AND** the user SHALL return to the client detail screen

### Requirement: Client detail screen

The system SHALL show a client's name, phone, notes, a map with the client's location, a photo gallery, and actions to navigate to the location, edit the client, and delete the client.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, phone, notes, map, and photo gallery SHALL be displayed

#### Scenario: Navigate to the client location
- **WHEN** the user taps "navigate" on the detail screen
- **THEN** the system SHALL launch an external navigation app to the client's location

#### Scenario: Edit from the detail screen
- **WHEN** the user taps edit on the detail screen
- **THEN** the system SHALL open the create/edit form in edit mode for that client

#### Scenario: Delete from the detail screen
- **WHEN** the user taps delete on the detail screen and confirms
- **THEN** the client SHALL be deleted
- **AND** the user SHALL return to the client list

### Requirement: Fullscreen photo viewer

The system SHALL display a selected photo fullscreen.

#### Scenario: View a photo fullscreen
- **WHEN** the user taps a photo thumbnail in a gallery
- **THEN** the photo SHALL be displayed fullscreen

#### Scenario: Close the fullscreen viewer
- **WHEN** the user closes the fullscreen photo viewer
- **THEN** the system SHALL return to the previous screen
