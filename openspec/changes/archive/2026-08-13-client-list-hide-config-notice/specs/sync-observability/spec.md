## MODIFIED Requirements

### Requirement: Sync observable from the client list

The system SHALL expose the sync status and a manual sync trigger from the main client list screen.

#### Scenario: Status visible on main screen
- **WHEN** the user views the client list
- **THEN** the current sync state SHALL be displayed on that screen
- **AND** a manual sync action SHALL be available there
- **AND** any sync error SHALL be communicated on that screen with an actionable message

#### Scenario: Configuration notice kept out of the list
- **WHEN** the server is not configured
- **THEN** the client list SHALL NOT display a server-configuration notice
- **AND** a notice telling the user to configure the server SHALL be available in the Settings screen instead

#### Scenario: Real sync errors still actionable
- **WHEN** a sync attempt fails due to network, authentication or clock issues
- **THEN** the client list SHALL display the specific failure
- **AND** SHALL keep the manual sync action available