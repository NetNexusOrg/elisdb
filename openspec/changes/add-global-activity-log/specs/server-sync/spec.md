# Spec Delta

## ADDED Requirements

### Requirement: Global edit history endpoint

The server SHALL expose an authenticated endpoint returning audit entries for all clients, newest first, each carrying the client identifier, the client name when known, the actor, the timestamp, the action and the recorded change details. It SHALL accept an optional `limit` (1 to 200, defaulting to 50) and an optional `before_id` cursor, and SHALL return a `next_before_id` value when older entries remain.

#### Scenario: History returned newest first
- **WHEN** an authenticated client requests the global history
- **THEN** the server SHALL return audit entries ordered by id descending
- **AND** the most recent change SHALL be first

#### Scenario: Entry identifies its client
- **WHEN** a global history entry is returned
- **THEN** it SHALL include the client identifier
- **AND** it SHALL include the client's current name when the client still exists

#### Scenario: Deleted client keeps its recorded name
- **WHEN** a global history entry refers to a deleted client
- **THEN** the entry name SHALL come from the deletion record's client snapshot
- **AND** the entry SHALL still be returned

#### Scenario: Paginated global history returns one page
- **WHEN** the app requests the global history with a `limit`
- **THEN** the server SHALL return at most `limit` entries
- **AND** SHALL include `next_before_id` when older entries exist

#### Scenario: Next page continues before the cursor
- **WHEN** the app requests the next page using `next_before_id`
- **THEN** the server SHALL return only entries with an id smaller than that cursor
- **AND** SHALL NOT repeat entries from the previous page

#### Scenario: Last global history page has no cursor
- **WHEN** no older entries remain after the requested page
- **THEN** the response SHALL carry no `next_before_id`
- **AND** the app SHALL treat the activity log as fully loaded

#### Scenario: Default page size is bounded
- **WHEN** the app requests the global history without a limit
- **THEN** the server SHALL apply its default page size instead of returning every entry

#### Scenario: Invalid global history limit is rejected
- **WHEN** the app requests a global history limit below 1 or above the maximum
- **THEN** the server SHALL reject the request with a client-error response

#### Scenario: Authentication is required
- **WHEN** an unauthenticated request asks for the global history
- **THEN** the server SHALL reject it like the other authenticated endpoints

#### Scenario: Entries without details are still returned
- **WHEN** an audit entry was recorded before change details existed
- **THEN** it SHALL appear in the global history with a null details value
