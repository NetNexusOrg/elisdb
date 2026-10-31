## ADDED Requirements

### Requirement: Attach images and PDFs to a client

The client form SHALL provide a "Mapa de dispositivos" section where the user can
add several attachments to the client record, choosing images from the gallery or
the camera and PDF files from the device storage, see which attachments are
already chosen, and remove them before saving.

#### Scenario: Add an image

- **WHEN** the user adds an image to the device map
- **THEN** the attachment SHALL appear in the list with its name and type
- **AND** SHALL be stored with the client record when the form is saved

#### Scenario: Add a PDF

- **WHEN** the user picks a PDF file for the device map
- **THEN** the file SHALL be stored byte for byte, without being re-encoded
- **AND** SHALL appear in the list with its name and type

#### Scenario: Remove an attachment

- **WHEN** the user removes an attachment before saving
- **THEN** it SHALL disappear from the list
- **AND** its file SHALL be deleted once the record is saved

#### Scenario: Attachments survive reopening the record

- **WHEN** the user opens an existing client that has attachments
- **THEN** the section SHALL list them in the order they were added

### Requirement: Reject unsupported attachments

The app SHALL refuse an attachment whose type is not an image or a PDF, or whose
size exceeds the server's limit, without changing the client record, and SHALL
tell the user why.

#### Scenario: Unsupported type rejected

- **WHEN** the user picks a file that is not an image or a PDF
- **THEN** the attachment SHALL NOT be added
- **AND** the screen SHALL report the unsupported type

#### Scenario: Oversized file rejected

- **WHEN** the chosen file exceeds the attachment size limit
- **THEN** the attachment SHALL NOT be added
- **AND** the screen SHALL report the size limit

### Requirement: Open the device map from the client detail

The client detail SHALL show the attachments of the device map, opening images in
the app's viewer and PDFs in an application that can display them.

#### Scenario: Image opens in the viewer

- **WHEN** the user taps an image attachment
- **THEN** the image SHALL open in the app's photo viewer

#### Scenario: PDF opens in an external reader

- **WHEN** the user taps a PDF attachment
- **THEN** the app SHALL hand the file to an external application that handles
  `application/pdf` with a temporary read permission

#### Scenario: Missing file is reported

- **WHEN** the attachment file is no longer on the device
- **THEN** the screen SHALL report that the file is unavailable
- **AND** SHALL NOT open an empty viewer

### Requirement: Device map syncs with the client

The attachments SHALL travel with the client record through push and pull, using
content identity so that unchanged files are neither uploaded nor downloaded
again, and SHALL be preserved when a duplicate client is merged.

#### Scenario: New attachment is uploaded

- **WHEN** a client with a new attachment is pushed
- **THEN** the file SHALL be uploaded before the record
- **AND** the server SHALL hold its bytes after the push

#### Scenario: Unchanged attachment is not uploaded

- **WHEN** a client is pushed again and an attachment's content is unchanged
- **THEN** that file SHALL NOT be uploaded again

#### Scenario: Missing attachment is retried

- **WHEN** the server reports an attachment as missing after a push
- **THEN** the app SHALL NOT record it as uploaded
- **AND** SHALL upload it again on the next sync

#### Scenario: Attachment from another device is stored

- **WHEN** a pull returns a client with an attachment the device does not have
- **THEN** the app SHALL download and store it
- **AND** SHALL show it in the client detail

#### Scenario: Duplicate merge keeps attachments

- **WHEN** a push is answered as a duplicate and the records are merged
- **THEN** the attachments of both records SHALL remain available under the
  canonical client
