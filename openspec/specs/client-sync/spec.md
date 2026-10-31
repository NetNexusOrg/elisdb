## Purpose

Enables the app to exchange client and photo data with the server's push/pull/delete API over authenticated HTTP and coordinates offline-first syncing: queue local changes, transmit pending records when online, pull the authoritative server state, and apply clears — without the app deciding conflicts, which the server owns.

## Requirements
### Requirement: Push records to the server

The system SHALL send local client and photo records to the server via the push endpoint, returning a per-record status, and SHALL represent unsynced records as base64 photos. Each record SHALL remain pending until the server acknowledges its state, and the acknowledgement SHALL NOT write record content: a record edited while the request was in flight SHALL stay pending with the user's edit intact. Photos the server reports as missing SHALL be uploaded again instead of being remembered as delivered.

#### Scenario: Push returns per-record status
- **WHEN** the app sends a batch of client and photo records to the server
- **THEN** the server response SHALL be interpreted as a per-record status per each pushed client
- **AND** the statuses SHALL be (accepted/duplicate/stale) and returned onward to the caller

#### Scenario: Sending local changes clears pending after ack
- **WHEN** the app has pending (created or edited) client records with their photos and connectivity is available
- **THEN** those records SHALL be sent to the server
- **AND** each SHALL remain pending until the server acknowledges its individual status

#### Scenario: Push failure leaves data intact
- **WHEN** a push cannot complete (no connectivity or transient error)
- **THEN** the pending records SHALL be kept
- **AND** the app SHALL retry later without data loss

#### Scenario: Edit during the push is not overwritten
- **WHEN** the user edits a record while its push is in flight
- **THEN** the edit SHALL be kept
- **AND** the record SHALL remain pending

#### Scenario: Stale ack does not discard the local edit
- **WHEN** the server reports a pushed record as stale and the record was not edited during the push
- **THEN** the app SHALL clear its pending flag
- **AND** the server's version SHALL be applied by the following pull

#### Scenario: Missing photos are pushed again
- **WHEN** the push result names photos the server does not have
- **THEN** the app SHALL NOT record them as delivered
- **AND** SHALL upload them on the next push

### Requirement: Pull the authoritative dataset

The system SHALL fetch all server client and photo records via the pull endpoint, so the caller can reconcile local data with the server, and SHALL apply the returned state as authoritative for records with no pending local edits.

#### Scenario: Pull returns all records with photos
- **WHEN** the app requests a pull
- **THEN** all server-stored clients with their photos SHALL be returned
- **AND** photos SHALL be decoded from base64 to app-private photo files
- **AND** photos whose content is unchanged SHALL be kept as-is without being re-materialized

#### Scenario: Pulled state accepted
- **WHEN** a pull returns the server dataset
- **THEN** records with no local pending edits SHALL be overwritten with the server values

#### Scenario: Pulled creation date preserved
- **WHEN** a pulled server record includes a creation timestamp
- **THEN** the local creation timestamp SHALL be set from the server value, keeping the real creation date even after later edits

#### Scenario: Pull without a creation timestamp keeps local date
- **WHEN** a pulled server record does not include a creation timestamp
- **THEN** the local creation date SHALL be preserved when the record already existed locally, rather than being replaced by the modification timestamp
- **AND** a freshly fetched record without a creation timestamp SHALL fall back to its modification timestamp

### Requirement: Delete a client on the server

The system SHALL send pending deletions to the server and, once the server confirms removal, permanently remove the local record and its photos.

#### Scenario: Delete server client
- **WHEN** the app requests deletion of a client by identifier
- **THEN** the server SHALL remove the client and its photos

#### Scenario: Delete transmitted and finalized
- **WHEN** a record marked pending-deletion is sent to the server and the server confirms removal
- **THEN** the local record SHALL be permanently deleted
- **AND** its pending-deletion marker SHALL not be exposed afterwards

### Requirement: Request authentication

Every request the app makes to the server SHALL carry the server's ed25519 request signature: a fresh timestamp and a signature over the message `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`, using an in-app private key whose matching public key is registered on the server.

#### Scenario: Requests are signed
- **WHEN** the app calls a push, pull, or delete endpoint
- **THEN** the request SHALL include the required auth headers derived from the in-app key and current timestamp
- **AND** a rejected (401) response SHALL be surfaced so the sync layer can report it

#### Scenario: Server configuration is configurable
- **WHEN** the app is configured with a server base URL
- **THEN** all data API requests SHALL be made against that base URL

### Requirement: Sync status exposure

