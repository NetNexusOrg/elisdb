# Spec Delta — server-sync

## ADDED Requirements

### Requirement: API version in server status

The server SHALL expose an API version, independent from its release version, in
both the open health response and the authenticated latest-data response. The
API version SHALL increase whenever a contract change matters for older clients.
The field SHALL be additive so older clients keep working.

#### Scenario: Health reports the API version
- **WHEN** a client requests `/health`
- **THEN** the response SHALL include `api_version` with the current API version

#### Scenario: Latest data reports the API version
- **WHEN** an authenticated client requests `/api/latest`
- **THEN** the response SHALL include `api_version` with the current API version
- **AND** SHALL keep `latest_updated_at` and `client_count`

#### Scenario: Older clients are unaffected
- **WHEN** a client that does not know `api_version` reads either response
- **THEN** it SHALL continue to work because the field is additive
