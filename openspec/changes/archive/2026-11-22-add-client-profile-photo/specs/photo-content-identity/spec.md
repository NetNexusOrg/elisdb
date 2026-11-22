# Spec Delta

## ADDED Requirements

### Requirement: Metadata-only changes do not rewrite photo content

When applying a pulled photo whose content is unchanged, the app SHALL update
photo metadata without downloading or rewriting the photo file.

#### Scenario: Profile flag changes on an unchanged photo
- **WHEN** a pull marks a photo as profile (or unmarks it) and the local photo
  bytes match the server's content hash
- **THEN** the app SHALL update only the stored profile flag
- **AND** SHALL NOT download, re-encode, re-write, or delete the photo file

#### Scenario: Unchanged content is still kept
- **WHEN** a pull returns an unchanged photo with different metadata
- **THEN** the existing local file and row SHALL be kept
- **AND** the row SHALL reflect the new metadata after the pull
