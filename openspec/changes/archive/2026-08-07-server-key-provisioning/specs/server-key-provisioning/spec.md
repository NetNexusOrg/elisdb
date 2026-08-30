## Purpose

Provides an operator-facing, documented flow to register an app public key in the server's trust allowlist and to verify the allowlist is configured, so first-run and release setup are repeatable.

## ADDED Requirements

### Requirement: Public key registration

The system SHALL expose a documented, repeatable way for an operator to register an app's ed25519 public key in the server allowlist using the management interface.

#### Scenario: Register an app public key
- **WHEN** an operator follows the documented flow with the app's public key
- **THEN** the key SHALL be added to the allowlist
- **AND** subsequent requests signed by that app SHALL be authenticated

### Requirement: Trusted-key configuration visibility

The system SHALL let an operator determine whether any trusted public keys are registered, so an allowlist misconfigured at first run is detectable before clients sign requests.

#### Scenario: No trusted keys on first run
- **WHEN** an operator checks key configuration and no trusted keys are registered
- **THEN** the system SHALL indicate that no trusted keys exist, distinct from a successful listing of keys