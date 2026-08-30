## ADDED Requirements

### Requirement: Startup provisioning from environment variables

The system SHALL provision trusted keys automatically when the container starts, for every user listed in the `ELISDB_USERS` environment variable (a comma-separated list of user names), so operators can declare first-run users declaratively in `compose.yaml` instead of running the CLI manually. Provisioning SHALL be idempotent: a listed user that already holds a trusted key SHALL NOT be issued a new key, so existing phones keep working across restarts. Provisioning SHALL run before the server accepts requests and SHALL write one configuration file per newly provisioned user, carrying the server URL and the private seed, into a configs directory inside the data volume, ready for import into the app.

#### Scenario: Provision users listed in ELISDB_USERS
- **WHEN** the container starts and `ELISDB_USERS` lists one or more user names
- **THEN** a key SHALL be issued for each listed user that does not already hold a trusted key
- **AND** each issued public key SHALL be added to the allowlist with the user name as comment
- **AND** a configuration file SHALL be written for each newly provisioned user containing the server URL and the private seed

#### Scenario: Provisioning is idempotent across restarts
- **WHEN** the container starts and a listed user already holds a trusted key from a previous run
- **THEN** the system SHALL NOT issue a new key for that user
- **AND** the existing key SHALL remain valid for authentication

#### Scenario: Provisioning with no users listed does nothing
- **WHEN** the container starts and `ELISDB_USERS` is empty or unset
- **THEN** no keys SHALL be provisioned automatically
- **AND** the server SHALL start normally

#### Scenario: Provisioning requires a server URL
- **WHEN** the container starts with users listed in `ELISDB_USERS` but no server URL is configured
- **THEN** provisioning SHALL fail with an error
- **AND** no key SHALL be provisioned for the listed users