# Spec Delta

## MODIFIED Requirements

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
