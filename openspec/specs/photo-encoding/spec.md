## Purpose

Defines how photos are normalized before storage: downscaling, EXIF orientation correction, and WebP re-encoding, applied consistently to camera captures and gallery picks without re-encoding photos that are already stored or synced.

## Requirements

### Requirement: Photos are downscaled to a maximum dimension

The system SHALL downscale every newly stored photo so that its longest side does not exceed 1800 pixels, preserving the aspect ratio.

#### Scenario: Large photo is downscaled
- **WHEN** a photo with a side longer than 1800 pixels is captured or picked
- **THEN** the stored photo SHALL have its longest side at most 1800 pixels

#### Scenario: Small photo is not upscaled
- **WHEN** a photo whose longest side is at most 1800 pixels is captured or picked
- **THEN** the stored photo SHALL NOT be upscaled

### Requirement: Photos are re-encoded to WebP

The system SHALL encode newly stored photos as WebP lossy and record their MIME type as `image/webp`.

#### Scenario: Photo stored as WebP
- **WHEN** a photo is captured or picked
- **THEN** the stored file SHALL be a WebP image
- **AND** the photo's recorded MIME type SHALL be `image/webp`

### Requirement: EXIF orientation is applied before encoding

The system SHALL read the source photo's EXIF orientation and apply the full transform to the pixels before encoding, including rotation and mirroring, so the stored photo displays with the correct orientation and handedness.

#### Scenario: Rotated photo is stored upright
- **WHEN** a photo carrying EXIF orientation metadata other than normal is captured or picked
- **THEN** the stored photo SHALL be encoded with the orientation already applied to the pixels

#### Scenario: Mirrored photo keeps its handedness
- **WHEN** a photo carries a mirrored EXIF orientation (for example a horizontally flipped selfie)
- **THEN** the stored photo SHALL be encoded with the mirroring applied to the pixels
- **AND** SHALL NOT appear flipped compared with the source

#### Scenario: Normal orientation is unchanged
- **WHEN** a photo carries the normal EXIF orientation
- **THEN** its pixels SHALL be encoded without rotation or mirroring

### Requirement: Camera and gallery share the same encoding

The system SHALL apply the same downscaling, orientation correction, and WebP encoding to photos regardless of whether they come from the camera or the gallery.

#### Scenario: Same constraints for both sources
- **WHEN** an equivalent photo is captured with the camera and picked from the gallery
- **THEN** both SHALL be stored downscaled and encoded to WebP under the same constraints

### Requirement: Existing photos are not re-encoded

The system SHALL NOT re-encode photos that are already stored or already synced; only newly captured or picked photos SHALL be compressed.

#### Scenario: Already-synced photo keeps its bytes
- **WHEN** a photo already exists locally and on the server as JPEG
- **THEN** its bytes SHALL remain unchanged
- **AND** its content hash SHALL remain unchanged, so sync SHALL NOT re-upload or delete it

### Requirement: Failed encoding preserves the source

The system SHALL NOT delete or overwrite the source photo when encoding fails; the operation SHALL fail without attaching a broken photo.

#### Scenario: Encoding failure is non-destructive
- **WHEN** decoding or encoding a captured or picked photo fails
- **THEN** the source file or image SHALL be left intact
- **AND** no broken photo SHALL be attached to the client
