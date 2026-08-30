## MODIFIED Requirements

### Requirement: Startup provisioning from environment variables

The system SHALL provision trusted keys automatically when the container starts, for every user listed in the `ELISDB_USERS` environment variable (a comma-separated list of user names), so operators can declare first-run users declaratively in `compose.yaml` instead of running the CLI manually. Provisioning SHALL be idempotent: a listed user that already holds a trusted key SHALL NOT be issued a new key, so existing phones keep working across restarts. Keys issued automatically by provisioning SHALL be distinguishable from keys issued manually through the CLI. On start, a user removed from `ELISDB_USERS` SHALL have their auto-provisioned key revoked (removed from the allowlist), while manually issued keys SHALL be left untouched. When `ELISDB_USERS` is empty or unset, all auto-provisioned keys SHALL be revoked and their configuration files removed. Provisioning SHALL run before the server accepts requests and SHALL write one configuration file per newly provisioned user, carrying the server URL and the private seed, into a configs directory inside the data volume, ready for import into the app.

#### Scenario: Provision users listed in ELISDB_USERS
- **WHEN** the container starts and `ELISDB_USERS` lists one or more user names
- **THEN** a key SHALL be issued for each listed user that does not already hold a trusted key
- **AND** each issued public key SHALL be added to the allowlist with the user name as comment
- **AND** a configuration file SHALL be written for each newly provisioned user containing the server URL and the private seed

#### Scenario: Provisioning is idempotent across restarts
- **WHEN** the container starts and a listed user already holds a trusted key from a previous run
- **THEN** the system SHALL NOT issue a new key for that user
- **AND** the existing key SHALL remain valid for authentication

#### Scenario: Empty ELISDB_USERS revokes all provisioned keys
- **WHEN** the container starts and `ELISDB_USERS` is empty or unset
- **THEN** all auto-provisioned keys SHALL be revoked from the allowlist
- **AND** their configuration files SHALL be removed
- **AND** requests signed with revoked keys SHALL no longer be accepted
- **AND** the server SHALL start normally

#### Scenario: Provisioning requires a server URL
- **WHEN** the container starts with users listed in `ELISDB_USERS` but no server URL is configured
- **THEN** provisioning SHALL fail with an error
- **AND** no key SHALL be provisioned for the listed users

#### Scenario: Removing a user from ELISDB_USERS revokes their provisioned key
- **WHEN** the container starts with a user that previously held an auto-provisioned key but is no longer listed in `ELISDB_USERS`
- **THEN** that user's auto-provisioned key SHALL be removed from the allowlist
- **AND** requests signed with the removed key SHALL no longer be accepted

#### Scenario: Manually issued keys are not revoked by provisioning
- **WHEN** the container starts and a key issued manually through the CLI carries a user name not listed in `ELISDB_USERS`
- **THEN** that key SHALL remain in the allowlist
- **AND** the key SHALL continue to authenticate requests
