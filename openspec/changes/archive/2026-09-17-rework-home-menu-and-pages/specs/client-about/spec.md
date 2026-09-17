## Purpose

Provides a dedicated About screen, reachable from the home menu, that tells the user what the app is and who develops it: app name and version, a short description, and the developer's name and contact email.

## ADDED Requirements

### Requirement: About screen

The system SHALL provide an about screen, reached from the client list menu, that shows information about the app and about the developer. The app information SHALL include the app name, the running app version, and a short description of the app. The developer information SHALL include the developer's name and a contact email address, and the contact email SHALL be actionable (opening an external email composer). The about screen SHALL NOT be merged into any other screen.

#### Scenario: About screen is opened from the client list menu
- **WHEN** the user opens the client list menu and taps the about action
- **THEN** the system SHALL open the about screen
- **AND** no other screen SHALL be shown

#### Scenario: App information is shown
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL show the app name
- **AND** the screen SHALL show the version of the running app
- **AND** the screen SHALL show a short description of the app

#### Scenario: Version reflects the installed build
- **WHEN** the user opens the about screen
- **THEN** the version shown SHALL be the version of the installed build

#### Scenario: Developer information is shown
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL show the developer's name
- **AND** the screen SHALL show the developer's contact email address

#### Scenario: Contact email is actionable
- **WHEN** the user taps the developer's contact email
- **THEN** the system SHALL open an external email composer addressed to that email
- **AND** a failure to open an email app SHALL NOT crash the app
