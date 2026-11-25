# Spec Delta

## ADDED Requirements

### Requirement: Duplicate merge preserves client services

When a locally created client is acknowledged as a duplicate, the merge SHALL preserve the service associations of both the local record and the canonical record. The merged record SHALL carry the union by catalog item, keeping the greater quantity when both records have the same device, and SHALL be marked pending for re-push.

#### Scenario: Services of both records are kept

- **WHEN** a local client acknowledged as duplicate has associations and the canonical client has its own
- **THEN** the retained client SHALL carry the union of both sets
- **AND** no association SHALL be lost

#### Scenario: Same device keeps the greater quantity

- **WHEN** the duplicate and the canonical record both have the same device
- **THEN** the retained association SHALL keep the greater of the two quantities

#### Scenario: Merged services are re-pushed

- **WHEN** the duplicate merge completes
- **THEN** the canonical client SHALL be marked pending
- **AND** its merged associations SHALL be pushed with it

#### Scenario: Canonical record is not available locally

- **WHEN** the canonical client does not exist locally during the merge
- **THEN** its associations SHALL be obtained from the server before the merge completes
- **AND** SHALL be preserved in the retained client
