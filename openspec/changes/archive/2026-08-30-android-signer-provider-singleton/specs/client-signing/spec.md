## Purpose

Covers the mechanics of producing ed25519 request signatures on the device, including one-time registration of the cryptographic provider so repeated signing does not leak static state.

## ADDED Requirements
### Requirement: Cryptographic provider registered once

The app SHALL register the EdDSA security provider at most once for the process lifetime. Constructing a new signer or performing additional signatures SHALL NOT add another copy of the provider to the static security provider list.

#### Scenario: First signer registers the provider
- **WHEN** the first `Signer` is constructed in the process
- **THEN** the EdDSA provider SHALL be registered

#### Scenario: Subsequent signers do not duplicate the provider
- **WHEN** further `Signer` instances are constructed (e.g. on later syncs)
- **THEN** no additional copy of the provider SHALL be added to the static list

#### Scenario: Signing still works after many signers
- **WHEN** many `Signer` instances have been constructed over the process lifetime
- **THEN** requests SHALL continue to be signed and verified normally
