# Spec Delta

## ADDED Requirements

### Requirement: Prompt for a newer build when the app opens

On launch, the app SHALL check the server for the newest published build when a
server and key are configured. If that build's version code is greater than the
installed one, the app SHALL show a modal prompt identifying the new version and
offering **Atualizar** and **Adiar**, without blocking background work.

#### Scenario: Newer build is offered at launch
- **WHEN** the app opens with a configured server and the server publishes a version code greater than the installed one
- **THEN** a prompt SHALL identify the new version
- **AND** offer the actions **Atualizar** and **Adiar**

#### Scenario: Updating opens the About screen
- **WHEN** the prompt is showing and the user taps **Atualizar**
- **THEN** the prompt SHALL close
- **AND** the app SHALL open the About screen, where the package can be downloaded and installed
- **AND** the prompt SHALL NOT reappear during this launch

#### Scenario: Postponing silences the prompt until the next launch
- **WHEN** the prompt is showing and the user taps **Adiar**
- **THEN** the prompt SHALL close
- **AND** it SHALL NOT reappear for that version during this launch
- **AND** it SHALL be offered again on the next launch if the version is still newer

#### Scenario: No prompt is shown when there is nothing to install
- **WHEN** the app opens and the installed build is up to date, no release is published, the server is not configured, the device is offline, or the check fails
- **THEN** no prompt SHALL be shown
- **AND** the app SHALL continue to its normal start screen

#### Scenario: Prompt does not duplicate the About screen
- **WHEN** the user is already on the About screen
- **THEN** the launch prompt SHALL NOT be shown
- **AND** the About screen SHALL keep its own update card behavior
