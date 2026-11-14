# server-deployment Specification

## Purpose
Defines secure, reproducible Docker deployment defaults for the ElisDB server, so a first run does not silently create accounts, expose an insecure URL, or bind to the public network.

## Requirements

### Requirement: Deployment defaults do not create accounts

The shipped Compose configuration SHALL NOT contain default user names, default secrets, or a placeholder server URL that can be used as-is. Starting the server SHALL NOT create, revoke, or modify any account or trusted key. Accounts SHALL only be created by an explicit operator action outside the server process, and a server with no registered keys SHALL still start and answer health checks.

#### Scenario: Default compose has no preconfigured users
- **WHEN** an operator starts the server with the shipped defaults
- **THEN** no user account SHALL be provisioned
- **AND** no private seed SHALL appear in the logs

#### Scenario: Startup never provisions accounts
- **WHEN** the container starts, with or without a server URL configured
- **THEN** no trusted key SHALL be created, revoked, or modified
- **AND** request authentication SHALL depend only on keys registered before startup

#### Scenario: Empty key set starts normally
- **WHEN** the server starts with no trusted keys registered
- **THEN** the server SHALL start and answer health checks
- **AND** authenticated endpoints SHALL reject requests until the operator registers a key

#### Scenario: Users without a URL fail fast
- **WHEN** the operator issues a key without a configured server URL
- **THEN** issuance SHALL fail with a clear error
- **AND** no key SHALL be registered for that user

#### Scenario: Placeholder URL is rejected
- **WHEN** the operator issues a key with the shipped placeholder or an otherwise invalid server URL
- **THEN** issuance SHALL fail instead of writing an unusable configuration file

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

The deployment procedure SHALL NOT upload local databases, device credentials, photos, runtime data or environment files to the server. The synchronisation step SHALL neither delete remote files nor overwrite the host-owned data and configs directories. When a server container is already running, a deployment SHALL create a backup before rebuilding and SHALL abort if the backup fails.

#### Scenario: Local data and secrets are not uploaded
- **WHEN** the deploy script synchronises the server directory
- **THEN** `*.db`, `*.db-*`, `*.config.json`, `photos/`, `data/`, `elisdb-data/`, `configs/`, `.env*` and logs SHALL be excluded

#### Scenario: Synchronisation does not delete runtime data
- **WHEN** the deploy script synchronises the server directory
- **THEN** it SHALL NOT use delete semantics
- **AND** files under `elisdb-data/` and `configs/` SHALL remain untouched

#### Scenario: Pre-deploy backup is required
- **WHEN** a server container is running and a deployment starts
- **THEN** a backup SHALL be created before `docker compose up -d --build`
- **AND** a failed backup SHALL abort the deployment

#### Scenario: First deployment skips the backup
- **WHEN** no server container is running yet
- **THEN** the deployment SHALL proceed without a pre-deploy backup and say so

### Requirement: Runtime data is host-owned and shared

The deployment SHALL mount the database, photos, APK files and backups from a host directory selected by `ELISDB_DATA_DIR` (default `./elisdb-data`), and device configuration files from a separate host directory (default `./configs`).

#### Scenario: Container and local runs share the same data
- **WHEN** the operator stops the container and starts the server locally with the same `.env`
- **THEN** both runs SHALL use the same database, photos, APK directory and device configurations
- **AND** no copying or exporting SHALL be required

#### Scenario: Device configurations are not part of the data directory
- **WHEN** the operator backs up or copies the data directory
- **THEN** device configuration files SHALL NOT be included automatically
- **AND** they SHALL remain available from their own host directory

### Requirement: Container runs as an unprivileged host user

The container SHALL run as `PUID:PGID`, SHALL NOT change ownership of mounted paths at startup, and SHALL fail with an actionable message when a mount source is missing or not writable by that user.

#### Scenario: Startup fails on an unwritable data directory
- **WHEN** the mounted data or configs directory is not writable by `PUID:PGID`
- **THEN** startup SHALL fail with a message naming the directory and the expected owner
- **AND** the server SHALL NOT begin accepting requests

#### Scenario: Startup does not change ownership
- **WHEN** the container starts against existing host directories
- **THEN** it SHALL NOT alter the owner or permissions of the mounted files
- **AND** files created at runtime SHALL be owned by `PUID:PGID`

### Requirement: Configuration comes from one environment file

Operator-facing settings SHALL come from a single `.env` file next to `compose.yaml`, and a versioned `.env.example` SHALL document every supported key without real values. The Compose file SHALL interpolate these values and SHALL NOT contain a working server URL, a user list, or a secret. The `.env` file SHALL be git-ignored and readable only by its owner.

#### Scenario: Shipped example contains no secrets
- **WHEN** an operator inspects the versioned `.env.example`
- **THEN** it SHALL list every supported key with safe defaults or empty values
- **AND** it SHALL NOT contain a real server URL, user list, key, or seed

#### Scenario: Missing environment file falls back to safe defaults
- **WHEN** no `.env` file exists
- **THEN** the deployment SHALL use the documented safe defaults
- **AND** the server SHALL bind to loopback and require no accounts

#### Scenario: Environment file is never committed
- **WHEN** an operator creates `.env`
- **THEN** Git SHALL ignore it
- **AND** it SHALL be readable only by its owner

### Requirement: Container image contains no runtime data

The image build SHALL copy an explicit allowlist of application paths and SHALL exclude device configurations, runtime data directories, environment files, operator scripts and VCS metadata, so a local `.env` or `configs/` cannot be baked into a published image.

#### Scenario: Local configuration and data are excluded
- **WHEN** the image is built from a checkout that contains `.env`, `configs/` and `elisdb-data/`
- **THEN** the resulting image SHALL NOT contain those files
- **AND** the image SHALL still import the server package and start with the shipped defaults
