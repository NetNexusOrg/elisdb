# Tasks

## 1. Server
- [x] 1.1 Write `user_name` in the issued configuration file (`app/keys.py`)
- [x] 1.2 Cover the file content and `0600` permissions with a unit test (`tests/test_keys.py`)

## 2. Android
- [x] 2.1 Parse the optional `user_name` in `ServerConfigDto`
- [x] 2.2 Persist the user name in `SettingsStore` (encrypted prefs + plain-text migration) and clear it with the rest of the configuration
- [x] 2.3 `ServerConfigViewModel`: resolve the identity from the file on init/import; keep the server lookup as the fallback when the file has no name
- [x] 2.4 Remove the unnecessary round trip after importing a file that carries the name

## 3. Tests
- [x] 3.1 Unit: name from the file is shown without calling the server; import stores the name; existing lookup/fallback tests still pass
- [x] 3.2 Instrumented: importing a file with `user_name` shows the user in the server card

## 4. Docs and spec
- [x] 4.1 Update `project-context` skill (config file fields)
- [x] 4.2 OpenSpec deltas for `client-key-import` and `server-key-provisioning`; validate with `--strict`
- [x] 4.3 Verify on the emulator against the live server with a re-issued configuration
