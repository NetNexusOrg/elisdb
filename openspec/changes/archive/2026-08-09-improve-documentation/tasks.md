## 1. Server documentation (`elisdb-server/docs/`)

- [x] 1.1 Create `elisdb-server/docs/index.md` (PT) explaining the docs folder and linking the four guide files.
- [x] 1.2 Write `instalacao.md`: prerequisites (Python 3 + venv), step-by-step `pip install -r requirements.txt`, first run facts (no default key, `401` until allowlist populated).
- [x] 1.3 Write `execucao.md`: `uvicorn app.main:app --reload`, default host/port, `/docs` interactive API, how to stop, how to pick `ELISDB_DB`/`ELISDB_PHOTOS`.
- [x] 1.4 Write `configuracao.md`: env table (`ELISDB_DB`, `ELISDB_PHOTOS` + defaults), the `app.keys` CLI (issue/add/list/remove) with copy-paste examples.
- [x] 1.5 Write `uso.md`: endpoint table (`/api/push`, `/api/pull`, `/api/clients`, `/api/clients/{id}`), request/response examples, dedupe + last-write-wins rules, photo cascade delete.
- [x] 1.6 Rewrite `elisdb-server/README.md` as a concise index (tech overview, setup/run quick-start, link to `docs/`), dropping prose duplicated in the guides.

## 2. App — `docs/` in elisdb repo

- [x] 2.1 Create `docs/` folder and `docs/index.md` (PT) linking the four guides and the "first run" flow.
- [x] 2.2 Write app `instalacao.md`: prerequisites (JDK + Android SDK + `local.properties`), first-build scaffolding via `./gradlew`/Android Studio import.
- [x] 2.3 Write app `execucao.md`: `./gradlew :app:assembleDebug`, install to device/emulator, `assembleRelease` + `scripts/build-release.sh` (fail-fast signing, keystore creds formats).
- [x] 2.4 Write app `configuracao.md`: single-source versioning (`gradle.properties` app.versionCode/Name), keystore/properties vs env vars, BuildConfig source + fail-fast empty-default behavior.
- [x] 2.5 Write app `uso.md`: key pairing with the server (`app.keys` + seed → `ELISDB_PRIVATE_KEY`), first-run sync, "Definições" runtime URL/key, and the end-to-end CLI path.
- [x] 2.6 Rewrite `README.md` as an index: project pitch, stack, 3-command quick start, link to `docs/`; cross-link to `../elisdb-server` docs.

## 3. Consistency and verification

- [x] 3.1 Run every documented command (server install/run/CLI; server pytest; app assembleDebug) and fix any inaccuracy found.
- [x] 3.2 Verify no loss of existing details (versioning single source, fail-fast signing, R8 scope, dedupe/conflict rules) and that all internal links resolve.
- [x] 3.3 Review all prose is PT with code/commands in EN; no secrets/real keys in examples.