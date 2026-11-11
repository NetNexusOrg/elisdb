# Spec Delta — server-auth

## MODIFIED Requirements

### Requirement: Anti-replay protection

The server SHALL reject replayed requests by checking the signed timestamp; a request whose timestamp is too old or too far in the future SHALL be rejected. Within the accepted timestamp window, the server SHALL additionally reject a request whose signature has already been seen (replay), using a short-TTL nonce or a cache of consumed signatures, so a captured signed request cannot be replayed while its timestamp remains valid. The cache entry SHALL expire no earlier than the signed timestamp itself stops being valid, so a request signed with a timestamp ahead of the server clock remains protected for the whole time it would still be accepted.

#### Scenario: Request with a stale timestamp is rejected
- **WHEN** a signed request has a timestamp older than the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request with a future timestamp is rejected
- **WHEN** a signed request has a timestamp ahead of the server clock beyond the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Replayed request is rejected
- **WHEN** a signed request is replayed within its valid timestamp window
- **THEN** the server SHALL reject the replay (the signature has already been consumed) with an unauthorized error

#### Scenario: Distinct requests within the window are accepted
- **WHEN** two different signed requests arrive within the window with unique signatures
- **THEN** both SHALL be processed normally
