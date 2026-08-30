## Why

`SyncManager` deriva o sinal de "uploads pendentes" de `dao.observeAll()` (`SyncManager.kt:69-74`), mas `ClienteDao.observeAll()` filtra `deletePending = 0` (`data/local/ClienteDao.kt:15-16`). Por isso uma eliminação em fila fica invisível ao indicador de sync: o utilizador vê "sincronizado" enquanto um delete ainda está pendente de envio. Isto contradiz a spec `client-sync-state` ("Server synchronization status" deve refletir deletes pendentes).

## What Changes

- O sinal de uploads pendentes passa a incluir também eliminações pendentes (observar `getAll()` ou `getPendingDeletions()` em vez de `observeAll()` filtrado).
- O indicador de sync reflete o estado real: há trabalho por enviar quer sejam edições ou eliminações.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-sync-state`: a requirement "Server synchronization status" passa a exigir explicitamente que eliminações pendentes (`deletePending`) contribuam para o sinal de uploads pendentes, não só criações/edições.

## Impact
- Android: `SyncManager.kt`, `ClienteDao.kt` (observar eliminações pendentes). Sem alteração de protocolo.
- Testes: testar que apagar um cliente offline faz o indicador mostrar "pendente".
