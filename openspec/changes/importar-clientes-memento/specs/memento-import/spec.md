## ADDED Requirements

### Requirement: Single interactive import command

The system SHALL provide a single executable command, `./elisdb-memento/importar`, that imports clients from the Memento export into the ElisDB server. The command SHALL NOT require options, flags, or non-interactive modes. It SHALL use the fixed paths `elisdb-memento/ELIS.csv`, `elisdb-memento/memento-import.config.json`, `elisdb-memento/memento-import-report.txt`, and the optional `elisdb-memento/memento-import-exclude.txt`.

#### Scenario: Operator runs the import
- **WHEN** the operator runs `./elisdb-memento/importar`
- **THEN** the command SHALL read the fixed CSV and config paths and start the interactive import flow
- **AND** the command SHALL NOT require `--dry-run`, `--yes`, `--csv`, `--config`, or any other flag

#### Scenario: CSV is missing
- **WHEN** `elisdb-memento/ELIS.csv` does not exist
- **THEN** the command SHALL explain which path is missing and exit without contacting the server or writing data

### Requirement: Dedicated authenticated access

The system SHALL authenticate to the server with an ed25519 key stored in `memento-import.config.json` with `server_url`, `private_seed`, and optionally `user_name`. The config SHALL be readable only by its owner (permissions `600`). The system SHALL NOT print, log, or include the private seed in the report.

#### Scenario: First local run creates the key
- **WHEN** the config is missing and Docker is available
- **THEN** the system SHALL create a dedicated `memento-import` key through the server's Docker container and copy the config to the fixed path
- **AND** the private seed SHALL NOT be printed to the terminal

#### Scenario: Config cannot be created automatically
- **WHEN** the config is missing and the key cannot be created automatically
- **THEN** the system SHALL show the one-time setup commands and exit without importing
- **AND** after the operator places the config at the fixed path, the same command SHALL work

#### Scenario: Config has insecure permissions
- **WHEN** an existing config is readable by other users
- **THEN** the system SHALL attempt to change its permissions to `600` before using it

### Requirement: Read and map the Memento CSV

The system SHALL read `ELIS.csv` as UTF-8 with an optional BOM and map `Nome` to the client name, `PDL` to `contrato`, `Coordenadas` to `latitude`/`longitude`, and `Notas` to `notas`. It SHALL ignore `Rota`, `Foto`, and `Plantas`. Empty contracts and notes SHALL be stored as NULL.

#### Scenario: Normal row is mapped
- **WHEN** a row has a name, PDL, coordinates, and notes
- **THEN** the import SHALL send the mapped contract, name, coordinates, and notes

#### Scenario: Empty coordinates are allowed
- **WHEN** a row has no coordinates
- **THEN** the client SHALL be importable with NULL latitude and longitude

#### Scenario: Parse error is skipped
- **WHEN** a row's coordinate field cannot be parsed into complete pairs
- **THEN** the row SHALL be reported as a parse error and SHALL NOT produce a client

### Requirement: Validate CSV structure before contacting the server

The system SHALL validate the CSV structure before loading the config or contacting the server. It SHALL require `Nome`, `PDL`, `Coordenadas`, and `Notas`, accept `Rota`, `Foto`, and `Plantas`, and warn about unknown columns. It SHALL require at least one data row and one non-empty name. It SHALL enforce a normalized name up to 200 characters and contract/notes up to 100 000 characters.

#### Scenario: Required column is missing
- **WHEN** the CSV does not contain `Nome`, `PDL`, `Coordenadas`, or `Notas`
- **THEN** the system SHALL abort with a non-zero exit code before contacting the server and SHALL NOT write a report

#### Scenario: Field exceeds the server limit
- **WHEN** a normalized name, contract, or notes value exceeds the server limit
- **THEN** the system SHALL abort with a non-zero exit code before contacting the server

#### Scenario: CSV has no usable rows
- **WHEN** the CSV has no data rows or no row with a non-empty name
- **THEN** the system SHALL abort with a non-zero exit code before contacting the server

#### Scenario: Unknown column
- **WHEN** the CSV has a column other than `Nome`, `PDL`, `Coordenadas`, `Notas`, `Rota`, `Foto`, or `Plantas`
- **THEN** the system SHALL warn and continue, ignoring the column

### Requirement: Validate CSV coordinates

The system SHALL validate coordinates during the same safeguard. Empty coordinates SHALL be allowed. Non-empty coordinates SHALL be complete numeric pairs with latitude between -90 and 90 and longitude between -180 and 180. A row whose selected coordinates are malformed or out-of-range SHALL be reported and skipped; the remaining rows MAY be imported.

