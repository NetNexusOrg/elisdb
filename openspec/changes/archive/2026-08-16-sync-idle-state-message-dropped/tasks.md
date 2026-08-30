# Tasks: Sync idle state message

## 1. App — mensagem no estado IDLE

- [ ] 1.1 Em `ui/settings/SyncStatusBar.kt`, no `syncMessage`, adicionar a ramificação IDLE (o `else` final) para devolver uma mensagem informativa (ex. "Sem sincronizações pendentes" com um ícone adequado), mantendo a precedência dos estados pending/sucesso/erro/progresso

## 2. Testes

- [ ] 2.1 Atualizar `SyncStatusBarTest.idle_mostraBotaoSemMensagem` para verificar a nova mensagem IDLE (ex. `onNodeWithText("Sem sincronizações pendentes")`), mantendo a posição fixa da ação de sync

## 3. Verificação

- [ ] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [ ] 3.2 Verificação no emulador: abrir Definições sem sincronização pendente e confirmar que a barra mostra a mensagem IDLE junto ao botão de sincronizar
