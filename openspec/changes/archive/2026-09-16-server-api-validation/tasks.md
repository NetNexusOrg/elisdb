## 1. Pull validation

- [x] 1.1 Validar `limit` (`>= 1`, cap 1000) e rejeitar `since_id` sem `since` em `app/main.py:pull`; verificar que `limit=0`/`-1`/grande e `since_id` sem `since` devolvem 4xx

## 2. Field length limits

- [x] 2.1 Adicionar `Field(max_length=...)` a `nome` (200), `contrato` (100_000) e `notas` (100_000) em `app/models.py`; verificar que um campo acima do limite é rejeitado com 422

## 3. Future timestamp rejection

- [x] 3.1 Rejeitar `updated_at` no futuro distante em `app/database.py:save_client` (ms) e `deleted_at` em `app/main.py:delete_client` (s), com `MAX_FUTURE_SKEW_SECONDS = 3600`; verificar que um `updated_at`/`deleted_at` muito no futuro é rejeitado

## 4. Per-record push results

- [x] 4.1 Processar `POST /api/push` registo a registo, devolvendo `status="error"` por registo inválido em vez de abortar com 400; verificar que um lote com um registo inválido devolve resultados por registo e persiste os restantes
- [x] 4.2 Tratar o status `error` no cliente Android (`SyncCoordinator.pushEdits`) mantendo o registo pendente e emitindo aviso; verificar com os testes unitários
