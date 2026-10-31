# Proposal

## Why

O aviso de “servidor desatualizado” aparecia a quem usa a app, mas essa pessoa
não tem forma de atualizar o servidor. É informação técnica que só gera ruído;
a versão de API continua útil para diagnóstico do lado do servidor.

## What Changes

- Remove o aviso de servidor desatualizado do ecrã de Configuração e o estado
  associado na app.
- Mantém `api_version` em `/health` e `/api/latest`, agora só para diagnóstico
  (documentação e testes do servidor).
- `ServerInfo.apiVersion` continua mapeado, sem consequências visíveis.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-settings`: deixa de exigir o aviso de servidor desatualizado.

## Impact

- `ui/settings/ConfigScreen.kt`, `ui/settings/SyncViewModel.kt`,
  `sync/SyncManager.kt`, `sync/ServerInfo.kt`.
- Testes unitários e instrumentados; documentação do servidor e da app.
