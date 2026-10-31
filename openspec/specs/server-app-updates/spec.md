# server-app-updates Specification

## Purpose

TBD - Update Purpose after archive.

## Requirements

### Requirement: Release discovered from the published package

The server SHALL discover the current release from the files present in its release directory, whose names follow `elisdb-<versionName>-<versionCode>.apk`, select the highest version code when several are present and ignore files with other names.

#### Scenario: Newest build selected
- **WHEN** the release directory contains several files matching the convention
- **THEN** the reported version SHALL be the one with the highest version code

#### Scenario: Unexpected file names ignored
- **WHEN** the directory contains a file that does not match the convention
- **THEN** that file SHALL NOT be reported as a release

#### Scenario: No release published
- **WHEN** the directory is missing or contains no valid release file
- **THEN** the server SHALL report that no release is available

### Requirement: Authenticated release metadata and download

The server SHALL expose authenticated endpoints that report the release version name, version code, size, SHA-256 digest and filename, and that serve the bytes of the discovered package with the Android package media type.

#### Scenario: Metadata matches the package
- **WHEN** a signed request asks for the latest release
- **THEN** the response SHALL carry the version name and code from the filename
- **AND** the size and SHA-256 of the published file

#### Scenario: Download serves the discovered package
- **WHEN** a signed request asks for the download
- **THEN** the response SHALL carry the bytes of the file reported as the latest release
- **AND** the digest of those bytes SHALL match the reported SHA-256

#### Scenario: Signature required
- **WHEN** a request reaches the release endpoints without a valid signature
- **THEN** the server SHALL reject it with 401

#### Scenario: No release requested
- **WHEN** no release is published and the endpoints are called
- **THEN** both SHALL answer 404
