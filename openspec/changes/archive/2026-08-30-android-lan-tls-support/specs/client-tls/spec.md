## Purpose

Lets the app trust a user-supplied certificate or certificate authority for the configured sync server, so a self-hosted server on a LAN (using a self-signed or private-CA HTTPS certificate) can be reached over TLS without falling back to cleartext and without being blocked by the system trust manager.

## ADDED Requirements
### Requirement: User-configured trust anchor

The system SHALL allow the user to import or configure a certificate (or CA) that the app trusts for the configured server host, and SHALL use that trust anchor when establishing HTTPS connections to the server, in addition to the system trust store.

#### Scenario: Self-signed LAN server is trusted after import
- **WHEN** the user imports the server's certificate/CA in settings and configures the server URL
- **THEN** the app SHALL connect over HTTPS trusting that certificate
- **AND** SHALL NOT require a publicly trusted certificate

#### Scenario: No configured anchor uses normal validation
- **WHEN** no user-supplied certificate is configured
- **THEN** the app SHALL validate the server certificate against the system trust store as normal

#### Scenario: Untrusted server is still rejected
- **WHEN** the server presents a certificate that is neither system-trusted nor matching the user-supplied anchor
- **THEN** the connection SHALL be rejected

### Requirement: Trust anchor scoped to the configured host

The user-supplied trust anchor SHALL apply only to the configured server host, not globally relax TLS validation for other hosts.

#### Scenario: Anchor does not weaken other connections
- **WHEN** a user-supplied anchor is configured for the sync server host
- **THEN** TLS connections to other hosts SHALL continue to use the system trust store
