## ADDED Requirements

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