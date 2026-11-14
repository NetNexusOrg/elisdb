## Purpose

Provides an operator-facing, documented flow to register an app public key in the server's trust allowlist and to verify the allowlist is configured, so first-run and release setup are repeatable.

## Requirements

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

### Requirement: Key identity is discoverable by the key holder

The system SHALL let a signed request discover the user name associated with the key that signed it, so a client can identify itself without holding additional metadata.

#### Scenario: The holder looks up their own identity
- **WHEN** an authenticated request asks for the identity of its own signing key
- **THEN** the response SHALL identify the key and SHALL include the user name associated with it

#### Scenario: Unknown key identity
- **WHEN** an authenticated request asks for its identity and no user name is stored for the key
- **THEN** the response SHALL still identify the key
- **AND** SHALL indicate that no user name is associated

### Requirement: Trusted-key configuration visibility

The system SHALL let an operator determine whether any trusted public keys are registered, so an allowlist misconfigured at first run is detectable before clients sign requests.

#### Scenario: No trusted keys on first run
- **WHEN** an operator checks key configuration and no trusted keys are registered
- **THEN** the system SHALL indicate that no trusted keys exist, distinct from a successful listing of keys

#### Scenario: Key listing shows user comments
- **WHEN** an operator lists the trusted keys
- **THEN** each listing entry SHALL include the key's user-name comment alongside its id, public key, and creation time

### Requirement: Device key listing is not exposed

The server SHALL NOT expose the full trusted-key allowlist to arbitrary authenticated client devices. Listing or removing trusted keys SHALL be an operator action (CLI) or scoped to an operator role, so a compromised or curious device cannot enumerate or tamper with other devices' keys. A device SHALL still be able to discover its own identity via the per-request identity endpoint (`/api/keys/me`).

#### Scenario: Device cannot list all keys
- **WHEN** an authenticated client device requests the full key list
- **THEN** the server SHALL refuse (or require operator scope) rather than returning all trusted keys

#### Scenario: Device can read its own identity
- **WHEN** an authenticated client device requests its own identity
- **THEN** the server SHALL return only that device's key id and user name

#### Scenario: Operator can still list keys
- **WHEN** an operator uses the CLI (or an operator-scoped request) to list keys
- **THEN** the full allowlist SHALL be returned as today

#### Scenario: A removed key is still revoked
- **WHEN** an operator removes a trusted key
- **THEN** requests signed by that key SHALL no longer be accepted, exactly as before

### Requirement: Issued configuration files are private

The system SHALL write issued configuration files (which carry the private seed) into the configured configs directory with file permissions that restrict access to the owner only (`0600`), so other local users cannot read device private keys.

#### Scenario: Config file is written owner-only
- **WHEN** an operator issues a key for a user
- **THEN** the configuration file containing the private seed SHALL be created with `0600` permissions
- **AND** the file SHALL be written into the configured configs directory
- **AND** the file SHALL NOT be readable by other users on the host or container

#### Scenario: Existing configuration file is not overwritten
- **WHEN** an operator issues a key for a user that already has a configuration file
- **THEN** the system SHALL refuse to overwrite it
- **AND** SHALL point the operator to a different output name

#### Scenario: Provisioned config file is written owner-only
- **WHEN** an operator issues a key through the operator interface with an explicit output directory
- **THEN** the configuration file containing the private seed SHALL be created with `0600` permissions
- **AND** the file SHALL be owned by the operator's user

### Requirement: Private seeds are never logged

Issuing a key SHALL NOT write the private seed to standard output, container logs, or any log sink. The seed SHALL exist only in the owner-only configuration file returned to the operator; logs MAY include the key identifier and the configuration file path.

#### Scenario: Issuance does not leak the seed
- **WHEN** an operator issues a key
- **THEN** the private seed SHALL NOT appear in terminal output, container output or logs
- **AND** the generated configuration file SHALL still contain the seed with `0600` permissions

#### Scenario: Manual issuance does not print the seed by default
- **WHEN** an operator issues a key without explicitly requesting the secret on the terminal
- **THEN** the private seed SHALL NOT be printed
- **AND** the operator SHALL be pointed to the owner-only configuration file

#### Scenario: Provisioned key does not leak its seed
- **WHEN** a key is issued and the server logs the operation
- **THEN** the private seed SHALL NOT appear in container output or logs
- **AND** the generated configuration file SHALL still contain the seed with `0600` permissions

### Requirement: Configuration paths are confined

The system SHALL validate user names and key comments used to build configuration file paths. Values containing path separators, `..`, or characters outside a safe allowlist SHALL be rejected, and every resolved path SHALL be verified to remain inside the configured configs directory before writing or deleting.

#### Scenario: Traversal user name is rejected
- **WHEN** an operator supplies a user name containing `../` or a path separator
- **THEN** the system SHALL reject that user
- **AND** SHALL NOT write or delete any file outside the configs directory

#### Scenario: Revocation stays inside the configs directory
- **WHEN** a key comment is used to locate a configuration file for deletion
- **THEN** the resolved path SHALL be inside the configs directory
- **AND** a comment that escapes it SHALL be refused rather than deleted
