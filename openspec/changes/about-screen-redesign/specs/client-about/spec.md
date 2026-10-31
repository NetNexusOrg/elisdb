# Spec Delta

## REMOVED Requirements

### Requirement: About screen

**Reason**: The about screen no longer shows a developer/team card; it is reduced to
the app identity and the updates section, which is clearer and avoids stale contact
details.

**Migration**: Replaced by the "App information screen" requirement below, which
covers the same screen with the current content.

## ADDED Requirements

### Requirement: App information screen

The system SHALL provide an about screen, reached from the client list menu, that shows the app identity and the app update section. The identity SHALL include the app icon, the app name and the running app version, and SHALL NOT show a description or a developer/team card. The about screen SHALL NOT be merged into any other screen.

#### Scenario: About screen is opened from the client list menu
- **WHEN** the user opens the client list menu and taps the about action
- **THEN** the system SHALL open the about screen
- **AND** no other screen SHALL be shown

#### Scenario: App information is shown
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL show the app icon
- **AND** the screen SHALL show the app name
- **AND** the screen SHALL show the version of the running app
- **AND** the screen SHALL NOT show a description of the app

#### Scenario: Version reflects the installed build
- **WHEN** the user opens the about screen
- **THEN** the version shown SHALL be the version of the installed build

#### Scenario: No developer or team card
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL NOT show developer or team names
- **AND** the screen SHALL NOT show contact email addresses

#### Scenario: Updates are available from the about screen
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL offer the action to look for app updates
