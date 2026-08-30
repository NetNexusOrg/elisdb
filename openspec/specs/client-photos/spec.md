## Purpose

Manages a client's photos: capturing new photos with the device camera, picking existing photos from the device gallery, and storing them in the app's private storage.

## Requirements
### Requirement: Capture a photo with the camera

The system SHALL allow the user to take a photo with the device camera and attach it to the client being edited.

#### Scenario: Camera permission granted
- **WHEN** the user taps "take photo" and the camera permission is granted
- **THEN** the camera SHALL open
- **AND** a captured photo SHALL be saved and attached to the client

#### Scenario: Camera permission denied
- **WHEN** the user taps "take photo" and the camera permission is denied
- **THEN** the system SHALL prompt for the camera permission
- **AND** if denied, the system SHALL inform the user and SHALL NOT open the camera

### Requirement: Pick a photo from the gallery

The system SHALL allow the user to select an existing photo from the device gallery using the system photo picker, without requiring storage permission.

#### Scenario: Photo selected from the gallery
- **WHEN** the user taps "choose from gallery" and selects a photo
- **THEN** the selected photo SHALL be copied into the app's storage and attached to the client

#### Scenario: Gallery selection cancelled
- **WHEN** the user opens the gallery picker and cancels
- **THEN** no photo SHALL be added and the app SHALL remain on the edit screen

### Requirement: Store photos in app-private storage

The system SHALL store all photo files in the app's private storage directory as compressed WebP images and persist only the file path and MIME type in the database.

#### Scenario: Photo file location
- **WHEN** a photo is captured or picked
- **THEN** the photo SHALL be downscaled and re-encoded to WebP before being stored in the app's private storage
- **AND** the database SHALL store the path and MIME type of the file

### Requirement: Remove a photo

The system SHALL allow removing a photo from a client during editing.

#### Scenario: Remove a photo
- **WHEN** the user removes a photo in the edit screen
- **THEN** the photo SHALL no longer be associated with the client
- **AND** its file SHALL be removed from app storage

### Requirement: Edited photos tracked for push

The system SHALL track which photos of a client changed since the last successful push, so the sync layer can upload binary content only for new or modified photos and reference the rest by id.

#### Scenario: Photo data needed after edit
- **WHEN** a photo is added or replaced since the last successful sync
- **THEN** the sync layer SHALL upload its binary content to the photo endpoint

#### Scenario: Unedited photos referenced on push
- **WHEN** a photo is unchanged since the last successful sync
- **THEN** the sync layer SHALL reference it by id without content in the next push