#### Scenario: Invalid coordinates in one row
- **WHEN** a row's selected coordinates are malformed or out-of-range
- **THEN** the system SHALL report that row and skip it, while allowing the remaining rows to be imported

### Requirement: Additive and idempotent import

The system SHALL compare each CSV client's normalized name with the server's existing clients. A client whose normalized name already exists SHALL be ignored and SHALL NOT be updated, overwritten, or deleted. Re-running the command SHALL only add names that are not already on the server. The import SHALL use the server time for `updated_at` and SHALL send confirmed clients in batches of at most 200 records.

#### Scenario: Existing client is ignored
- **WHEN** the CSV contains a client whose normalized name already exists on the server
- **THEN** the import SHALL skip that client and SHALL NOT change any of its server fields

#### Scenario: Re-running is idempotent
- **WHEN** the operator runs the import again with the same CSV after a successful import
- **THEN** the previously imported clients SHALL be treated as existing and no new records SHALL be created

#### Scenario: Large import is batched
- **WHEN** more than 200 clients are confirmed
- **THEN** the system SHALL send them in multiple push requests of at most 200 records each

### Requirement: Interactive review before import

Before sending, the system SHALL show a summary and write a preview report. It SHALL optionally allow the operator to review TIPO A, TIPO B, and possible duplicate/rename cases.

#### Scenario: Possible duplicate is skipped by default
- **WHEN** a client's name matches an existing client after removing accents/punctuation, or is at least 92% similar with the same non-empty contract
- **THEN** the client SHALL be marked as a possible duplicate/rename
- **AND** it SHALL be skipped by default unless the operator explicitly chooses to include it

#### Scenario: TIPO A/B review
- **WHEN** the CSV contains TIPO A or TIPO B cases
- **THEN** the system SHALL offer to review them before the per-client confirmation
- **AND** the default SHALL accept the automatic resolution

### Requirement: Per-client confirmation

The system SHALL ask for confirmation for each client to import, one at a time, with these actions: `s` to import, `n` or Enter to skip, `v` to view full details, `f` to finish the review and send the clients already confirmed, and `q` to abort without sending anything. Only confirmed clients SHALL be sent. There SHALL NOT be a second "apply all" confirmation.

#### Scenario: Operator skips a client
- **WHEN** the operator answers `n` or presses Enter for a client
- **THEN** that client SHALL NOT be sent
- **AND** the next client SHALL be shown

#### Scenario: Operator finishes early
- **WHEN** the operator answers `f`
- **THEN** the clients confirmed so far SHALL be sent
- **AND** the current client and the remaining clients SHALL be reported as not reviewed

#### Scenario: Operator aborts
- **WHEN** the operator answers `q`
- **THEN** no client SHALL be sent, including clients confirmed earlier in the review
- **AND** the report SHALL record the import as aborted

### Requirement: Report and exclusions

The system SHALL write a local report with the analysis counts, decisions, and per-client result. The report SHALL be written before confirmation as a preview and updated after the run. The system SHALL optionally read a fixed exclusion file; names listed there SHALL NOT be imported. The report and exclusion file SHALL be excluded from Git because they contain client data.

#### Scenario: Report records the result
- **WHEN** the import finishes
- **THEN** the report SHALL list imported, duplicated, skipped, not reviewed, stale, and failed clients

#### Scenario: Excluded client is not imported
- **WHEN** a normalized name is listed in `memento-import-exclude.txt`
- **THEN** that client SHALL NOT be sent to the server

### Requirement: Failure handling

The system SHALL abort before sending if the CSV cannot be read, the server time cannot be obtained, the clients cannot be listed, or authentication fails. It SHALL report per-record `accepted`, `duplicate`, `stale`, and `error` statuses. It SHALL ask before retrying a network failure. It SHALL return a non-zero exit code when any confirmed client is not imported successfully, while treating `duplicate` as an already-existing client rather than a failure.

#### Scenario: Authentication fails
- **WHEN** the server rejects the key while listing clients or pushing
- **THEN** the system SHALL explain that the key was not accepted and SHALL NOT silently continue

#### Scenario: Network fails during push
- **WHEN** a push request fails due to a network error
- **THEN** the system SHALL ask whether to retry
- **AND** if the operator declines, the affected clients SHALL be reported as errors and the command SHALL exit non-zero

#### Scenario: Server reports a duplicate during push
- **WHEN** the server returns `duplicate` for a client that became existing after the initial comparison
- **THEN** the system SHALL report it as an existing client and SHALL NOT treat it as an error
