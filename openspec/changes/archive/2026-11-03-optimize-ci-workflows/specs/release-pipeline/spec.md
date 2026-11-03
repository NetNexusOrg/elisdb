# Spec Delta

## ADDED Requirements

### Requirement: CI workload separation

The Android CI SHALL run unit tests, lint and a debug build on every relevant
push and pull request, SHALL run the Room migration test as a blocking release
gate, and SHALL run the full instrumented suite only on pull requests and manual
dispatches. Tag releases SHALL require the fast checks and the Room migration
gate.

#### Scenario: Pull request runs the full suite
- **WHEN** a pull request targets main
- **THEN** the full instrumented suite SHALL run

#### Scenario: Main push and tag skip the full suite
- **WHEN** a commit is pushed to main or a release tag is pushed
- **THEN** the full instrumented suite SHALL NOT run
- **AND** the fast checks and Room migration gate SHALL still run

#### Scenario: Room migration failure blocks release
- **WHEN** the Room migration test fails for a release tag
- **THEN** the release job SHALL NOT run

### Requirement: Superseded CI runs are cancelled

Both the Android and Server CI workflows SHALL cancel in-progress runs for the
same workflow and ref when a newer run starts.

#### Scenario: New run supersedes old run
- **WHEN** a newer run starts for the same workflow and ref
- **THEN** the older in-progress run SHALL be cancelled
