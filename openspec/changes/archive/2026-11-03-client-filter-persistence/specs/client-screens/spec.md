# Spec Delta

## ADDED Requirements

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