The system SHALL expose a synchronization status (e.g. idle, in-progress, error) and support triggering a manual sync.

#### Scenario: Sync status is observable and manual sync is supported
- **WHEN** a sync runs, fails, or completes
- **THEN** an observable sync status SHALL be updated accordingly
- **AND** the user SHALL be able to trigger a manual sync

#### Scenario: Manually triggered sync is reported
- **WHEN** the user triggers a manual sync from the UI
- **THEN** the sync engine SHALL run a full push/pull synchronisation
- **AND** the observable sync status SHALL transition to in-progress and then to success or error with a result

#### Scenario: Sync result distinguishes configuration faults
- **WHEN** a sync fails because no server base URL is configured or no signing key is available
- **THEN** the sync result SHALL indicate the configuration fault distinctly from a transient network error, so the UI can show an actionable message
- **AND** the observable sync status SHALL be reported as an error

### Requirement: Clock offset measured from the server

The system SHALL measure the difference between the device clock and the server clock, persist it and use it to normalize timestamps so records compare fairly in last-write-wins. The offset SHALL be refreshed from the `Date` header of every server response and at app start, and a signed request rejected as stale SHALL be retried once with a corrected timestamp. A skew of 120 seconds or more SHALL be shown to the user with a correction action.

#### Scenario: Offset measured and applied on a successful sync
- **WHEN** a sync succeeds and the server reported its current time
- **THEN** the device↔server clock offset SHALL be computed and persisted
- **AND** subsequent `updated_at` stamps SHALL use the offset-normalized time

#### Scenario: Offset not available on first sync
- **WHEN** no clock offset has been persisted yet (first ever sync)
- **THEN** the sync SHALL still proceed using the device clock as-is

#### Scenario: Stale offset is refreshed
- **WHEN** a sync runs and the server reports its current time
- **THEN** the persisted clock offset SHALL be refreshed to the newly measured value

#### Scenario: Offset refreshed from every response
- **WHEN** any server response carries a `Date` header
- **THEN** the persisted offset SHALL be updated from it without an extra request

#### Scenario: Offset measured at app start
- **WHEN** the app starts with a server configuration available
- **THEN** the offset SHALL be measured before the first sync of that session

#### Scenario: Stale timestamp retried once
- **WHEN** the server rejects a signed request because the timestamp is stale
- **THEN** the app SHALL correct the offset from the server time
- **AND** repeat the request once with a freshly signed timestamp
- **AND** only report a clock fault if the retry is also rejected

#### Scenario: Invalid key is not retried
- **WHEN** the server rejects a signed request because the key or signature is invalid
- **THEN** the app SHALL report the auth failure without retrying

#### Scenario: Large skew surfaced with a correction action
- **WHEN** the measured device↔server skew is 120 seconds or more
- **THEN** the configuration screen SHALL show how far off the clock is and in which direction
- **AND** offer an action that re-measures the offset immediately
- **AND** the skew warning SHALL disappear once the skew is below the threshold

### Requirement: Stale push surfaced to the user

The system SHALL NOT silently discard a local edit rejected as `stale` by the server; the sync outcome SHALL report which client edits were superseded by a newer server version.

#### Scenario: Stale edit reported
- **WHEN** a pushed record is acknowledged as `stale` (a newer server version exists)
- **THEN** the sync outcome SHALL include the name of the affected client
- **AND** the user SHALL be able to see that the local edit was superseded

### Requirement: Delete competes in last-write-wins

The app SHALL send the deleting device's timestamp with a delete, and SHALL reconcile a `stale` response caused by a tombstone (a newer deletion) the same way it reconciles a superseded edit.

#### Scenario: Delete sends the device timestamp
- **WHEN** the app deletes a client
- **THEN** the delete request SHALL carry the device's current timestamp

#### Scenario: Tombstoned push is reconciled as stale
- **WHEN** the server responds `stale` to a pushed record (including because a tombstone exists)
- **THEN** the app SHALL clear the local pending marker
- **AND** report the record's name in the sync outcome

### Requirement: Pull applies tombstones locally

The app SHALL drop local records whose identifier appears in the pull response's tombstones, unless the record has pending local edits. Tombstones are delivered by delivery timestamp, so a deletion made by a device with a slow clock still reaches devices whose watermark is ahead of the deletion timestamp.

#### Scenario: Tombstoned local record removed
- **WHEN** a pull returns a tombstone for a locally stored record with no pending edits
- **THEN** the app SHALL delete the local record and its photos

#### Scenario: Tombstoned record with pending edits kept
- **WHEN** a pull returns a tombstone for a locally stored record that has pending edits
- **THEN** the app SHALL keep the record locally pending for the next push

