# Spec Delta

## ADDED Requirements

### Requirement: Downloaded photo bytes are validated against declared MIME

Before materializing a downloaded photo, the app SHALL validate the response bytes against the MIME type declared by the server using magic-byte sniffing. A mismatch or unrecognized image SHALL abort materialization, keep the previous local file and row, and surface a sync error; the bytes SHALL NOT be stored as a photo.

#### Scenario: Matching image is materialized
- **WHEN** the server declares `image/jpeg`, `image/png`, or `image/webp` and the downloaded bytes match that format
- **THEN** the app SHALL store the photo and update its row

#### Scenario: Declared MIME does not match bytes
- **WHEN** the server declares `image/jpeg` but the downloaded bytes are PNG, WebP, or a non-image payload
- **THEN** the app SHALL reject the download
- **AND** SHALL keep the previous local file and row unchanged

#### Scenario: Unknown image payload is rejected
- **WHEN** the downloaded bytes do not match any accepted image format
- **THEN** the app SHALL reject the download
- **AND** SHALL NOT create or replace a photo file

#### Scenario: Failed validation is retryable
- **WHEN** a photo download fails validation
- **THEN** the sync SHALL report the failure
- **AND** a later retry SHALL be able to fetch and validate the photo again
