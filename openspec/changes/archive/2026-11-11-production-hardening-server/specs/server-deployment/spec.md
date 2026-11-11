# Spec Delta — server-deployment

## ADDED Requirements

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
