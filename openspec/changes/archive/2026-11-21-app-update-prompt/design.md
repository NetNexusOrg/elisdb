# Design

## Context

See `proposal.md` for the motivation. The Android app already owns the whole
update flow in `UpdateViewModel` and the About screen: it asks
`GET /api/app/latest`, compares `versionCode`, downloads
`GET /api/app/download` with progress, verifies size and SHA-256, and hands the
file to `ApkInstaller`. `AppContainer` exposes the API client and the
`ServerAvailability` monitor. The only missing piece is a check at app start
that surfaces the result outside the About screen.

## Goals / Non-Goals

**Goals:**

- Surface a newer published build as soon as the app opens, without blocking the
  client list or duplicating the download logic.
- Keep the About screen unchanged and let it own downloading and installing.

**Non-Goals:**

- Background or scheduled update checks while the app is not open.
- Silent/in-app installation (Android still requires the system installer).
- Persisting a "skip this version" decision across launches.
- Server, API, version, or signing changes.

## Decisions

**Reuse `UpdateViewModel` instead of a new checker.** It already encapsulates the
version comparison, availability gating, and offline/failure classification,
and is covered by unit tests. A second implementation would drift. The launch
prompt only reads `UpdatePhase.Available`.

**Mount the prompt at the Navigation root.** `MainNavigation` creates a
dedicated `UpdateViewModel` for the launch prompt and renders the dialog next to
`NavDisplay`. The About entry keeps its own instance because Navigation 3 gives
each entry its own `ViewModelStore`; sharing one instance would require hoisting
state across navigation scopes for no benefit.

**Extract the ViewModel wiring into a helper.** The constructor arguments
(configured check, fetch, download, availability, refresh) already live inline in
`Navigation.kt`; a `rememberUpdateViewModel(container)` composable keeps a single
copy for both the root and the About entry.

**Dismissal is in-memory for the launch.** `rememberSaveable<Int?>` stores the
version code the user postponed or opened, so rotation and navigation do not
re-prompt, while a fresh launch starts with no dismissal. This matches the
chosen "remind me next launch" behavior and avoids adding UI preferences to
`SettingsStore`. Alternative considered: a persisted "skipped version" flag,
rejected because the user explicitly wants to be reminded on every launch.

**Trigger the check when availability becomes `Online`.** `UpdateViewModel.check()`
returns early while `ServerAvailability` is `Checking`; a `LaunchedEffect` keyed
on availability calls it only in the `Online` state, the same pattern as
`AboutScreen`. The result arrives asynchronously and the dialog appears then.

**Never prompt over the About screen.** The prompt checks the current back stack
destination and stays hidden when About is on top, avoiding two update surfaces
at once.

## Risks / Trade-offs

- **One extra request per launch** → the check is silent, reuses the shared API
  client, and is skipped entirely when unconfigured or offline; a failure shows
  nothing.
- **Prompt on every launch can annoy** → the user chose this behavior; **Adiar**
  is always available and the dialog is non-blocking.
- **Two `UpdateViewModel` instances** (root + About) could check twice if the
  user taps **Atualizar** → both checks are cheap and idempotent; About's card
  needs its own state to download.
- **Install permission** → unchanged: About opens the system permission screen
  when needed.

## Migration Plan

No data migration. Ship with the next app release; rollback is removing the
prompt from `MainNavigation`, with no persisted state to clean up.

## Open Questions

None.
