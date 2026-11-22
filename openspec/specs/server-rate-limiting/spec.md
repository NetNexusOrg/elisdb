## Purpose

Limita a frequência de pedidos ao servidor de sincronização por chave autenticada e por endereço IP, devolvendo 429 quando o limite é excedido, para impedir que um único cliente (ou um atacante não autenticado) esgote CPU/recursos com pedidos repetidos.

## Requirements

### Requirement: Per-key and per-IP rate limiting
The server SHALL rate-limit authenticated requests per signing key and unauthenticated requests per client IP, rejecting excess with HTTP 429. Limiting SHALL run before the request body is read or buffered, so an oversized unauthenticated request is throttled without consuming memory. Normal pushes and pulls SHALL remain within the limits.

#### Scenario: Normal sync stays within limits
- **WHEN** an authenticated client performs ordinary pushes and pulls at a normal cadence
- **THEN** the server SHALL process those requests normally without returning 429

#### Scenario: Excessive requests are throttled
- **WHEN** a client sends requests faster than the configured limit for its key or IP
- **THEN** the server SHALL reject the excess requests with HTTP 429

#### Scenario: Unauthenticated endpoints are throttled by IP
- **WHEN** an unauthenticated client repeatedly hits an open endpoint (e.g. `/health` or `/api/time`) faster than the configured limit
- **THEN** the server SHALL reject the excess requests with HTTP 429

### Requirement: Rate limiting is configurable

The server SHALL allow the rate-limit window and request budget to be configured through environment variables, with sensible defaults, so operators can tune them without code changes.

#### Scenario: Limits are configurable
- **WHEN** an operator sets the rate-limit environment variables
- **THEN** the server SHALL apply the configured limits instead of the defaults
