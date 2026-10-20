# Spec Delta

## MODIFIED Requirements

### Requirement: Public key registration

The system SHALL expose a server-managed flow that ISSUES an app's ed25519 keypair: it generates the keypair, records the public key in the server allowlist together with a mandatory comment identifying the user, and emits a configuration file carrying the server URL, the user name and the private seed for the operator to import into the app. Issuance SHALL be the only way to add a public key to the allowlist: the CLI SHALL NOT offer a separate registration command for externally created keys. The issuance flow SHALL be available interactively: running the CLI without arguments SHALL prompt for the required data sequentially, and the URL shall default to the `ELISDB_SERVER_URL` environment variable when the operator does not supply one.

#### Scenario: Issue a key for a user
- **WHEN** an operator issues a key for a named user providing the server URL
- **THEN** an ed25519 keypair SHALL be generated
- **AND** the public key SHALL be added to the allowlist
- **AND** the stored key SHALL carry a comment with the user's name
- **AND** a configuration file SHALL be produced containing the server URL, the user name and the private seed

#### Scenario: Configuration file names the user
- **WHEN** a configuration file is written for a named user
- **THEN** its `user_name` SHALL be that user's name, matching the allowlist comment
- **AND** it SHALL match the name reported by the identity endpoint for the issued key

#### Scenario: Interactive issuance prompts in order
- **WHEN** an operator runs the CLI without arguments and chooses to create an authentication
- **THEN** the CLI SHALL prompt for the server URL, then the user name, in that order
- **AND** once all data is provided it SHALL produce the same result as the argument-based issuance

#### Scenario: Interactive issuance defaults the server URL
- **WHEN** an operator runs the CLI interactively without a server URL and `ELISDB_SERVER_URL` is set
- **THEN** the CLI SHALL use the environment variable as the default server URL

#### Scenario: Issued key authenticates requests
- **WHEN** a request is signed with the private seed of an issued key
- **THEN** the request SHALL be authenticated against the allowlisted public key

#### Scenario: Issuance without a user name is refused
- **WHEN** an operator attempts to issue a key without providing a user name
- **THEN** the issuance SHALL be refused
- **AND** no key SHALL be added to the allowlist

#### Scenario: Issuance without a server URL is refused
- **WHEN** an operator attempts to issue a key without providing a server URL
- **THEN** the issuance SHALL be refused
- **AND** no configuration file SHALL be produced
- **AND** no key SHALL be added to the allowlist

#### Scenario: Externally created keys are not registrable via the CLI
- **WHEN** an operator tries to register a public key that was not produced by the server-issue flow
- **THEN** the CLI SHALL NOT offer a command to register it
- **AND** the externally created key SHALL NOT be added to the allowlist by the CLI

### Requirement: Startup provisioning from environment variables

The system SHALL provision trusted keys automatically when the container starts, for every user listed in the `ELISDB_USERS` environment variable (a comma-separated list of user names), so operators can declare first-run users declaratively in `compose.yaml` instead of running the CLI manually. Provisioning SHALL be idempotent: a listed user that already holds a trusted key SHALL NOT be issued a new key, so existing phones keep working across restarts. Keys issued automatically by provisioning SHALL be distinguishable from keys issued manually through the CLI. On start, a user removed from `ELISDB_USERS` SHALL have their auto-provisioned key revoked (removed from the allowlist), while manually issued keys SHALL be left untouched. When `ELISDB_USERS` is empty or unset, all auto-provisioned keys SHALL be revoked and their configuration files removed. Provisioning SHALL run before the server accepts requests and SHALL write one configuration file per newly provisioned user, carrying the server URL, the user name and the private seed, into a configs directory inside the data volume, ready for import into the app.

#### Scenario: Provision users listed in ELISDB_USERS
- **WHEN** the container starts and `ELISDB_USERS` lists one or more user names
- **THEN** a key SHALL be issued for each listed user that does not already hold a trusted key
- **AND** each issued public key SHALL be added to the allowlist with the user name as comment
- **AND** a configuration file SHALL be written for each newly provisioned user containing the server URL, the user name and the private seed

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
