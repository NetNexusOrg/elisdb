## Purpose

Converts a Memento Database CSV export into the data format used by the elisdb server, producing an import file without touching the app, the server, or the production database. It validates coordinates, resolves conflicting coordinate/name cases, and emits a detailed report of every decision it makes.

## ADDED Requirements

### Requirement: Parse the Memento CSV export

The system SHALL read `ELIS.csv` and parse the columns `Rota`, `PDL`, `Nome`, `Coordenadas`, `Notas`, `Foto`, `Plantas`. It SHALL map `PDL` to contract, `Nome` to name, `Coordenadas` to latitude/longitude, `Notas` to notes, and SHALL ignore `Rota`, `Foto` and `Plantas`.

#### Scenario: A normal row maps fields correctly
- **WHEN** a row has a name, contract, coordinates and notes
- **THEN** the output SHALL carry the mapped contract, name, latitude/longitude and notes

#### Scenario: Empty optional fields map to NULL
- **WHEN** a row has an empty contract or empty notes
- **THEN** the output SHALL store NULL for the missing field, not an empty string

### Requirement: Parse coordinates robustly

The system SHALL extract all numeric values from the `Coordenadas` field (not assume exactly two), interpret them as latitude/longitude pairs, and only accept fields with exactly two numbers (one pair). Fields with more than two numbers (multiple pairs) or an odd number of values SHALL be handled as conflicts or parse errors, never guessed.

#### Scenario: Exactly two numbers is a valid coordinate
- **WHEN** a `Coordenadas` field contains exactly two numbers
- **THEN** the first SHALL be used as latitude and the second as longitude

#### Scenario: More than two numbers is a conflict (TIPO A)
- **WHEN** a `Coordenadas` field contains more than two numbers in complete pairs
- **THEN** the row SHALL be flagged as a multi-coordinate conflict and resolved by the chosen strategy (see design)

#### Scenario: Odd or malformed numbers is a parse error
- **WHEN** a `Coordenadas` field contains an odd number of numbers or an otherwise invalid format
- **THEN** the row SHALL be flagged as a parse error and SHALL NOT produce a client

### Requirement: Resolve duplicate names (TIPO B)

The system SHALL detect rows whose names collide under case-insensitive, whitespace-insensitive comparison (UNIQUE COLLATE NOCASE), and collapse each group into a single client using the chosen coordinate/notes resolution strategy.

#### Scenario: Duplicate names collapse to one client
- **WHEN** several rows share the same normalized name
- **THEN** the output SHALL contain exactly one client for that name
- **AND** no duplicate name SHALL remain in the final output

#### Scenario: Each resolved duplicate is reported
- **WHEN** a duplicate-name group is collapsed
- **THEN** the report SHALL list the name and the conflicting coordinates that were considered

### Requirement: Generate an import file

The system SHALL produce an import file compatible with the server schema, containing clients with generated TEXT ids, contract, name, latitude/longitude, notes, `updated_at` and `created_at`.

#### Scenario: Output uses the server schema shape
- **WHEN** the script finishes successfully
- **THEN** an import file SHALL be produced whose records match the server schema (clients; photos are out of scope for this import)

#### Scenario: Ids are unique and consistent
- **WHEN** clients are written
- **THEN** each client SHALL have a unique TEXT id consistent with the app's id convention

### Requirement: Final report

The system SHALL emit a report listing: total rows processed, rows ignored, warnings, parse errors, TIPO A cases resolved, TIPO B cases resolved, and each individual resolved conflict with its chosen coordinates.

#### Scenario: Report enumerates decisions
- **WHEN** the migration completes
- **THEN** the report SHALL list every resolved conflict individually (not only counts), including which coordinates were chosen for each
