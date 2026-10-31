# Spec Delta — client-settings

## ADDED Requirements

### Requirement: Outdated server warning

The configuration screen SHALL warn when the server reports an API version older
than the minimum the app relies on, or when it reports no API version at all.
The warning SHALL explain that some changes may not be stored and SHALL suggest
updating the server.

#### Scenario: Outdated server is flagged
- **WHEN** the configuration screen loads server information whose `api_version`
  is below the app minimum
- **THEN** the synchronization card SHALL show the outdated-server warning

#### Scenario: Server without an API version is flagged
- **WHEN** the server information has no `api_version`
- **THEN** the synchronization card SHALL show the outdated-server warning

#### Scenario: Current server shows no warning
- **WHEN** the server reports an API version greater than or equal to the app minimum
- **THEN** the warning SHALL NOT be shown
