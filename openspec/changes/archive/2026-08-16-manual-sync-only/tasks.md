# Tasks: Manual sync only

## 1. App — remover sync automática

- [x] 1.1 Em `ElisdbApp.kt`, remover a chamada `container.syncManager.schedulePeriodic()` para que não seja agendada sincronização automática
- [x] 1.2 (Opcional) Deixar `schedulePeriodic`/`SyncWorker` dormentes ou removê-los; garantir que não há outra via automática de push/pull

## 2. App — mensagens de estado

- [x] 2.1 Em `ui/settings/SyncStatusBar.kt`, no `syncMessage`, atualizar as mensagens: sem pendências → "Sem sincronizações pendentes"; só uploads → "Sincronização disponível (enviar)"; só downloads → "Sincronização disponível (receber)"; ambos → "Sincronização disponível (enviar e receber)"

## 3. Testes

- [x] 3.1 Atualizar `SyncStatusBarTest` para as novas mensagens (idle/sem pendências, enviar, receber, enviar e receber), mantendo a posição fixa da ação de sync
- [x] 3.2 Adicionar/ajustar teste unitário que confirme que a sync periódica não é agendada (ou que o `SyncManager` não enfileira trabalho periódico)

## 4. Verificação

- [x] 4.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 4.2 Verificação no emulador: criar um cliente → abrir Definições → mostra "Sincronização disponível (enviar)"; sincronizar manualmente → "Sincronizado"; confirmar que a app não transfere dados automaticamente (sem worker periódico ativo)

## 5. Mudança relacionada

- [x] 5.1 Descartar (não implementar) a mudança pendente `sync-idle-state-message`, pois a mensagem IDLE é agora coberta por esta mudança
