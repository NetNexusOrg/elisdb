# Spec Delta — client-update

## ADDED Requirements

### Requirement: Update downloads accept only plain APK filenames

The app SHALL accept the release filename from the server only when it is a
single path segment (no `/` or `\`, not `.` or `..`) and ends with `.apk`
(case-insensitive). Otherwise it SHALL refuse the download before writing any
file, so a rogue or compromised server cannot escape the update cache directory.

#### Scenario: Traversal filename is rejected

- **WHEN** the published release filename is `../evil.apk`
- **THEN** the app SHALL NOT write the file
- **AND** it SHALL report a download failure

#### Scenario: Plain APK filename is accepted

- **WHEN** the published release filename is `elisdb-1.13.0.apk`
- **THEN** the app SHALL download it into the updates cache directory
- **AND** it SHALL verify its size and SHA-256 as before
