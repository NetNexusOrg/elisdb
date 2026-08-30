## REMOVED Requirements

### Requirement: Example configs contain no live secrets
**Reason**: The only committed example/demo configuration file (`demo.config.example.json`) is being removed as part of the Docker-first cleanup. With no example config tracked in the repository, this requirement and its scenarios no longer describe any artifact that exists.
**Migration**: None — the security goal is preserved by the absence of any committed example config and by the existing `.gitignore` rule for `*.config.json` (live configs with real seeds remain excluded from version control).
