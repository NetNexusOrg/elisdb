# Proposal

## Why

The Android CI runs the full instrumented suite on every main push and tag even
though it is non-blocking and not required by the release job, starting a second
emulator for several extra minutes. Neither workflow cancels superseded runs,
and the Server CI reinstalls Python dependencies on every run.

## What Changes

- Restrict the Android full instrumented suite to pull requests and manual
  dispatches; keep the blocking Room migration gate and the tag release path.
- Add workflow-level concurrency cancellation to Android and Server CI.
- Enable pip caching in Server CI.
- Keep the Server Docker runtime check: it validates the container entrypoint and
  runtime imports, which pytest on the host does not cover.

## Capabilities

### Modified Capabilities

- `release-pipeline`: CI workload separation and cancellation behavior.

## Impact

- `.github/workflows/android-release.yml`
- `.github/workflows/server-ci.yml`
- `.opencode/skills/github-actions/SKILL.md`
- No application or server code changes.
