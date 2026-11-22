# Spec Delta

## ADDED Requirements

### Requirement: Server persists each photo's profile flag

The server SHALL persist, for each photo of a client, whether it is the client's
profile photo, and SHALL return that flag in the photo metadata of pulls.

#### Scenario: Push stores the profile flag
- **WHEN** an accepted push marks one photo as the client's profile photo
- **THEN** the server SHALL store that flag with the photo's row

#### Scenario: Pull returns the profile flag
- **WHEN** a stored client's photos are pulled
- **THEN** each photo's metadata SHALL include whether it is the profile photo

#### Scenario: Only one photo is marked per client
- **WHEN** a push marks more than one photo as profile
- **THEN** the server SHALL keep only one marked, choosing the first by gallery
  order

#### Scenario: Flag survives a schema upgrade
- **WHEN** a database created before this feature is opened
- **THEN** the profile-flag column SHALL be added without removing or orphaning
  any photo row
- **AND** existing photos SHALL start unmarked

### Requirement: Pushes from older clients preserve the profile choice

The server SHALL distinguish a push that does not mention the profile flag from a
push that explicitly clears it, so an older client cannot erase a choice made by
a newer one.

#### Scenario: Missing flag preserves the stored value
- **WHEN** a push omits the profile flag for a photo the server already stores
- **THEN** the server SHALL keep the stored profile flag unchanged

#### Scenario: Explicit false clears the flag
- **WHEN** a push sends the profile flag as false for the marked photo
- **THEN** the server SHALL clear it

#### Scenario: New photo from an older client starts unmarked
- **WHEN** a push adds a photo without the profile flag
- **THEN** the server SHALL store the new photo as not being the profile photo
