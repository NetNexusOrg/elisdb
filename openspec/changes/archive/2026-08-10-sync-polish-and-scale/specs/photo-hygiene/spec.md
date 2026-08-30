## Purpose

Keeps the app's photo store clean: photo rows whose backing file is missing are removed instead of lingering as permanently-unsynced, and files on disk no longer referenced by any row are reclaimed.

## ADDED Requirements

### Requirement: Phantom photo rows removed

The system SHALL detect photo rows whose backing file is missing and remove them (or mark for deletion) rather than leave them silently unsyncable.

#### Scenario: Missing-file photo is dropped or flushed
- **WHEN** a sync encounters a photo row whose file does not exist
- **THEN** the system SHALL either remove the row or mark it as pending deletion
- **AND** it SHALL NOT remain as a permanently-unsynced row
- **AND** the missing photo SHALL be omitted from the pushed record

### Requirement: Orphaned photo files removed

The system SHALL remove files in the photo directory that are not referenced by any photo row (leftovers from interrupted syncs or failed deletes).

#### Scenario: Sweep removes orphans
- **WHEN** targeted sweep runs and a file exists that no photo row references
- **THEN** the file SHALL be deleted

#### Scenario: Sweep is bounded per run
- **WHEN** the sweep is triggered (after a successful pull or on app start)
- **THEN** it SHALL process at most a bounded batch of orphans per run, deferring the rest to the next sweep

#### Scenario: Sweep never races an in-flight pull
- **WHEN** a pull is materializing photos
- **THEN** the orphan sweep SHALL wait until materialization completes before deleting anything