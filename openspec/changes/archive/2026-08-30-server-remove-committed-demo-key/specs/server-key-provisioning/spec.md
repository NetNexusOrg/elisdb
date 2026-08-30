## MODIFIED Requirements

### Requirement: Example configs contain no live secrets

Committed example or demo configuration files (such as `demo.config.json` / `demo.config.example.json`) SHALL NOT contain a functional `private_seed`; they SHALL carry only placeholders (e.g. server URL, empty or example-only fields) so importing them yields a non-authenticating, clearly non-functional config. Real, working private seeds SHALL exist only at runtime (produced by the issue/provisioning flow) and SHALL be excluded from version control via `.gitignore`.

#### Scenario: Committed demo config has no working seed
- **WHEN** an operator inspects a demo/example config file tracked in the repository
- **THEN** the file SHALL NOT contain a functional `private_seed`
- **AND** importing it SHALL NOT authenticate against the server

#### Scenario: Live configs are gitignored
- **WHEN** the issue or provisioning flow writes a configuration file containing a real `private_seed`
- **THEN** that file SHALL be covered by `.gitignore`
- **AND** SHALL NOT be committed to the repository

#### Scenario: Example documents the real flow without a secret
- **WHEN** an operator reads the example config to learn the format
- **THEN** the example SHALL illustrate the fields (server URL, key metadata) without embedding a usable private seed
