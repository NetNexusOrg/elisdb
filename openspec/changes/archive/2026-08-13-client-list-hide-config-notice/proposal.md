## Why

A primeira ecrã que o utilizador vê é a lista de clientes; exibir lá o aviso
"Servidor não configurado. Abre as Definições para configurar" polui a
apresentação e duplica informação que pertence às Definições. O aviso de
configuração deve viver apenas na ecrã de Definições, deixando a lista limpa.
O estado real de sincronização (a sincronizar, sincronizado, falha de rede/chave)
continua a ser útil na lista e permanece.

## What Changes

- A ecrã de lista de clientes deixa de apresentar o aviso de servidor não
  configurado (`SyncFault.CONFIG`) como banner.
- O estado de sincronização e os erros reais de sincronização (rede, autenticação,
  relógio) continuam visíveis na lista com a ação manual de sincronizar.
- As Definições mantêm o aviso completo de servidor não configurado.

## Capabilities

### New Capabilities
- nenhuma.

### Modified Capabilities
- `sync-observability`: o requisito "Sync observable from the client list" passa
  a não apresentar o estado de configuração em falta na lista de clientes (a
  mensagem de configuração fica reservada às Definições), mantendo os restantes
  estados/erros de sincronização na lista.

## Impact

- Código: `SyncStatusBar.kt` (mensagem de configuração opcionalmente suprimida) e
  `ClientListScreen.kt` (supressão no uso da lista).
- Sem alterações a API, a sincronização, specs de outros capabilities ou ao servidor.
- Sem alteração ao comportamento das Definições.