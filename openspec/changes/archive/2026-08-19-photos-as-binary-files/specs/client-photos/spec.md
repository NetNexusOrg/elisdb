## MODIFIED Requirements

### Requirement: Edited photos tracked for push

The system SHALL track which photos of a client changed since the last successful push, so the sync layer can upload binary content only for new or modified photos and reference the rest by id.

#### Scenario: Photo data needed after edit
- **WHEN** a photo is added or replaced since the last successful sync
- **THEN** the sync layer SHALL upload its binary content to the photo endpoint

#### Scenario: Unedited photos referenced on push
- **WHEN** a photo is unchanged since the last successful sync
- **THEN** the sync layer SHALL reference it by id without content in the next push