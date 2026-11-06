# Spec Delta

## MODIFIED Requirements

### Requirement: CI workload separation

The Android CI SHALL run unit tests, lint and a debug build on every relevant push and pull request, SHALL run the Room migration test as a blocking release gate, SHALL run an automated sync smoke test as a blocking release gate, and SHALL run the full instrumented suite only on pull requests and manual dispatches. The sync smoke test SHALL exercise push, pull, and delete against a disposable server with a freshly provisioned key. Tag releases SHALL require the fast checks, the Room migration gate, and the sync smoke gate.

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

#### Scenario: Sync smoke failure blocks release
- **WHEN** the push, pull, or delete smoke check fails for a release tag
- **THEN** the release job SHALL NOT run

#### Scenario: Sync smoke success allows release
- **WHEN** the fast checks, Room migration gate, and sync smoke gate all pass for a release tag
- **THEN** the release build and GitHub Release SHALL proceed
