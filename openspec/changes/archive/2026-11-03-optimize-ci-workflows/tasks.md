# Tasks

## 1. OpenSpec

- [x] 1.1 Create proposal, spec delta, design and tasks for CI optimization.
- [x] 1.2 Validate the change with `openspec validate --strict`.

## 2. Android CI

- [x] 2.1 Restrict the full instrumented suite to pull requests and manual dispatches.
- [x] 2.2 Add workflow-level concurrency cancellation.
- [x] 2.3 Keep the Test job, Room migration gate and tag release job unchanged.

## 3. Server CI

- [x] 3.1 Add workflow-level concurrency cancellation.
- [x] 3.2 Enable pip caching for `requirements-dev.txt`.
- [x] 3.3 Keep the Server tests and Docker runtime-import jobs.

## 4. Documentation and verification

- [x] 4.1 Update the `github-actions` skill with the new triggers and caching.
- [x] 4.2 Validate YAML syntax and run the relevant local checks.
- [x] 4.3 Sync the `release-pipeline` spec and archive the change.