#### Scenario: Deletion from a slow device still arrives
- **WHEN** a record is deleted with a timestamp older than the local watermark
- **THEN** the next pull SHALL still return its tombstone

### Requirement: Push omits unchanged photos

The system SHALL allow a push to reference unchanged photos by identifier without retransmitting their base64 content, so editing a client's other fields does not re-upload every photo.

#### Scenario: Unchanged photos sent as references only
- **WHEN** the app pushes a client whose photos are unchanged since the last successful push
- **THEN** the push payload SHALL include those photos as references (id, no data payload)
- **AND** the server SHALL keep its stored photo files for them

#### Scenario: New or changed photos carry data
- **WHEN** the app pushes a client with newly added or modified photos
- **THEN** the push payload SHALL include the base64 content for those photos
- **AND** the server SHALL store their files

#### Scenario: First push sends all photos with data
- **WHEN** the app pushes a client for the first time
- **THEN** every photo SHALL be sent with its base64 content (no server copy exists to reference)

### Requirement: History fetch errors propagate

The system SHALL NOT convert a history fetch failure into an empty result; a failed fetch SHALL be reported so the UI can show an error rather than a misleading "no history".

#### Scenario: Fetch failure surfaces as an error
- **WHEN** the app requests a client's edit history and the request fails
- **THEN** the app SHALL surface the failure (not an empty list)
- **AND** the UI SHALL present it as a load error

### Requirement: Sync is manual only

The system SHALL NOT synchronize automatically on a schedule; data transfers (push/pull) SHALL happen only when the user triggers a sync manually.

#### Scenario: No automatic sync runs
- **WHEN** the app is running and time passes without the user triggering a sync
- **THEN** the app SHALL NOT transfer data to or from the server automatically

#### Scenario: Manual trigger transfers
- **WHEN** the user triggers a sync manually
- **THEN** the app SHALL perform the full push/pull synchronisation

#### Scenario: Opening settings does not transfer
- **WHEN** the user opens the settings screen
- **THEN** the app SHALL check pending uploads and downloads without transferring client/photo data

### Requirement: Pending sync state shown on the synchronization screen

The system SHALL show pending synchronization states in the synchronization screen's sync status row — "synchronization pending" when there are local changes not yet acknowledged by the server, and "updates to receive" when the server holds data newer than this device's last pull — instead of showing a stale success or error from an earlier sync.

#### Scenario: Pending uploads state shown
- **WHEN** the user opens the synchronization screen and the app has pending or deleted-pending records
- **THEN** the sync status row SHALL indicate that a synchronization is pending
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending downloads state shown
- **WHEN** the user opens the synchronization screen and the server holds data newer than the device's last pull
- **THEN** the sync status row SHALL indicate that there are updates to receive
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending state clears after a successful sync
- **WHEN** a sync completes and all pending records are acknowledged and all newer server data is retrieved
- **THEN** the sync status row SHALL show the acknowledged outcome (success) instead of the pending states

#### Scenario: Pending state survives navigation
- **WHEN** the user leaves and reopens the synchronization screen while local changes remain pending or server updates remain unretrieved
- **THEN** the sync status row SHALL still indicate the pending synchronization state

### Requirement: Edit history fetched from the server

The system SHALL fetch a client's edit history from the server, including what changed in each entry, so the app can display who created and edited the client, when, and which fields or photos were affected.

#### Scenario: History loaded for the detail screen
- **WHEN** the user opens the client detail and requests the log
- **THEN** the app SHALL fetch the client's edit history from the server
- **AND** SHALL present the create entry and the edit entries to the user

#### Scenario: History fetch failure is non-blocking
- **WHEN** the server cannot be reached or the history fetch fails
- **THEN** the app SHALL show the detail screen without the log data
- **AND** SHALL inform the user that the history could not be loaded

#### Scenario: History carries change details
- **WHEN** the server returns history entries recorded with change details
- **THEN** the app SHALL keep those details available to the log view
- **AND** SHALL still handle entries returned without details

### Requirement: Pull exhausts pagination

The app SHALL follow the server's pagination cursor returned by the pull endpoint: after each page it SHALL inspect `next_since` (and `next_since_id` when present) and, when a cursor is returned, SHALL request the next page, accumulating records across all pages, before reconciling local data. The app SHALL NOT delete or drop local client records based on a single, partial page of a pull. A local-record sweep (removing clients absent from the server) SHALL run only after the full set of pages has been received, or never for an incremental (delta) pull. The watermark SHALL NOT advance past a record the app deliberately skipped because it had pending edits.

