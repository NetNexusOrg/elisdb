## MODIFIED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name and a thumbnail: the client's first photo when present, otherwise a generic profile image. The system SHALL allow filtering the list by a search term. The screen SHALL have a top bar that shows a title for the screen and SHALL host the create-client action and the settings action in that top bar.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Generic image for clients without a photo
- **WHEN** the client list screen is shown and a client has no photo
- **THEN** the row SHALL display a generic profile image in place of the photo thumbnail

#### Scenario: Open a client from the list
- **WHEN** the user taps a client row
- **THEN** the system SHALL open that client's detail screen
