# Spec Delta — release-pipeline

## ADDED Requirements

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
