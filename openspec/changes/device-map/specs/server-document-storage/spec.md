## ADDED Requirements

### Requirement: Documents are stored beside photos

The server SHALL store the documents attached to a client in the same directory
and through the same atomic write path as photos, marking each row with its kind
so that photos and documents can be listed separately, and SHALL remove document
files when their row is deleted.

#### Scenario: Document stored with its row

- **WHEN** a push references a document whose file was uploaded
- **THEN** the server SHALL keep one row for it, with its mime, order and name
- **AND** the file SHALL remain readable at its stored path

#### Scenario: Photos and documents never mix

- **WHEN** a client has both photos and documents
- **THEN** the pull response SHALL list them as two separate lists
- **AND** a document SHALL NOT appear among the photos

#### Scenario: Path traversal is rejected

- **WHEN** a document identifier tries to escape the storage directory
- **THEN** the server SHALL reject it with an error
- **AND** SHALL NOT write outside the directory

#### Scenario: Deleting the client removes the files

- **WHEN** a client with documents is deleted
- **THEN** its document rows SHALL be removed with the client
- **AND** their files SHALL be removed after the transaction commits

### Requirement: Authenticated document upload and download

The server SHALL expose authenticated endpoints to upload and download a
client document, answering with the stored mime type and refusing identifiers
that do not belong to a document.

#### Scenario: Upload then download

- **WHEN** a signed `PUT` stores a PDF and a signed `GET` asks for the same id
- **THEN** the download SHALL return the exact bytes
- **AND** the content type SHALL be the stored mime type

#### Scenario: Signature required

- **WHEN** a request reaches the document endpoints without a valid signature
- **THEN** the server SHALL reject it with 401

#### Scenario: Photo is not served as a document

- **WHEN** a signed request asks for a document id that belongs to a photo
- **THEN** the server SHALL answer 404

#### Scenario: Oversized upload is refused

- **WHEN** an upload exceeds the attachment size limit
- **THEN** the server SHALL refuse it with 413
- **AND** SHALL NOT store a partial file

### Requirement: Documents travel with the client record

The server SHALL accept a document list with each pushed client, keep it in sync
with the record, report documents whose file is missing, and return each
document's hash on pull so the client can skip unchanged files.

#### Scenario: Push stores the documents

- **WHEN** a push carries two documents with files already uploaded
- **THEN** the record SHALL keep both, in the given order

#### Scenario: Missing document reported

- **WHEN** a push references a document whose file does not exist on the server
- **THEN** the response SHALL report it as missing
- **AND** SHALL NOT create a row for it

#### Scenario: Pull returns the document hash

- **WHEN** a pull returns a client with documents
- **THEN** each document SHALL carry its identifier, name, mime, order and
  content hash

#### Scenario: Unsupported type rejects the record

- **WHEN** a push declares a document whose mime type is not an image or a PDF
- **THEN** the whole record SHALL be rejected with an error status
- **AND** the rest of the batch SHALL still be processed

### Requirement: Document files are garbage collected

The server's cleanup pass SHALL treat document files like photo files: a file in
the storage directory with no row SHALL be removed, and a file still referenced by
a document SHALL be kept.

#### Scenario: Unreferenced document file is removed

- **WHEN** the cleanup pass runs and a document file has no row
- **THEN** that file SHALL be deleted

#### Scenario: Referenced document file is kept

- **WHEN** the cleanup pass runs and a document file is referenced by a row
- **THEN** the file SHALL remain on disk

### Requirement: Document changes are recorded in the history

A push that adds or removes documents SHALL leave an audit entry naming what
changed, so a documents-only edit is visible in the client history.

#### Scenario: Adding a document is recorded

- **WHEN** a push adds a document to an existing client
- **THEN** the history SHALL gain an entry naming the added document

#### Scenario: Removing a document is recorded

- **WHEN** a push removes a document from an existing client
- **THEN** the history SHALL gain an entry naming the removed document
