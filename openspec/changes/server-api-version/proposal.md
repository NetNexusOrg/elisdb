# Proposal

## Why

O `/health` só devolve a versão do servidor, que não diz que funcionalidades
existem. Depois de uma alteração de contrato (o estado ativo/inativo), um
servidor antigo continuou a responder normalmente e a app perdia o campo em
silêncio. A versão do servidor não serve como sinal de compatibilidade.

## What Changes

- O servidor passa a expor uma **versão de API** (`api_version`) em `/health` e
  em `/api/latest`, independente do número de release do servidor.
- A app guarda a versão de API reportada pelo servidor e mostra um aviso no ecrã
  de Configuração quando ela é inferior à mínima que a app suporta, ou quando o
  servidor nem a reporta (servidor anterior a este campo).
- Sem alteração do contrato de sincronização; campos novos são aditivos.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `server-sync`: `/health` e `/api/latest` passam a incluir `api_version`.
- `client-settings`: o ecrã de Configuração avisa quando o servidor está
  desatualizado.

## Impact

- `elisdb-server/app/main.py` e testes.
- `sync/ServerInfo.kt`, `remote/ApiClient.kt`, `sync/SyncManager.kt`,
  `ui/settings/SyncViewModel.kt` e `ui/settings/ConfigScreen.kt`.
- Testes unitários e instrumentados; documentação.
