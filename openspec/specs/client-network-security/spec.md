# client-network-security Specification

## Purpose
Defines the Android app's outbound network policy so cleartext HTTP is limited to explicitly trusted local networks and every endpoint is validated before data is sent.

## Requirements

### Requirement: Cleartext is restricted to private hosts

The app SHALL permit cleartext HTTP only for loopback, private IPv4 (`10/8`, `172.16/12`, `192.168/16`), link-local (`169.254/16`), CGNAT (`100.64/10`), and private or link-local IPv6 addresses. HTTPS SHALL be permitted for any valid host. Any other cleartext destination SHALL be rejected before a connection is opened.

#### Scenario: Public HTTP host is rejected
- **WHEN** the configured server URL uses `http` and a public host name or public IP address
- **THEN** the app SHALL reject the URL with a clear configuration error
- **AND** SHALL NOT open a cleartext connection to that host

#### Scenario: Private HTTP host is allowed
- **WHEN** the configured server URL uses `http` and a loopback, private, link-local, CGNAT, or private IPv6 address
- **THEN** the app SHALL accept the URL and connect as configured

#### Scenario: Public HTTPS host is allowed
- **WHEN** the configured server URL uses `https` and any valid host
- **THEN** the app SHALL accept the URL

#### Scenario: Unsupported scheme is rejected
- **WHEN** the configured URL uses a scheme other than `http` or `https`
- **THEN** the app SHALL reject it before any request is made

### Requirement: One enforced network policy for every request

Every outbound request made by the app SHALL pass through the same URL policy, including sync, photo transfer, history, identity, time, update download, and any future endpoint. A request SHALL NOT bypass the policy through a separate HTTP stack or a redirect to a non-allowed scheme or host.

#### Scenario: All client calls are validated
- **WHEN** the app performs any server request
- **THEN** the destination SHALL be validated against the same cleartext policy before the request is sent

#### Scenario: Redirect to a disallowed destination is refused
- **WHEN** a server redirects a request to a cleartext public host or a non-HTTP(S) scheme
- **THEN** the app SHALL refuse to follow that redirect
- **AND** SHALL report a configuration or network error

#### Scenario: Direct cleartext bypass is blocked
- **WHEN** code attempts to open a cleartext connection to a non-private host outside the shared client
- **THEN** the platform network configuration or the app policy SHALL block it
- **AND** the bypass SHALL be treated as a defect

### Requirement: Insecure configuration fails closed and is visible

When the configured server URL violates the network policy, the app SHALL report a configuration fault and SHALL NOT attempt the request. It SHALL NOT downgrade to cleartext, silently switch hosts, or fall back to a previously stored URL.

#### Scenario: Legacy invalid URL fails closed
- **WHEN** a stored configuration contains a URL that the current policy rejects
- **THEN** sync SHALL fail with a configuration fault
- **AND** SHALL NOT send data to that URL

#### Scenario: No silent downgrade
- **WHEN** an HTTPS request fails
- **THEN** the app SHALL NOT retry the same request over cleartext to the same host
- **AND** SHALL surface the original failure
