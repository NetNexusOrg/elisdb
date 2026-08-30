---
name: android-testing
description: How to write and run tests for the ElisDB Android app (JUnit4 unit tests + Compose/Room instrumentation tests). Use when adding tests, diagnosing failing tests, or deciding test coverage for a new feature.
---

# Android Testing

## Test framework (verified)

- Unit tests: JUnit 4 in `elisdb-android/app/src/test/java/com/example/elisdb/`.
- Instrumented tests: `androidx.test` (runner, junit ext, espresso) + Compose UI test
  in `elisdb-android/app/src/androidTest/java/com/example/elisdb/`.
- Coroutines tested via `kotlinx-coroutines-test`; Ktor via `ktor-client-mock`.

## Running tests

```bash
cd elisdb-android
./gradlew :app:testDebugUnitTest          # local unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented (needs device/emulator)
./gradlew :app:lintDebug                  # lint
```

## Conventions

- Unit tests cover ViewModels, repository logic, sync coordination (`SyncCoordinatorTest`,
  `SyncManagerTest`), mapping, and `SignerTest` — pure logic without a device.
- Instrumented tests cover Room migrations (`AppDatabaseMigrationTest`), repository/DAO
  against a real DB, and Compose screens (`*ScreenTest`).
- Test files mirror the package of the code they test.

## Adding tests for a new feature

- Pure logic (mapping, validation, state transitions) → unit test in `app/src/test/`.
- UI behavior (screen renders, interactions, navigation) → Compose test in `app/src/androidTest/`.
- DB/schema changes → add a migration test in `AppDatabaseMigrationTest` and export schema JSON.
- Fakes for sync collaborators live in `app/src/test/.../sync/SyncFakes.kt` — reuse them.

## Diagnosing failures

1. Run the single failing test or class: `./gradlew :app:testDebugUnitTest --tests "<FQN>"`.
2. Read the HTML report at `app/build/reports/tests/testDebugUnitTest/`.
3. For instrumented failures, check `app/build/reports/androidTests/`.
4. Distinguish "test bug" (assumption wrong) from "product bug" (behavior wrong)
   before editing either — do not weaken a test to make it pass.

## Criteria

- New feature/change that affects behavior should include at least the unit test
  that locks the behavior; UI-only changes get a Compose test.
- Never commit a test that is skipped or commented out to hide a failure.
