# Spec Delta — app-build

## ADDED Requirements

### Requirement: Release builds are exercised before tagging

The CI SHALL assemble the minified release APK on every pull request, using an
ephemeral signing key provided by the workflow, so that R8/ProGuard or
resource-shrinking regressions fail before a release tag exists.

#### Scenario: Pull request builds the release variant

- **WHEN** a pull request targets `main`
- **THEN** the test job SHALL generate a throwaway keystore and run
  `:app:assembleRelease`
- **AND** the job SHALL fail if the release build fails

### Requirement: Encrypted settings tests gate releases

The blocking instrumented job used as the release gate SHALL run the encrypted
settings-store tests together with the Room migration tests.

#### Scenario: Encrypted store regression blocks the release

- **WHEN** the blocking instrumented job runs
- **THEN** it SHALL execute both `AppDatabaseMigrationTest` and
  `SettingsStoreTest`
- **AND** a failure in either test SHALL fail the release gate
