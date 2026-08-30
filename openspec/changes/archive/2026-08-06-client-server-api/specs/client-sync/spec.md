## Purpose

Enables the app to exchange client and photo data with the server's push/pull API over authenticated HTTP, so an offline-first sync layer can push and pull the full dataset. This is the transport and request-signing layer; it does not decide conflicts.

## ADDED Requirements

### Requirement: Push records to the server

The system SHALL send local client and photo records to the server via the push endpoint, returning a per-record status, and SHALL represent unsynced records as base64 photos.

#### Scenario: Push returns per-record status
- **WHEN** the app sends a batch of client and photo records to the server
- **THEN** the server response SHALL be interpreted as a per-record status per each pushed client
- **AND** the statuses SHALL be (accepted/duplicate/stale) and returned onward to the caller

### Requirement: Pull the authoritative dataset

The system SHALL fetch all server client and photo records via the pull endpoint, so the caller can reconcile local data with the server.

#### Scenario: Pull returns all records with photos
- **WHEN** the app requests a pull
- **THEN** all server-stored clients with their photos SHALL be returned
- **AND** photos SHALL be decoded from base64 to app-private photo files

### Requirement: Delete a client on the server

The system SHALL permit deleting a client by its identifier via the server management endpoint.

#### Scenario: Delete server client
- **WHEN** the app requests deletion of a client by identifier
- **THEN** the server SHALL remove the client and its photos

### Requirement: Request authentication

Every request the app makes to the server SHALL carry the server's ed25519 request signature: a fresh timestamp and a signature over the message `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`, using an in-app private key whose matching public key is registered on the server.

#### Scenario: Requests are signed
- **WHEN** the app calls a push, pull, or delete endpoint
- **THEN** the request SHALL include the required auth headers derived from the in-app key and current timestamp
- **AND** a rejected (401) response SHALL be surfaced so the sync layer can report it

#### Scenario: Server configuration is configurable
- **WHEN** the app is configured with a server base URL
- **THEN** all data API requests SHALL be made against that base URL