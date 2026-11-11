## Purpose

Automates the production of signed, reproducible Android release artifacts and their publication as GitHub Releases, in a private repository, driven by version tags and without exposing signing secrets or user data.

## Requirements

### Requirement: Version-controlled repository

The project SHALL be tracked in a single Git repository whose `.gitignore` excludes build artifacts, local machine configuration, and any file containing secrets or user data, while retaining everything needed to reproduce the build.

#### Scenario: Reproducible build from a fresh checkout
- **WHEN** the repository is cloned and the documented build command is run
- **THEN** the release APK SHALL build without requiring files that are excluded from version control, apart from the signing credentials

#### Scenario: Secrets and data are excluded
- **WHEN** the repository is inspected
- **THEN** keystore files, signing passwords, private seeds, production databases, and migration data SHALL NOT be present in version control

### Requirement: Automated release build

The system SHALL build the release APK automatically via GitHub Actions using the checked-in Gradle wrapper and the project's declared toolchain (Java 17), without relying on machine-specific local configuration.

#### Scenario: Release build from CI
- **WHEN** the CI workflow runs the release build task
- **THEN** a signed release APK SHALL be produced at the project's release output path

### Requirement: Tag-driven GitHub Release

The system SHALL create a GitHub Release, with the release APK attached, when a version tag matching the `v*` pattern (SemVer `MAJOR.MINOR.PATCH`) is pushed.

#### Scenario: Version tag produces a release
- **WHEN** a tag such as `v1.0.0` is pushed
- **THEN** a GitHub Release for that tag SHALL be created with the release APK attached

#### Scenario: Non-tag push does not create a release
- **WHEN** a commit is pushed to the main branch without a version tag
- **THEN** no GitHub Release SHALL be created

### Requirement: Signing via secrets

The CI workflow SHALL obtain the APK signing keystore and its credentials exclusively from GitHub Secrets (or environment configuration outside version control), never from files committed to the repository.

#### Scenario: Keystore supplied out-of-band
- **WHEN** the release build runs in CI
- **THEN** the keystore and passwords SHALL be sourced from GitHub Secrets and SHALL NOT be read from version-controlled files

#### Scenario: Missing signing credentials fail the build
- **WHEN** the release build runs without the required signing secrets configured
- **THEN** the build SHALL fail rather than produce an unsigned release artifact

### Requirement: Secrets never exposed in CI

The CI workflow SHALL NOT write signing secrets, keystores, or decoded key material to version control, uploaded build artifacts, or workflow logs.

#### Scenario: Keystore not uploaded as an artifact
- **WHEN** the release workflow runs
- **THEN** the decoded keystore file and signing passwords SHALL NOT appear in the uploaded artifacts or the release notes

### Requirement: Versioning strategy

The project SHALL follow Semantic Versioning (`MAJOR.MINOR.PATCH`) for releases, with the release tag and the app `versionName` kept consistent for a given release.

#### Scenario: Release version is consistent
- **WHEN** a release is produced from a tag
- **THEN** the tag SHALL match the app's release `versionName` and SHALL follow the `MAJOR.MINOR.PATCH` format

### Requirement: Android CI workflow name

The Android workflow SHALL be displayed in GitHub Actions as `Android CI`.

#### Scenario: Workflow list shows the Android CI name

- **WHEN** a user opens the repository's Actions tab
- **THEN** the Android workflow SHALL be listed as `Android CI`

### Requirement: CI workload separation

The Android CI SHALL run unit tests, lint and a debug build on every relevant push and pull request, SHALL run the Room migration test as a blocking release gate, SHALL run an automated sync smoke test as a blocking release gate, and SHALL run the full instrumented suite only on pull requests and manual dispatches. The sync smoke test SHALL exercise push, pull, and delete against a disposable server with a freshly provisioned key. Tag releases SHALL require the fast checks, the Room migration gate, and the sync smoke gate.

#### Scenario: Pull request runs the full suite
- **WHEN** a pull request targets main
- **THEN** the full instrumented suite SHALL run

#### Scenario: Main push and tag skip the full suite
- **WHEN** a commit is pushed to main or a release tag is pushed
- **THEN** the full instrumented suite SHALL NOT run
- **AND** the fast checks and Room migration gate SHALL still run

#### Scenario: Room migration failure blocks release
- **WHEN** the Room migration test fails for a release tag
- **THEN** the release job SHALL NOT run

#### Scenario: Sync smoke failure blocks release
- **WHEN** the push, pull, or delete smoke check fails for a release tag
- **THEN** the release job SHALL NOT run

#### Scenario: Sync smoke success allows release
- **WHEN** the fast checks, Room migration gate, and sync smoke gate all pass for a release tag
- **THEN** the release build and GitHub Release SHALL proceed

### Requirement: Superseded CI runs are cancelled

Both the Android and Server CI workflows SHALL cancel in-progress runs for the
same workflow and ref when a newer run starts.

#### Scenario: New run supersedes old run
- **WHEN** a newer run starts for the same workflow and ref
- **THEN** the older in-progress run SHALL be cancelled

### Requirement: Release tag matches the app version

The release job SHALL fail before building when the pushed tag `vX.Y.Z` does not
match `app.versionName` in `elisdb-android/gradle.properties`, so the published
APK cannot be labelled with a version different from its tag.

#### Scenario: Tag and version diverge

- **WHEN** the tag is `v1.13.0` but `app.versionName` is `1.12.0`
- **THEN** the release job SHALL exit with an error before building the APK

#### Scenario: Tag and version agree

- **WHEN** the tag is `v1.13.0` and `app.versionName` is `1.13.0`
- **THEN** the release job SHALL continue normally
