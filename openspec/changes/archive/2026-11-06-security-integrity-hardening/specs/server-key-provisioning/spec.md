# Spec Delta

## ADDED Requirements

### Requirement: Private seeds are never logged

Issuing or provisioning a key SHALL NOT write the private seed to standard output, container logs, or any log sink. The seed SHALL exist only in the owner-only configuration file returned to the operator; logs MAY include the key identifier and the configuration file path.

#### Scenario: Provisioned key does not leak its seed
- **WHEN** the container provisions a key from `ELISDB_USERS`
- **THEN** the private seed SHALL NOT appear in container output or logs
- **AND** the generated configuration file SHALL still contain the seed with `0600` permissions

#### Scenario: Manual issuance does not print the seed by default
- **WHEN** an operator issues a key without explicitly requesting the secret on the terminal
- **THEN** the private seed SHALL NOT be printed
- **AND** the operator SHALL be pointed to the owner-only configuration file

### Requirement: Configuration paths are confined

The system SHALL validate user names and key comments used to build configuration file paths. Values containing path separators, `..`, or characters outside a safe allowlist SHALL be rejected, and every resolved path SHALL be verified to remain inside the configured configs directory before writing or deleting.

#### Scenario: Traversal user name is rejected
- **WHEN** provisioning receives a user name containing `../` or a path separator
- **THEN** it SHALL reject that user
- **AND** SHALL NOT write or delete any file outside the configs directory

#### Scenario: Revocation stays inside the configs directory
- **WHEN** a provisioned key is revoked and its comment is used to locate the configuration file
- **THEN** the resolved path SHALL be inside the configs directory
- **AND** a comment that escapes it SHALL be refused rather than deleted
