## 1. Implementação

- [x] 1.1 Em `SyncStatusBar.kt`: adicionar parâmetro `suppressConfigMessage: Boolean = false` e, quando ativo com `SyncFault.CONFIG`, não emitir texto (apenas a ação de sincronizar).
- [x] 1.2 Em `ClientListScreen.kt`: passar `suppressConfigMessage = true` no uso da `SyncStatusBar` da lista.
- [x] 1.3 Assegurar que `SettingsScreen.kt` não altera nada (usa o valor por omissão e mantém o aviso de configuração).

## 2. Testes

- [x] 2.1 Adicionar teste unitário do comportamento: com `suppressConfigMessage = true` e `CONFIG`, a mensagem não é apresentada; sem a flag, mantém-se.
- [x] 2.2 Verificar que os testes existentes continuam a passar (`testDebugUnitTest` + `compileDebugAndroidTestKotlin`).

## 3. Verificação manual

- [x] 3.1 Compilar (`compileDebugKotlin`) e instalar no emulador.
- [x] 3.2 Confirmar no emulador que a lista de clientes deixa de mostrar "Servidor não configurado. Abre as Definições para configurar." após tentativa de sincronização sem configuração.
- [x] 3.3 Confirmar no emulador que a mensagem continua visível nas Definições.