#### Scenario: Multi-page pull is fully consumed
- **WHEN** a pull returns a page together with a `next_since` cursor
- **THEN** the app SHALL request subsequent pages using the cursor
- **AND** SHALL accumulate every returned record before reconciling

#### Scenario: Local records are not dropped on a partial page
- **WHEN** a (full) pull returns only a subset of server records in its first page
- **THEN** the app SHALL NOT delete local records merely because they are absent from that page
- **AND** the app SHALL continue paging until the response carries no `next_since` before any removal sweep

#### Scenario: Watermark advances from the last page
- **WHEN** a paginated pull completes
- **THEN** the app SHALL set its pull watermark from the cursor of the final page

#### Scenario: Skipped record holds the watermark back
- **WHEN** a page contains a record skipped because it has pending edits
- **THEN** the watermark SHALL stay below that record's delivery timestamp
- **AND** the next pull SHALL request it again

### Requirement: HTTP client lifecycle

The app SHALL reuse a single long-lived HTTP client (e.g. one Ktor `HttpClient`) for all requests to the server across the whole process lifetime, and SHALL NOT construct a new HTTP client for each sync, history fetch, or identity lookup. The shared client SHALL be created once (for example in the app container) and closed only when the process terminates.

#### Scenario: Repeated syncs reuse one client
- **WHEN** the app performs several syncs over its lifetime
- **THEN** all of them SHALL use the same underlying HTTP client instance
- **AND** no new client SHALL be constructed per sync

#### Scenario: History and identity fetches share the client
- **WHEN** the app fetches the latest updated-at, a key's identity, or a client's edit history
- **THEN** those requests SHALL use the same shared HTTP client as the sync path

#### Scenario: No client leak across syncs
- **WHEN** many syncs run back to back
- **THEN** the app SHALL NOT accumulate unclosed HTTP clients or connection pools

### Requirement: Server and device client totals shown

The configuration screen SHALL show the number of clients stored on the server alongside the number stored on the device. When the server total is unknown, the screen SHALL show a placeholder instead of a misleading zero. The server total SHALL be refreshed by the check that runs when the screen opens, without transferring client records.

#### Scenario: Both totals are shown
- **WHEN** the user opens the configuration screen with a configured server and a reachable connection
- **THEN** the screen SHALL show how many clients are stored on the server
- **AND** the screen SHALL show how many clients are stored on the device

#### Scenario: Unknown server total shows a placeholder
- **WHEN** the server total has not been retrieved (no configuration, no connection, or never fetched)
- **THEN** the server row SHALL show a placeholder rather than `0`

#### Scenario: Server total refreshes without transferring data
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL refresh the server client total
- **AND** it SHALL NOT push or pull client records

#### Scenario: Server total updates after a sync
- **WHEN** a sync completes successfully
- **THEN** the screen SHALL show the server client total as of that sync

### Requirement: Configuration screen

The system SHALL provide a single configuration screen, reached from the client list menu, combining the sync summary, the data counts and the server configuration: the last successful sync as a caption, device and server client totals side by side, the pending work in the summary's detail line, and the server URL, masked key and associated user, with an import action while unconfigured and only removal once configured.

#### Scenario: Configuration screen is opened from the client list menu
- **WHEN** the user opens the client list menu and taps the configuration action
- **THEN** the system SHALL open the configuration screen
- **AND** the menu SHALL NOT offer separate synchronization and server entries

#### Scenario: Total client count is shown
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL show the total number of clients stored on the device
- **AND** clients pending deletion SHALL NOT be counted
- **AND** the device total SHALL be shown alongside the server total for comparison

#### Scenario: Last successful sync time is shown
- **WHEN** the user opens the configuration screen and at least one sync has completed successfully
- **THEN** the screen SHALL show the date and time of the last successful synchronization as a caption of the sync summary
- **AND** the shown time SHALL be the one persisted from the last successful sync, surviving app restarts

#### Scenario: Never synchronised is shown
- **WHEN** the user opens the configuration screen and no sync has ever completed successfully
- **THEN** the screen SHALL indicate that no synchronization has completed yet

#### Scenario: Pending work is reported by the summary
- **WHEN** there are local records awaiting upload or server changes to receive
- **THEN** the sync summary SHALL name the pending work in its detail line, with the number of records awaiting upload
- **AND** the data card SHALL show only the device and server client totals

