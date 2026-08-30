# Tasks: Sync bar not configured

## 1. App — mostrar "Não configurado" sem config

- [x] 1.1 Em `ui/settings/SyncStatusBar.kt`, adicionar parâmetro `hasConfig: Boolean` (default `true`) e, no `syncMessage`, quando `!hasConfig` (e não in-progress), devolver "Não configurado"
- [x] 1.2 Em `ui/settings/SettingsViewModel.kt`/`SettingsScreen.kt`, passar `hasKey` (do `SettingsUiState`) como `hasConfig` ao `SyncStatusBar`

## 2. Testes

- [x] 2.1 Atualizar `SyncStatusBarTest`: idle sem config → "Não configurado"; idle com config → "Sincronizado"

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: com servidor não configurado, abrir Definições e confirmar que a barra mostra "Não configurado" imediatamente (sem clicar em sincronizar); importar a config e confirmar que mostra "Sincronizado"
