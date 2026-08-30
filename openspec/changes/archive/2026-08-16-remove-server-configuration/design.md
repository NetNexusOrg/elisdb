## Context

The "Remover chave" action calls `SettingsViewModel.clearKey()` (`ui/settings/SettingsViewModel.kt:105-116`), which persists `settingsStore.save(baseUrl, "")` — clearing only the signing key while keeping the base URL. The user-visible inconsistency (see proposal.md — Why) is that the URL stays configured after removal. There is no stored "user": the associated user is runtime state (`keyIdentity`), resolved via `GET /api/keys/me` and reset by `clearKey` to `Unresolved`.

## Goals / Non-Goals

**Goals:**
- A single "Remover configuração" action clears base URL, signing key, and resets the associated-user state together.
- The Settings screen reflects the cleared state: "Não configurado" URL, "Não configurada" key, associated-user row and removal action hidden.
- A later sync reports a configuration error.

**Non-Goals:**
- Changing how configuration is imported or validated.
- Persisting the user (the user stays runtime-resolved).
- Removing the server-side key or any server data — this is purely a local device configuration action.

## Decisions

**D1 — Reuse `SettingsStore.save("", "")` as the clear mechanism.**
- `SettingsStore` already persists base URL and signing key together in `save()` (`settings/SettingsStore.kt:39-41`) and `save("", "")` fully clears both (blank strings are stored verbatim, overriding build defaults).
- *Rationale*: no interface change needed; the store already supports a full clear. The proposal noted a dedicated clear helper is optional — a named `clearConfig()` helper in the ViewModel keeps call sites expressive.
- *Alternative considered*: adding `SettingsStore.clear()` — rejected to keep the store surface minimal; `save("", "")` is equivalent.

**D2 — Replace `clearKey()` with `clearConfig()` in `SettingsViewModel`.**
- `clearConfig()` calls `settingsStore.save("", "")` and resets the UI state: `baseUrl = ""`, `signingKey = ""`, `hasKey = false`, `keyIdentity = Unresolved`, `signingKeyError = null`, and sets a confirmation message (e.g. "Configuração removida.").
- *Rationale*: mirrors the single-action user intent and keeps `SettingsUiState` consistent.
- *Alternative considered*: keeping `clearKey()` and adding URL clearing separately — rejected; the whole point is one atomic action.

**D3 — Update the button label and wiring.**
- In `ui/settings/SettingsScreen.kt:138-143`, change the button text to "Remover configuração" and `onClick` to `viewModel::clearConfig`.
- The button remains rendered only when a key is stored (`state.hasKey`), which after import is when configuration exists.
- *Rationale*: minimal UI change; the existing conditional rendering already matches "config present".

**D4 — Sync behavior after removal stays a config error.**
- After clearing, `ApiClientProvider.create` (`remote/ApiClientProvider.kt:17-28`) throws `ConfigException` (blank key first, then blank URL) → `SyncFault.CONFIG` → "Servidor não configurado. Abre as Definições para configurar." This is consistent and requires no code change.
- *Rationale*: the current blank-key path already produces the desired fault; clearing the URL too makes the message fully accurate.

## Risks / Trade-offs

- [A user accidentally removes the URL along with the key and must re-import] → Mitigated by the explicit single action label and the confirmation message; re-import is a single step and the standard setup path.
- [Build-config fallback re-surfaces a URL after clearing (blank pref falls back to `BuildConfig`)] → `save("", "")` stores blank strings verbatim, so they override build defaults and no fallback is triggered (confirmed by `SettingsStoreTest.save_blankValuesPersistedVerbatim`).

## Migration Plan

1. Update `SettingsViewModel` (`clearConfig`) and the settings screen button.
2. Update tests: androidTest `SettingsViewModelTest.clearKey_persistsBlankAndKeyHidden` → asserts URL and key both cleared; add unit test for `clearConfig`; add a settings-screen test for the new label if practical.
3. Build and run the app test suite.
4. Rollback: revert the ViewModel and screen changes — no schema or API impact.

## Open Questions

None.