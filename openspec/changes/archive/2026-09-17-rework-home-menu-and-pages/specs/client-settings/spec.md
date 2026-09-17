## MODIFIED Requirements

### Requirement: Server connection settings screen

The system SHALL provide a server screen that shows the server connection (base URL, masked ed25519 signing key, and associated user name) in read-only, non-editable form, configured by importing the server-issued configuration file, and where the entire configuration can be removed in one action. The connection details SHALL be presented with a consistent visual hierarchy: labels and values in clearly distinct, uniform font sizes, and related fields grouped together so the screen is easy to scan.

#### Scenario: View current server configuration
- **WHEN** the user opens the server screen after importing a configuration file
- **THEN** the screen SHALL show the server base URL, the signing key masked so the full secret is not visible, and the associated user name when known
- **AND** none of these values SHALL be editable

#### Scenario: Configuration is not manually editable
- **WHEN** the user opens the server screen
- **THEN** there SHALL be no field to edit the server base URL and no save action for the connection configuration

#### Scenario: View configuration when no key is set
- **WHEN** the user opens the server screen and no signing key is stored
- **THEN** the screen SHALL indicate that no key is configured

#### Scenario: Associated user is always shown when a key is set
- **WHEN** the user opens the server screen and a signing key is stored
- **THEN** the screen SHALL always show the "associated user" information, even when the identity lookup has not completed yet
- **AND** while the identity lookup is in progress the screen SHALL indicate that the user is being resolved
- **AND** when the lookup succeeds the screen SHALL show the reported user name

#### Scenario: No associated user is shown as absent
- **WHEN** the configured server reports no user name for the stored key
- **THEN** the screen SHALL indicate that no user is associated with the key
- **AND** SHALL NOT fabricate a name

#### Scenario: Unresolvable identity is shown as unavailable, not absent
- **WHEN** a signing key is stored but the identity lookup cannot reach the server or fails (network or server error)
- **THEN** the screen SHALL show that the associated user could not be determined
- **AND** SHALL NOT fabricate a name
- **AND** SHALL NOT claim no user is associated with the key

#### Scenario: Fields use a consistent type hierarchy
- **WHEN** the user views the server connection configuration
- **THEN** every field label SHALL use the same label style and every field value SHALL use the same value style
- **AND** the value of each field SHALL be visually distinct from its label by font size
- **AND** related fields SHALL be grouped together, visually separated from other groups

#### Scenario: Remove the entire server configuration
- **WHEN** the user taps the remove-configuration action for a stored server configuration
- **THEN** the stored base URL and signing key SHALL be removed together
- **AND** the screen SHALL indicate that no server is configured and no key is configured
- **AND** the associated user SHALL no longer be shown

### Requirement: Import configuration from a file

The system SHALL offer an action to import the server configuration file. The server screen SHALL NOT show an explanatory paragraph about how the file is issued.

#### Scenario: Import action is available
- **WHEN** the user opens the server screen
- **THEN** an action to import the server configuration file SHALL be available

#### Scenario: No import instructions paragraph
- **WHEN** the server screen shows the import action
- **THEN** the screen SHALL NOT show a paragraph explaining how the configuration file is emitted

## REMOVED Requirements

### Requirement: Sync status and manual trigger on the settings screen

**Reason**: Synchronization is now a dedicated screen, separate from server configuration.

**Migration**: The sync status, fault messages, and manual trigger are specified by `client-sync` on the dedicated synchronization screen.

### Requirement: Sync action stays in a fixed position

**Reason**: The sync status row moved from the settings screen to the dedicated synchronization screen.

**Migration**: The fixed-position requirement is specified by `client-sync` for the synchronization screen.

### Requirement: Pending sync state shown in settings

**Reason**: Pending synchronization state is now shown on the dedicated synchronization screen, not the settings screen.

**Migration**: The pending-state requirement is specified by `client-sync` for the synchronization screen.
