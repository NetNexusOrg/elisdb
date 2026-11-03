# Spec Delta

## ADDED Requirements

### Requirement: Remember the client map filter

The system SHALL remember the last selected client map filter across app restarts. When no saved preference exists, the default SHALL be to show only active clients. The client list filter and search SHALL remain independent from the map filter.

#### Scenario: Restore the saved map filter
- **WHEN** the client map opens and a saved map filter exists
- **THEN** the map SHALL show only clients matching that filter
- **AND** the filter control SHALL indicate the active filter

#### Scenario: Persist a map filter change
- **WHEN** the user selects a map filter
- **THEN** the selection SHALL be saved for the next app launch
- **AND** the client list filter and search SHALL remain unchanged

#### Scenario: Default to active clients without a saved filter
- **WHEN** the client map opens and no saved map filter exists
- **THEN** the map SHALL show only active clients
