# server-deployment Specification

## Purpose
Defines secure, reproducible Docker deployment defaults for the ElisDB server, so a first run does not silently create accounts, expose an insecure URL, or bind to the public network.

## Requirements

### Requirement: Deployment defaults do not create accounts

The shipped Compose configuration SHALL NOT contain default user names, default secrets, or a placeholder server URL that can be used as-is. Provisioning SHALL run only when the operator explicitly lists users and provides a server URL, and SHALL fail with a clear error when users are present without a URL.

#### Scenario: Default compose has no preconfigured users
- **WHEN** an operator starts the server with the shipped defaults
- **THEN** no user account SHALL be provisioned
- **AND** no private seed SHALL appear in the logs

#### Scenario: Users without a URL fail fast
- **WHEN** the operator lists users but does not configure a server URL
- **THEN** provisioning SHALL fail with a clear error
- **AND** the server SHALL NOT start with partially provisioned users

#### Scenario: Placeholder URL is rejected
- **WHEN** the configured server URL is the shipped placeholder or is otherwise invalid
- **THEN** provisioning SHALL fail instead of issuing unusable configuration files

### Requirement: Server binds locally by default

The shipped deployment SHALL bind the HTTP port to loopback by default. Exposing the server on a non-loopback address SHALL require an explicit opt-in, and the deployment documentation SHALL require TLS or a TLS-terminating reverse proxy for any non-local exposure.

#### Scenario: Default bind is loopback
- **WHEN** the server starts without an explicit bind address
- **THEN** the published port SHALL listen on loopback only

#### Scenario: Public bind is explicit
- **WHEN** the operator explicitly configures a non-loopback bind
- **THEN** the server SHALL bind as requested
- **AND** the documentation SHALL warn that TLS or a reverse proxy is required

### Requirement: Startup configuration is validated

The server SHALL validate required deployment configuration before accepting requests and SHALL fail with an actionable message when it is invalid. Validation errors SHALL NOT include secret values.

#### Scenario: Invalid configuration stops startup
- **WHEN** a required environment value is missing or invalid
- **THEN** the server SHALL exit with a clear error before serving requests

#### Scenario: Errors do not leak secrets
- **WHEN** startup validation fails
- **THEN** the output SHALL NOT contain private seeds, signing keys, or photo bytes
