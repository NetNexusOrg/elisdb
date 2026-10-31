# Spec Delta

## MODIFIED Requirements

### Requirement: About screen

The system SHALL provide an about screen, reached from the client list menu, that shows information about the app and about the team behind it. The app information SHALL include the app icon, the app name and the running app version. The team information SHALL include each member's name, role and contact email address, and each contact email SHALL be actionable (opening an external email composer). The about screen SHALL NOT be merged into any other screen.

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

#### Scenario: Developer information is shown
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL show each team member's name
- **AND** the screen SHALL show each team member's contact email address
- **AND** the screen SHALL show each team member's role

#### Scenario: Contact email is actionable
- **WHEN** the user taps a team member's contact email
- **THEN** the system SHALL open an external email composer addressed to that email
- **AND** a failure to open an email app SHALL NOT crash the app
