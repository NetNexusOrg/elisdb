## ADDED Requirements

### Requirement: Check for updates from the About screen

The app SHALL offer an explicit action on the About screen that asks the server for the newest published build and compares its version code with the installed one, reporting whether a newer build is available, the app is up to date, no build is published, the server is not configured, or the check failed.

#### Scenario: Newer build available
- **WHEN** the server reports a version code greater than the installed one
- **THEN** the screen SHALL show the version name, version code and download size
- **AND** offer to download the package

#### Scenario: App is up to date
- **WHEN** the server reports a version code equal to or lower than the installed one
- **THEN** the screen SHALL report that the app is up to date

#### Scenario: No build published
- **WHEN** the server answers that no release is available
- **THEN** the screen SHALL report that there are no updates, not a connection error

#### Scenario: Server not configured
- **WHEN** no server configuration has been imported
- **THEN** the check SHALL NOT send a request
- **AND** the screen SHALL ask the user to import the configuration first

#### Scenario: Check fails
- **WHEN** the request fails because of the network or the server
- **THEN** the screen SHALL report the failure
- **AND** offer to try again

### Requirement: Download and verify the published build

The app SHALL download the offered package into its cache while reporting progress, and SHALL verify the size and SHA-256 digest reported by the server before offering to install it.

#### Scenario: Download reports progress
- **WHEN** the download is running
- **THEN** the screen SHALL show the transferred amount or percentage

#### Scenario: Verified package is ready
- **WHEN** the download completes and the digest and size match the server metadata
- **THEN** the screen SHALL offer to install the package

#### Scenario: Corrupted download is discarded
- **WHEN** the digest or size does not match the metadata
- **THEN** the downloaded file SHALL be deleted
- **AND** the screen SHALL report an integrity failure distinct from a network failure

### Requirement: Install the verified package

The app SHALL hand the verified package to the system package installer through a content URI, and SHALL request the install-unknown-apps permission when it is missing.

#### Scenario: Installer opened
- **WHEN** the user taps install and the install-unknown-apps permission is granted
- **THEN** the system installer SHALL open for the downloaded package

#### Scenario: Permission missing
- **WHEN** the user taps install and the permission is missing
- **THEN** the app SHALL open the settings page where the permission is granted
