## MODIFIED Requirements

### Requirement: Store photos in app-private storage

The system SHALL store all photo files in the app's private storage directory as compressed WebP images and persist only the file path and MIME type in the database.

#### Scenario: Photo file location
- **WHEN** a photo is captured or picked
- **THEN** the photo SHALL be downscaled and re-encoded to WebP before being stored in the app's private storage
- **AND** the database SHALL store the path and MIME type of the file
