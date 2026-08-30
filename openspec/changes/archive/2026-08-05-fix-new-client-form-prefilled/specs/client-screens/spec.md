## MODIFIED Requirements

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), phone (optional), contract (optional), location selection, notes, and a photo management section, plus a save action. A form opened in create mode SHALL start with all fields empty and no location selected. A form opened in edit mode SHALL load the data of the selected client only, regardless of which forms were opened previously.

#### Scenario: Required name validation
- **WHEN** the user tries to save with an empty name
- **THEN** the save SHALL be prevented and the name field SHALL be flagged as required

#### Scenario: Save a new client
- **WHEN** the user fills valid data and taps save in create mode
- **THEN** a new client SHALL be created
- **AND** the user SHALL return to the client list

#### Scenario: Save edits to a client
- **WHEN** the user edits a client and taps save in edit mode
- **THEN** the client SHALL be updated
- **AND** the user SHALL return to the client detail screen

#### Scenario: Create form starts empty after another form was opened
- **WHEN** the user opens a create/edit form, leaves it, and opens the create form again
- **THEN** every field SHALL be empty
- **AND** no location SHALL be selected

#### Scenario: Edit form loads only the selected client
- **WHEN** the user opens the edit form for a client after having previously opened a form for a different client
- **THEN** the form SHALL show the name, phone, contract, notes and location of the selected client only
