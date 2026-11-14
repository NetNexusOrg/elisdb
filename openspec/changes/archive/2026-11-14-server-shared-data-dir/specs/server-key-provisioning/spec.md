# server-key-provisioning Specification

## REMOVED Requirements

### Requirement: Startup provisioning from environment variables

**Reason**: a gestão de chaves passa a ser exclusivamente explícita. O
provisionamento automático no arranque criava e revogava chaves com base numa
variável de ambiente, podendo invalidar dispositivos sem qualquer ação do
operador. Foi removido `elisdb/provision.py`, a chamada no entrypoint e
`ELISDB_USERS`.

**Migration**: as chaves existentes mantêm-se válidas (a remoção é apenas do
mecanismo automático). O operador cria novas chaves com `python -m elisdb.keys
issue` e, na fase seguinte, pela TUI. Servidores novos começam com zero chaves e
exigem uma emissão explícita.

## MODIFIED Requirements

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
