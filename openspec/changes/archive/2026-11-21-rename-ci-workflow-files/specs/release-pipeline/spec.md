# Spec Delta

## ADDED Requirements

### Requirement: CI workflow file names

Each CI workflow file SHALL be named after the component it covers, so the file name and the workflow name displayed in GitHub Actions identify the same thing.

#### Scenario: Android workflow file name

- **WHEN** a maintainer opens `.github/workflows/`
- **THEN** the Android workflow SHALL be the file `android-ci.yml`
- **AND** GitHub Actions SHALL display it as `Android CI`

#### Scenario: Server workflow file name

- **WHEN** a maintainer opens `.github/workflows/`
- **THEN** the server workflow SHALL be the file `server-ci.yml`
- **AND** GitHub Actions SHALL display it as `Server CI`

### Requirement: CI job names

Each CI job SHALL be named so that its component and purpose are unambiguous in the pull request checks list, even when both workflows run at the same time.

#### Scenario: Android checks are identifiable

- **WHEN** a pull request targets main
- **THEN** the Android unit test, lint and debug build check SHALL be named `Android tests`
- **AND** the full instrumented check SHALL be named `Instrumented suite (non-blocking)`

#### Scenario: Server smoke check is identifiable inside the Android workflow

- **WHEN** a release tag is pushed
- **THEN** the server sync smoke check SHALL be named `Server sync smoke (release gate)`
