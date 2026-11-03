# Spec Delta

## ADDED Requirements

### Requirement: Component changelogs

Each component SHALL keep its own `CHANGELOG.md` at the component root:
`elisdb-android/`, `elisdb-server/`, `elisdb-telegram-bot/` and
`elisdb-memento/`. The repository root SHALL NOT contain a changelog.

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
