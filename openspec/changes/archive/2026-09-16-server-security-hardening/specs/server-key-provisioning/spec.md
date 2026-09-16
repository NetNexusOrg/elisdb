## ADDED Requirements

### Requirement: Issued configuration files are private

The system SHALL write issued configuration files (which carry the private seed) with file permissions that restrict access to the owner only (`0600`), so other local users cannot read device private keys.

#### Scenario: Config file is written owner-only
- **WHEN** an operator issues a key for a user
- **THEN** the configuration file containing the private seed SHALL be created with `0600` permissions
- **AND** the file SHALL NOT be readable by other users on the host or container

#### Scenario: Provisioned config file is written owner-only
- **WHEN** the container provisions a key automatically from `ELISDB_USERS`
- **THEN** the configuration file containing the private seed SHALL be created with `0600` permissions
