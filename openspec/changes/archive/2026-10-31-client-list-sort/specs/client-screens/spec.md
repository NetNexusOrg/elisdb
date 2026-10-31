# Spec Delta

## ADDED Requirements

### Requirement: Client list ordering

The client list SHALL offer an alphabetical ordering, a most-recent-first
ordering and a last-edited-first ordering, selectable from the same menu as the
status filter. The alphabetical ordering SHALL be the default. Ordering SHALL be
applied after the status filter and the search term, and SHALL NOT change the
stored data.

#### Scenario: Alphabetical order by default
- **WHEN** the client list is shown without any ordering choice
- **THEN** the clients SHALL be listed in alphabetical order by name

#### Scenario: Most recent first
- **WHEN** the user selects the most-recent-first ordering
- **THEN** the clients SHALL be listed with the most recently created client first
- **AND** clients created at the same time SHALL be ordered alphabetically

#### Scenario: Most recently edited first
- **WHEN** the user selects the last-edited-first ordering
- **THEN** the clients SHALL be listed with the most recently modified client first
- **AND** clients modified at the same time SHALL be ordered by creation date and then alphabetically

#### Scenario: Ordering respects the active filter and search
- **WHEN** the user changes the ordering while a status filter or a search term is active
- **THEN** the list SHALL keep only the matching clients
- **AND** SHALL present them in the chosen order

#### Scenario: Ordering options are reachable from the filter menu
- **WHEN** the user opens the filter menu
- **THEN** the menu SHALL show the status options and the ordering options
- **AND** the current ordering SHALL be marked as selected
