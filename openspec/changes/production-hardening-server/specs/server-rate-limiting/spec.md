# Spec Delta — server-rate-limiting

## MODIFIED Requirements

### Requirement: Per-key and per-IP rate limiting

The server SHALL limit the rate of authenticated requests per signing key and of unauthenticated requests per client IP address. When a request exceeds its limit, the server SHALL reject it with an HTTP 429 response. Rate limiting SHALL be applied before the request body is read or buffered, so an unauthenticated oversized request is throttled without consuming memory first. Rate limiting SHALL NOT block the normal sync flow: a client performing ordinary pushes and pulls SHALL remain within the limits.

#### Scenario: Normal sync stays within limits
- **WHEN** an authenticated client performs ordinary pushes and pulls at a normal cadence
- **THEN** the server SHALL process those requests normally without returning 429

#### Scenario: Excessive requests are throttled
- **WHEN** a client sends requests faster than the configured limit for its key or IP
- **THEN** the server SHALL reject the excess requests with HTTP 429

#### Scenario: Unauthenticated endpoints are throttled by IP
- **WHEN** an unauthenticated client repeatedly hits an open endpoint (e.g. `/health` or `/api/time`) faster than the configured limit
- **THEN** the server SHALL reject the excess requests with HTTP 429
