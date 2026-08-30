## Context

The Settings screen has text elements the user wants removed:

1. `SettingsScreen.kt` renders a `Text` under the "Importar configuração do servidor" button explaining how the file is issued (3 lines).
2. `SettingsViewModel.importKey` sets `message = "Configuração importada e guardada."` on success, which the screen shows via `state.message`.
3. `SettingsViewModel.clearConfig` sets `message = "Configuração removida."`, which the screen shows via `state.message`.

## Goals / Non-Goals

**Goals:**
- The import-instructions paragraph is gone.
- Importing a config shows no success confirmation message.
- Removing the config shows no confirmation message.

**Non-Goals:**
- Removing other messages (`state.message` is still used for error/sync messages).
- Changing the import action, the remove action, or their sync-triggering behaviour.

## Decisions

**D1 — Remove the instructions paragraph.**
- Delete the `Text(...)` block for the import instructions in `SettingsScreen.kt` (lines ~155-162).
- *Rationale*: the action button is self-explanatory; the paragraph is clutter.

**D2 — Stop setting the import success message.**
- In `SettingsViewModel.importKey`, keep `message = null` after a successful import instead of `"Configuração importada e guardada."` (the message was already cleared at the start of the function).
- *Rationale*: the screen still renders `state.message` for other cases; only the success confirmation is dropped.

**D3 — Stop setting the remove confirmation message.**
- In `SettingsViewModel.clearConfig`, keep `message = null` instead of `"Configuração removida."`.
- *Rationale*: consistent with D2 — no confirmation text for config lifecycle actions.

**D4 — Files touched.**
- `SettingsScreen.kt`, `SettingsViewModel.kt`. Update the test in `SettingsViewModelTest` that asserts on "Configuração removida." (no other test asserts the removed texts). No server change.

## Risks / Trade-offs

- [No confirmation after import/remove] → The imported or cleared URL/key become visible in the fields immediately, so the user still sees the result.

## Migration Plan

None — app-only change.

## Open Questions

None.
