## Why

A API de sincronização aceita input inválido sem validação, o que permite corromper o estado partilhado: `updated_at`/`deleted_at` no futuro distante ganham o last-write-wins e quebram o pull incremental de todos os outros dispositivos; `limit` no pull aceita valores negativos/zero/enormes; e os campos de texto (`nome`, `contrato`, `notas`) são ilimitados, permitindo armazenar megabytes por registo. Além disso, `POST /api/push` aborta o lote inteiro com 400 quando um único registo é inválido, sem indicar quais registos persistiram.

## What Changes

- Validar os parâmetros do pull: `limit` deve ser `>= 1` e é limitado a um máximo; `since_id` só é aceite quando `since` está presente.
- Rejeitar registos cujo `updated_at` ou `deleted_at` estejam num futuro distante (acima do relógio do servidor mais uma janela), para que um timestamp malicioso não ganhe o LWW permanentemente.
- Limitar o comprimento dos campos de texto (`nome`, `contrato`, `notas`) para impedir bloat da base de dados.
- Processar `POST /api/push` registo a registo, devolvendo um resultado por registo (`accepted`, `duplicate`, `stale`, `error`) em vez de abortar o lote com 400, para que um registo inválido não descarte os restantes.

## Capabilities

### New Capabilities

### Modified Capabilities

- `server-sync`: validação dos parâmetros do pull, limite de comprimento dos campos de texto, rejeição de timestamps no futuro, e push com resultado por registo.

## Impact

- Servidor: `app/main.py` (validação do pull e do `delete_client`, push por registo), `app/database.py` (rejeição de `updated_at` no futuro), `app/models.py` (limites `max_length`).
- Cliente Android: `sync/SyncCoordinator.kt` trata o novo status `error` mantendo o registo pendente (em vez de o marcar como aceite).
