# Spec Delta

## ADDED Requirements

### Requirement: Configuration change cancels the in-flight sync

Clearing or replacing the server configuration SHALL cancel and await any in-flight synchronization before resetting the watermark, photo hashes, clock offset and sync status. A sync started under the previous configuration SHALL NOT publish success, advance the watermark, or restore state after the reset.

#### Scenario: Config reset waits for the active sync
- **WHEN** the user clears or imports a configuration while a sync is running
- **THEN** the app SHALL cancel that sync and wait for it to finish
- **AND** SHALL only then reset the sync state

#### Scenario: Old sync cannot overwrite the new state
- **WHEN** a sync from the previous configuration finishes after the reset
- **THEN** it SHALL NOT write the watermark, photo hashes, clock offset or a success status
- **AND** the next sync SHALL use the new configuration
