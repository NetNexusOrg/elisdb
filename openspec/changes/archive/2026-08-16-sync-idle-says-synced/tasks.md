# Tasks: Sync idle says synced

## 1. App — mensagem idle

- [x] 1.1 Em `ui/settings/SyncStatusBar.kt`, no `syncMessage`, substituir a string do `else` (idle) de "Sem sincronizações pendentes" para "Sincronizado"

## 2. Testes

- [x] 2.1 Atualizar `SyncStatusBarTest` (teste idle) para esperar "Sincronizado" em vez de "Sem sincronizações pendentes"

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: abrir Definições sem sincronizações pendentes e confirmar que mostra "Sincronizado"
