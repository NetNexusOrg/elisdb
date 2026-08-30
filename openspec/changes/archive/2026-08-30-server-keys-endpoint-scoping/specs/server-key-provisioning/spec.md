## MODIFIED Requirements

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
