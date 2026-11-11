# Spec Delta — repository-layout

## MODIFIED Requirements

### Requirement: Component-owned files

Android-specific files SHALL live under `elisdb-android/`, server-specific
files under `elisdb-server/`, and Telegram bot files under
`elisdb-telegram-bot/`.

#### Scenario: Android files stay in the Android component

- **WHEN** a file is specific to the Android app, such as its changelog
- **THEN** it SHALL live under `elisdb-android/`

#### Scenario: Server files stay in the server component

- **WHEN** a file is specific to the sync server, such as its deploy helper
- **THEN** it SHALL live under `elisdb-server/`

#### Scenario: Bot and migration files stay in their components

- **WHEN** a file belongs to the Telegram bot
- **THEN** it SHALL live under `elisdb-telegram-bot/`

### Requirement: Component changelogs

Each component SHALL keep its own `CHANGELOG.md` at the component root:
`elisdb-android/`, `elisdb-server/` and `elisdb-telegram-bot/`. The repository
root SHALL NOT contain a changelog.

#### Scenario: Each component has a changelog

- **WHEN** a component directory is inspected
- **THEN** it SHALL contain a `CHANGELOG.md` at its root

#### Scenario: Android release notes keep their source

- **WHEN** a `v*` tag is pushed
- **THEN** the release job SHALL continue to extract the notes from
  `elisdb-android/CHANGELOG.md`
- **AND** the other component changelogs SHALL NOT be used for Android release
  notes

#### Scenario: No root changelog

- **WHEN** the repository root is listed
- **THEN** it SHALL NOT contain a `CHANGELOG.md`
