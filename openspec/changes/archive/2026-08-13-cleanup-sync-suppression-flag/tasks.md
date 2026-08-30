## 1. Remover o flag do componente

- [x] 1.1 Remover o parâmetro `suppressConfigMessage` do composable `SyncStatusBar` em `SyncStatusBar.kt`
- [x] 1.2 Remover o parâmetro `suppressConfigMessage` de `syncMessage` em `SyncStatusBar.kt`
- [x] 1.3 No ramo `SyncFault.CONFIG` de `syncMessage`, devolver sempre o aviso "Servidor não configurado. Abre as Definições para configurar." (eliminar o `null` condicional)
- [x] 1.4 Atualizar a chamada interna a `syncMessage(...)` no composable para a nova assinatura

## 2. Atualizar testes

- [x] 2.1 Remover de `SyncStatusBarTest.kt` o parâmetro `suppressConfigMessage` do helper `setBar`
- [x] 2.2 Remover os dois casos de teste que passavam `suppressConfigMessage = true` (linhas ~88 e ~96)

## 3. Verificação

- [x] 3.1 Confirmar que nenhum caller em produção passa `suppressConfigMessage` (grep em `app/src/main`)
- [x] 3.2 Compilar/validar a UI (gradle de `elisdb-android`) sem erros