## Why

Quando o servidor responde `duplicate` a um push, a app funde o registo local no registo canónico (existing_id) e re-push o conjunto **já amputado**: as fotos do registo canónico não são preservadas. O `mergeDuplicate` usa `insert` (REPLACE), que dispara a FK CASCADE de `fotos` e apaga localmente as fotos do canónico quando este já existe; e mesmo sem cascade, o re-push reenvia só as fotos do registo local, pelo que `_replace_fotos` no servidor apaga as fotos canónicas **permanentemente** — muitas vezes sem a app alguma vez as ter visto.

## What Changes

- O servidor passa a incluir o registo canónico completo (client + fotos) na resposta de push com status `duplicate`, tornando a resposta auto-suficiente para o merge.
- `mergeDuplicate` passa a usar `upsert` em vez de `insert` (REPLACE), eliminando o DELETE+INSERT que dispara a FK CASCADE de `fotos`.
- O merge junta as fotos do registo canónico com as do registo local (união, sem perda), materializando as do canónico antes do re-push.
- O re-push do registo fundido envia o conjunto completo (fotos canónicas + fotos locais), pelo que `_replace_fotos` no servidor passa a preservar tudo.
- Nenhuma foto se perde em nenhum dos dois cenários: canónico já existente localmente, ou canónico só existente no servidor.
- **BREAKING**: o contrato do endpoint `POST /api/push` muda — a resposta `duplicate` passa a carregar o registo canónico (client + fotos). Clientes antigos ignoram os campos novos.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `duplicate-reconciliation`: o merge passa a preservar também as fotos do registo canónico (união), e a resposta `duplicate` passa a fornecer os dados do canónico (client + fotos) para o merge atómico.
- `server-sync`: o endpoint de push, ao devolver `duplicate`, passa a incluir o registo canónico completo (client + fotos) na resposta.

## Impact

- **Servidor** (`elisdb-server/app`): `main.py` (endpoint `/api/push`), `models.py` (`PushResult`), `database.py` (`save_client` — expor o canónico com fotos).
- **App Android** (`elisdb-android/app`): `SyncCoordinator.kt` (`mergeDuplicate`, tratamento da resposta de push), `remote/RemoteDto.kt` (`PushResultDto`), `remote/ApiClient.kt` (desserialização), `data/local/ClienteDao.kt` (reutilizar `upsert`).
- **Testes**: `test_main.py`/`test_provision.py` no servidor; `SyncCoordinatorTest.kt`, `SyncFakes.kt` na app.