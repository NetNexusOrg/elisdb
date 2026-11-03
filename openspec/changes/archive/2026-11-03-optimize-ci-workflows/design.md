# Design

## Context

The Android workflow currently has two emulator jobs: a blocking Room migration
gate and a non-blocking full suite. The full suite runs on main and tag pushes
even though it is not part of `release.needs`, so it adds an emulator and several
minutes without protecting the release. The Server workflow runs host pytest and
a Docker runtime-import check; those cover different failure modes.

## Decisions

1. Add `if: github.event_name == 'pull_request' || github.event_name == 'workflow_dispatch'`
   to `instrumented-suite`, leaving `test`, `instrumented` and `release`
   unchanged.
2. Add workflow-level `concurrency` with
   `group: ${{ github.workflow }}-${{ github.ref }}` and
   `cancel-in-progress: true` to both workflows.
3. Enable `cache: pip` in Server CI with
   `cache-dependency-path: elisdb-server/requirements-dev.txt`.
4. Keep both Server jobs: `Server tests` checks application behavior, while
   `Docker image` checks that the production container builds and imports all
   runtime modules.

## Risks / Trade-offs

- A UI regression that only appears after a main merge will not be seen by CI
  until the next pull request or manual dispatch. This is acceptable because the
  suite was already non-blocking.
- Concurrency cancellation may stop a long-running run when a newer commit
  arrives; that is the intended behavior for superseded code.
