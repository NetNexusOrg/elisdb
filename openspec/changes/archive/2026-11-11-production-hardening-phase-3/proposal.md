# Proposal

## Why

A fase 3 do hardening para produção fecha riscos operacionais que não mudam o
contrato da API: escritas SQLite em contenção devolviam 500 em vez de um erro
retryable, o contentor não tinha limites de recursos nem rotação de logs, e os
snapshots locais da app podiam sobreviver à remoção/mudança de configuração,
mantendo dados de clientes no dispositivo.

## What Changes

- Servidor: `sqlite3.OperationalError` passa a ser devolvido como HTTP 503
  (`database busy, retry later`), para o cliente poder tentar novamente em vez
  de tratar como erro definitivo.
- Servidor: `compose.yaml` ganha `pids_limit`, `mem_limit` e rotação de logs
  json-file, limitando exaustão de recursos e crescimento ilimitado de logs.
- App Android: `LocalSnapshotStore.clear()` apaga os snapshots locais, chamado
  por `AppContainer.resetSyncState()` ao trocar ou remover a configuração, para
  não reter dados de clientes do servidor anterior.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. São alterações operacionais e de retenção de dados locais, sem
alteração de requisitos observáveis da API.

## Impact

- `elisdb-server/compose.yaml`
- `elisdb-server/app/main.py`
- `elisdb-android/app/src/main/java/com/example/elisdb/sync/LocalSnapshotStore.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/AppContainer.kt`

Sem alterações de versão, `CHANGELOG`, base de dados ou servidor de produção.
