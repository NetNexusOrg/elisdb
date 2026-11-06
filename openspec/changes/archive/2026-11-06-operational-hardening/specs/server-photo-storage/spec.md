# Spec Delta

## ADDED Requirements

### Requirement: Photo content hash is persisted and reused

When the server computes a photo's content hash for a record with no stored hash, it SHALL persist that hash in the same committed operation and reuse it on later reads. A read SHALL NOT leave an uncommitted hash update that is discarded and recomputed on every request.

#### Scenario: Missing hash is persisted
- **WHEN** a stored photo has no content hash and its file exists
- **THEN** the server SHALL compute the hash
- **AND** SHALL persist it so later reads return the stored value

#### Scenario: Later reads reuse the hash
- **WHEN** the same photo is read again after the hash was persisted
- **THEN** the server SHALL NOT recompute the file hash
- **AND** SHALL return the persisted hash
