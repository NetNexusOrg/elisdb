# Tasks: Config not set message

## 1. App — mensagem de configuração

- [x] 1.1 Em `ui/settings/SyncStatusBar.kt`, no `syncMessage`, alterar a mensagem de `SyncFault.CONFIG` de "Servidor não configurado. Abre as Definições para configurar." para "Não configurado"

## 2. Testes

- [x] 2.1 Atualizar `SyncStatusBarTest.errorConfig_mostraMensagemConfiguracao` para esperar "Não configurado"

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: com servidor não configurado, abrir Definições e confirmar que a barra de sync mostra "Não configurado"
