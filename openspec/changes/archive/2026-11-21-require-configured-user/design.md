# Design

## Context

The app stores the server URL, the ed25519 signing key and the user name in
`PrefsSettingsStore` (EncryptedSharedPreferences), but `settings()` is a plain
read with no change notification. `MainNavigation` always starts on the client
list, so an app without a credential could still create local records that would
never sync. The configuration screen (`ServerConfigViewModel` + `ConfigScreen`)
already imports and clears the credential, and the server already exposes
`GET /api/keys/me` to resolve the user that owns a key. The update prompt from
the previous change is mounted at the Navigation root and stays independent of
the gate.

## Goals / Non-Goals

**Goals:**

- Make a complete configuration (URL, key and user name) a precondition for the
  operational screens, checked at launch and after every credential change.
- Recover the user name automatically for credentials emitted before the name
  was included.
- Keep the configuration screen reachable so the user can always fix the state.

**Non-Goals:**

- Verifying with the server that the key is still registered on every launch
  (that would block offline use and duplicate the sync error reporting).
- Blocking or encrypting existing local records when the credential is removed.
- Server, API, database, version or signing changes.

## Decisions

**Gate at the Navigation root, with the configuration screen taking priority.**
`MainNavigation` reads `container.settings` and chooses between
`ConfigDestination` (when opened from setup), `SetupRequiredScreen` and
`MainDestinations`. Giving `configOpen` priority means the configuration
ViewModel keeps its scope alive after a successful import, so the post-import
`resetSyncState()` + `syncManager.syncNow()` finishes instead of being cancelled
by the screen leaving the composition. The setup screen is not embedded inside
the main back stack; this keeps `NavDisplay` from ever composing the client
list while unconfigured.

**Pure predicates in the settings package.** `hasCredentials()` (URL + key) and
`hasConfiguredUser()` (URL + key + name) live next to `ServerSettings` and are
unit-tested. Sync and update checks keep using `hasCredentials()`; only the gate
requires the name. This prevents a credential with a blank name from unlocking
the app while still letting sync diagnostics work.

**A `StateFlow` in `AppContainer`, not in the `SettingsStore` interface.**
`AppContainer` owns `_settings`, initialized from `settingsStore.settings()` and
refreshed explicitly. `ServerConfigViewModel` receives an `onSettingsChanged`
callback invoked after a successful save or clear; `Navigation` wires it to
`container.refreshSettings()`. Adding a flow to `SettingsStore` would force every
test fake to implement it for a single consumer, and the only writer in main code
is the configuration ViewModel.

**Recover the user name in two places.** At process start,
`AppContainer.recoverUserName()` calls `/api/keys/me` when URL and key exist but
the name is blank and persists the answer. During configuration,
`ServerConfigViewModel.lookupKeyIdentity()` adopts a resolved name only when the
current settings still match the ones captured before the lookup (base URL and
key), so an in-flight lookup cannot attach the wrong user to a newly imported
credential. Both are best-effort: offline leaves the setup screen asking for the
file again, and local records are untouched.

**The update prompt stays at the Navigation root.** `AppUpdatePrompt` is only
composed inside `MainDestinations`, so an incomplete configuration keeps it out
of the composition just like the rest of the operational screens. Its behaviour
(including the per-launch postponement) is unchanged.

**Extract `ConfigDestination`.** The inline `entry<Config>` construction
(`SyncViewModel` + `ServerConfigViewModel` + `ConfigScreen`) becomes a private
composable used by both the gate and the navigation entry, with
`onSettingsChanged = { container.refreshSettings() }`. The About/update wiring is
unchanged.

## Risks / Trade-offs

- **Old credentials without a name can block an offline device** → the setup
  screen explains the missing name; once online, the name is recovered
  automatically, and reimporting the file also works.
- **Strict name requirement** → it is exactly the user's requirement; the
  credential file now carries `user_name`, and the recovery path covers legacy
  files.
- **The configuration ViewModel opened from setup lives in the Activity store**
  → it stays alive after returning to setup; its state is derived from the store,
  so reopening shows the current configuration. Cost is one idle ViewModel.
- **Clearing the credential while on Config unmounts the main back stack** → the
  next successful configuration returns to `ClientList`; no cursor or local data
  is lost, and `resetSyncState()` already handles the server state.

## Migration Plan

No data migration. Existing complete credentials keep working; partial
credentials are sent to the setup screen. Rollback is removing the gate from
`MainNavigation`, with no persisted state to clean up.

## Open Questions

None.
