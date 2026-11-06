# Spec Delta

## ADDED Requirements

### Requirement: Invalid configuration changes do not clear sync state

When an import or configuration change is rejected, the app SHALL preserve the current server configuration, watermark, photo hashes and pending records. Only a successful configuration change SHALL reset the server-scoped sync state.

#### Scenario: Rejected import preserves the current server
- **WHEN** an import fails validation
- **THEN** the settings screen SHALL keep showing the current configuration
- **AND** the sync state SHALL remain associated with the current server

#### Scenario: Successful change resets only server-scoped state
- **WHEN** a valid new configuration is applied
- **THEN** the app SHALL reset the server-scoped watermark and photo hashes
- **AND** SHALL keep local client records as before
