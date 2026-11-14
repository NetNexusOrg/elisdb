# server-management-tui — Delta

## ADDED Requirements

### Requirement: Status reports the running mode and data state

The management menu SHALL show, without changing anything, whether the server
is running in Docker, running locally, or stopped, together with the resolved
data directory, its owner and writability, disk usage, record counts and the
number of active keys.

#### Scenario: Server is running in Docker

- **WHEN** the operator opens Status while the container is running
- **THEN** the output shows `docker` as the active mode, the health result,
  the data directory with its owner, and the client/photo/tombstone counts

#### Scenario: Server is stopped

- **WHEN** neither the container nor a local process is running
- **THEN** the output shows `stopped` and still reports the data directory,
  its owner and the record counts read in read-only mode

#### Scenario: Data is not writable by the expected user

- **WHEN** the data directory is not writable by the host uid
- **THEN** Status reports the mismatch and prints the `chown` command needed to
  fix it, without attempting to change ownership itself

### Requirement: Server runs in exactly one mode at a time

The menu SHALL start the server either in Docker or locally, and MUST refuse to
start one mode while the other is running.

#### Scenario: Local start with the container running

- **WHEN** the operator chooses to start locally while the container is up
- **THEN** the menu refuses, explains which mode is active and how to stop it

#### Scenario: Docker start with a local process running

- **WHEN** the operator chooses Docker while a local server process is alive
- **THEN** the menu refuses and reports the local process id

#### Scenario: Stop is confirmed

- **WHEN** the operator chooses to stop the running server
- **THEN** the menu asks for confirmation and, on yes, stops only that mode
  without removing volumes or data

### Requirement: Users are managed explicitly from the menu

The menu SHALL list, add and revoke users, and MUST never print a private seed
or overwrite an existing configuration file.

#### Scenario: Add a user

- **WHEN** the operator adds a user that has no configuration file
- **THEN** a key is issued and `configs/<user>.config.json` is written with mode
  0600, and the menu shows the resulting key id

#### Scenario: Existing configuration file

- **WHEN** the operator adds a user whose configuration file already exists
- **THEN** the menu refuses to overwrite it and suggests a new name

#### Scenario: Revoke the last active key

- **WHEN** the operator tries to revoke the only active key
- **THEN** the menu refuses unless the operator confirms with the exact phrase

#### Scenario: List groups keys by user

- **WHEN** the operator lists users
- **THEN** each user is shown once with its active and revoked key counts and
  whether a configuration file exists

### Requirement: Configuration is validated before it is written

The menu SHALL edit the environment file through validated fields, preserving
comments and unknown keys, and MUST write atomically with a backup.

#### Scenario: Invalid value

- **WHEN** a field is given a value of the wrong type or format
- **THEN** nothing is written and the menu reports the field and the rule

#### Scenario: Apply after saving

- **WHEN** a valid change is saved
- **THEN** the menu shows the diff, asks whether to apply now, and restarts the
  active mode only after confirmation

#### Scenario: Restart fails

- **WHEN** the restarted mode fails its health check
- **THEN** the menu shows the failure and offers to restore the `.env.bak-*`
  backup it created

### Requirement: Management actions are available non-interactively

The entry point SHALL accept flags that perform the same actions without the
menu, for scripts and automation.

#### Scenario: List users from a script

- **WHEN** the entry point is called with `--list-users`
- **THEN** it prints the user list and exits with status zero

#### Scenario: Add a user from a script

- **WHEN** the entry point is called with `--add-user NAME --yes`
- **THEN** the key is issued without prompting and the exit status reflects the
  result
