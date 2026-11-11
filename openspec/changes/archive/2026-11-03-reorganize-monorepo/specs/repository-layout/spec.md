# Spec Delta

## Purpose

Defines the monorepo layout by component so each component owns its files and
the repository root only keeps cross-cutting documentation, CI and tooling.

## ADDED Requirements

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

#### Scenario: Bot files stay in their component

- **WHEN** a file belongs to the Telegram bot
- **THEN** it SHALL live under `elisdb-telegram-bot/`

### Requirement: Root only holds cross-cutting files

The repository root SHALL contain only cross-cutting documentation, CI, and
developer tooling, including `AGENTS.md`, `README.md`, `opencode.json`,
`openspec/`, `.github/` and `.opencode/`. Component-specific files SHALL NOT be
kept at the root.

#### Scenario: Root layout is cross-cutting only

- **WHEN** the repository root is listed
- **THEN** it SHALL NOT contain component-specific files or directories such as
  a root `CHANGELOG.md`, `scripts/` or `photos/`

### Requirement: Android changelog location

The Android release notes SHALL live at `elisdb-android/CHANGELOG.md`, and the
release automation SHALL read that file when extracting the notes for a tag.

#### Scenario: Release notes are read from the Android component

- **WHEN** a `v*` tag is pushed
- **THEN** the release job SHALL extract the `## [X.Y.Z]` section from
  `elisdb-android/CHANGELOG.md`
- **AND** SHALL fail before building when the section is missing

### Requirement: Server deploy helper location

The production deploy helper SHALL live at
`elisdb-server/scripts/deploy-server.sh` and SHALL resolve the repository root
from its own location, so it works regardless of the caller's current directory.

#### Scenario: Deploy helper runs from its new location

- **WHEN** the deploy helper is invoked from the repository root or from
  `elisdb-server/`
- **THEN** it SHALL operate on the repository containing `elisdb-server/`
- **AND** `bash -n` SHALL accept the script

### Requirement: Build and runtime artifacts stay out of Git

Build outputs, local configuration, runtime databases, photos and secrets SHALL
remain excluded from version control, while source and documentation stay
tracked.

#### Scenario: Artifacts are not committed

- **WHEN** the repository status is inspected after a build or server run
- **THEN** build outputs, databases, local configuration, photo files and
  secrets SHALL NOT be staged for commit
