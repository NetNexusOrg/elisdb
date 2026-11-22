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

### Requirement: Choose a client's profile photo

The system SHALL let the user choose which of a client's photos is the profile
photo shown in the client list, independently of the photo's position in the
gallery.

#### Scenario: Mark a photo as profile
- **WHEN** the user taps the profile control on a photo in the edit screen
- **THEN** that photo SHALL be marked as the client's profile photo
- **AND** any previously marked photo SHALL be unmarked

#### Scenario: Profile choice does not reorder the gallery
- **WHEN** the user marks a photo that is not the first in the gallery
- **THEN** the photos SHALL keep their current order in the edit grid
- **AND** the marked photo SHALL be shown as the profile photo in the client list

#### Scenario: Only one profile photo per client
- **WHEN** the user marks a different photo as profile
- **THEN** exactly one photo SHALL remain marked for that client

### Requirement: Client list shows the chosen profile photo

The system SHALL show the photo marked as profile in the client list and SHALL
fall back to the first photo by gallery order when no photo is marked.

#### Scenario: Preferred photo is shown
- **WHEN** a client has a photo marked as profile
- **THEN** the client list SHALL show that photo

#### Scenario: Unmarked photos fall back to the first photo
- **WHEN** a client has photos but none marked as profile, including photos
  stored before this feature existed
- **THEN** the client list SHALL show the first photo by gallery order

#### Scenario: Client without photos shows the generic avatar
- **WHEN** a client has no photos
- **THEN** the client list SHALL show the generic avatar

### Requirement: Removing the profile photo reassigns it

The system SHALL keep a usable profile photo when the marked photo is removed,
without requiring an extra user action.

#### Scenario: Next photo becomes the profile
- **WHEN** the user removes the photo marked as profile and other photos remain
- **THEN** the first remaining photo SHALL become the profile photo

#### Scenario: Last photo removed leaves no profile
- **WHEN** the user removes the last photo of a client
- **THEN** the client SHALL have no profile photo
- **AND** the client list SHALL show the generic avatar

#### Scenario: First photo added becomes the profile
- **WHEN** the user adds a photo to a client that has no photos
- **THEN** the new photo SHALL be marked as profile

### Requirement: Profile photo choice is saved with the client

The system SHALL persist the chosen profile photo as part of the client's saved
photos, so it survives app restarts.

#### Scenario: Choice survives reopen
- **WHEN** the user saves a client after choosing a profile photo
- **AND** later reopens the client
- **THEN** the same photo SHALL still be marked as profile

#### Scenario: Choice is applied on save
- **WHEN** the user changes the profile photo and leaves the edit screen without
  saving
- **THEN** the previous profile photo SHALL remain in effect

### Requirement: Profile photo choice synchronizes between devices

The system SHALL include the profile-photo choice in the synchronized photo
metadata so every device shows the same profile photo.

#### Scenario: Choice is pushed with the client
- **WHEN** a client with a marked profile photo is pushed
- **THEN** the profile flag SHALL be sent with that photo's metadata

#### Scenario: Choice is applied on pull
- **WHEN** another device pulls the client
- **THEN** it SHALL mark the same photo as profile
- **AND** the client list SHALL show that photo

#### Scenario: Unchanged photo is not downloaded for a flag change
- **WHEN** a pull changes only the profile flag of a photo whose bytes are
  unchanged
- **THEN** the app SHALL update the stored flag without downloading or rewriting
  the photo file
