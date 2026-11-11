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

### Requirement: Single-process deployment is enforced

The server SHALL refuse to start when the deployment asks for more than one worker process, because the replay cache and rate limiter are per-process and SQLite is single-host. The startup error SHALL name the offending variable.

#### Scenario: Multiple workers are refused
- **WHEN** `WEB_CONCURRENCY` or `UVICORN_WORKERS` is greater than 1
- **THEN** startup SHALL fail with an actionable error before serving requests

#### Scenario: Default single-worker start is accepted
- **WHEN** neither variable is set or both are 1
- **THEN** the server SHALL start normally

### Requirement: Deployments preserve server data

The deployment procedure SHALL NOT upload local databases, device credentials, photos, runtime data or environment files to the server. When a server container is already running, a deployment SHALL create a backup before rebuilding and SHALL abort if the backup fails.

#### Scenario: Local data and secrets are not uploaded
- **WHEN** the deploy script synchronises the server directory
- **THEN** `*.db`, `*.config.json`, `photos/`, `data/`, `.env*` and logs SHALL be excluded

#### Scenario: Pre-deploy backup is required
- **WHEN** a server container is running and a deployment starts
- **THEN** a backup SHALL be created before `docker compose up -d --build`
- **AND** a failed backup SHALL abort the deployment

#### Scenario: First deployment skips the backup
- **WHEN** no server container is running yet
- **THEN** the deployment SHALL proceed without a pre-deploy backup and say so
