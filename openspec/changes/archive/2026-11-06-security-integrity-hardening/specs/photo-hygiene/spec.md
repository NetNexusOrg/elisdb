# Spec Delta

## ADDED Requirements

### Requirement: Staged photos are protected from the sweep

The orphan sweep SHALL NOT delete a photo file that belongs to a staged or recently captured photo which has not yet been persisted as a photo row. Staging and capture SHALL either register the file as protected or hold the same synchronization boundary as the sweep, and the sweep SHALL apply a minimum age before deleting an unreferenced file.

#### Scenario: Staged photo survives a sweep
- **WHEN** the user stages a photo and a sync or app-start sweep runs before the photo is saved
- **THEN** the sweep SHALL NOT delete the staged file
- **AND** saving the client SHALL still find the photo

#### Scenario: Recent unreferenced file is not deleted immediately
- **WHEN** an unreferenced file is newer than the configured grace period
- **THEN** the sweep SHALL skip it
- **AND** a later sweep SHALL remove it if it is still unreferenced

#### Scenario: Old orphan is still removed
- **WHEN** an unreferenced file is older than the grace period and not staged
- **THEN** the sweep SHALL delete it as before
