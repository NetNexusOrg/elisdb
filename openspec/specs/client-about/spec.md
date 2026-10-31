## Purpose

Provides a dedicated About screen, reachable from the home menu, that tells the user what the app is and who develops it: app name and version, a short description, and the developer's name and contact email.

## Requirements

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

### Requirement: Community group access

The about screen SHALL show a community section with a QR code for the app's Telegram
group and an action that opens the invite link on the device. The QR code SHALL be
bundled with the app so it is shown without a network connection.

#### Scenario: Community invite is shown
- **WHEN** the user opens the about screen
- **THEN** the screen SHALL show a community section
- **AND** the section SHALL show a QR code that encodes the group invite link

#### Scenario: Open the invite on the device
- **WHEN** the user taps the action to open the community link
- **THEN** the system SHALL hand the invite link to an external app that can open it
- **AND** a device without such an app SHALL remain on the about screen without crashing

#### Scenario: Invite is available offline
- **WHEN** the device has no network connection and the user opens the about screen
- **THEN** the QR code SHALL still be shown
