## MODIFIED Requirements

### Requirement: Photo skip by content identity (bandwidth)

In a pull, photos SHALL be represented by their content hash rather than their bytes, and the app SHALL download a photo's binary content from the dedicated photo endpoint only when it does not already hold a local copy with the same hash.

#### Scenario: Unchanged photos not retransmitted
- **WHEN** a pull returns a photo whose content hash matches a photo the app already stores locally
- **THEN** the app SHALL NOT download that photo again

#### Scenario: New or changed photos downloaded as binary
- **WHEN** a pull returns a photo whose content hash is missing locally or differs from the local copy
- **THEN** the app SHALL download that photo's binary content from the dedicated photo endpoint
- **AND** the downloaded bytes SHALL be stored locally for future pulls