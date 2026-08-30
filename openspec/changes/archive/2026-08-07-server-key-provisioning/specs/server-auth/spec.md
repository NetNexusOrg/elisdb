## Purpose

Extends the key management capability to surface operator-facing configuration health on first run, so a server with no trusted keys is detected early instead of only surfacing 401s.

## MODIFIED Requirements

### Requirement: Management of trusted keys

The system SHALL expose a management surface to add, remove, and list trusted public keys, and SHALL make an empty/first-run allowlist observable so operators can confirm configuration.

The following scenarios are added to the existing key-management requirements.

#### Scenario: Empty allowlist is distinguishable
- **WHEN** no trusted keys are registered
- **THEN** the management surface SHALL indicate that the allowlist is empty, distinct from a listing that contains keys