#### Scenario: Server configuration is shown
- **WHEN** the user opens the configuration screen and a server configuration is loaded
- **THEN** the screen SHALL show the server URL, the signing key masked, and the user associated with the key
- **AND** the key SHALL NOT be shown in full

#### Scenario: Configuration is imported on the same screen
- **WHEN** the user taps the import configuration action and picks a valid configuration file
- **THEN** the screen SHALL show the imported URL and masked key
- **AND** the system SHALL start a synchronisation

#### Scenario: Configuration can be removed
- **WHEN** the user taps the remove configuration action
- **THEN** the URL and the key SHALL be shown as not configured
- **AND** the stored configuration SHALL be cleared

#### Scenario: Import is not offered once configured
- **GIVEN** a configuration is already present
- **THEN** the server card SHALL offer the removal action and SHALL NOT offer to import another configuration

#### Scenario: Invalid configuration file is rejected
- **WHEN** the picked file is not a valid configuration
- **THEN** the screen SHALL show an error explaining the expected format
- **AND** the current configuration SHALL be kept

### Requirement: Sync summary and manual sync control

The sync summary SHALL be the screen's prominent block: a state icon, a short state title, and a detail line naming the pending work. A single full-width manual sync control SHALL be the only sync action; when no server is configured the summary SHALL state that synchronization is not available instead of offering an action.

#### Scenario: Sync status is shown
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL show the current sync status as an icon and a short title, formatted more prominently than the surrounding detail rows
- **AND** when a sync is in progress the title SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the title SHALL indicate that the data is synchronized
- **AND** when a sync has just failed the title SHALL indicate the failure

#### Scenario: Pending work is summarised in one line
- **WHEN** the screen shows pending uploads, pending downloads, or both
- **THEN** the sync summary SHALL show a detail line with what is pending (the number of changes to send and/or the availability of updates to receive)
- **AND** the screen SHALL NOT need to be read row by row to know whether a sync is worth running

#### Scenario: A single sync action is offered
- **WHEN** the configuration screen is shown
- **THEN** the screen SHALL offer a single, full-width manual sync control
- **AND** SHALL NOT offer a separate check-for-updates action
- **AND** the manual sync control SHALL perform the data transfer

#### Scenario: Prominent manual sync control
- **WHEN** the configuration screen is shown
- **THEN** a full-width manual sync control SHALL be available
- **AND** while a sync is in progress the control SHALL indicate that a sync is in progress and SHALL NOT start a second synchronisation

#### Scenario: No configuration shows not configured
- **WHEN** no server configuration is loaded
- **THEN** the sync summary SHALL indicate that the server is not configured, without requiring a sync attempt
- **AND** it SHALL state that synchronization is not available
- **AND** the import action SHALL be offered only in the server card, as a single entry point
- **AND** no control in the summary SHALL start a synchronisation

#### Scenario: Missing server configuration is shown
- **WHEN** a sync fails because the server is not configured
- **THEN** the sync summary SHALL indicate that the server is not configured, using a short message ("Não configurado")

#### Scenario: Nothing to sync is shown
- **WHEN** the configuration screen is opened, a server configuration is loaded, and there are no pending uploads and no pending downloads
- **THEN** the sync summary SHALL indicate that the data is synchronized

#### Scenario: Send-only is shown
- **WHEN** the configuration screen is opened and there are pending uploads but no pending downloads
- **THEN** the sync summary SHALL indicate that there are changes to send

#### Scenario: Receive-only is shown
- **WHEN** the configuration screen is opened and there are pending downloads but no pending uploads
- **THEN** the sync summary SHALL indicate that there are updates to receive

#### Scenario: Send and receive is shown
- **WHEN** the configuration screen is opened and there are both pending uploads and pending downloads
- **THEN** the sync summary SHALL indicate that there are changes to send and updates to receive

#### Scenario: Sync fault messages are actionable
- **WHEN** a sync has failed
- **THEN** the configuration screen SHALL show a message explaining the failure (missing server configuration, unregistered key, or network problem) in the same place as the sync status

#### Scenario: Manual sync from the configuration screen
- **WHEN** a sync is not in progress and the user taps the full-width manual sync control
- **THEN** the system SHALL start a synchronisation
- **AND** the screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the full-width manual sync control while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

### Requirement: Pending state refreshed on open

When the configuration screen opens, the system SHALL refresh the pending-upload and pending-download indicators and the server client total by querying the server without transferring data.

#### Scenario: Pending state is checked when the screen opens
- **WHEN** the user opens the configuration screen
- **THEN** the system SHALL query the server for the pending indicators and the server client total
- **AND** the system SHALL NOT transfer client records while doing so
