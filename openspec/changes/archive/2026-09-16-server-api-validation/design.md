## Context

Motivação no `proposal.md`. Estado atual: `app/database.py:save_client` aplica LWW sem validar o `updated_at`; `app/main.py:pull` não valida `limit`/`since_id`; `app/models.py` não limita o comprimento dos campos; e `app/main.py:push` faz `raise HTTPException(400)` ao primeiro `ValueError`, descartando o lote. Os timestamps de dados do cliente são epoch **milliseconds** para `updated_at` e epoch **seconds** para `deleted_at` (os clientes enviam `nowMillis()` vs `normalizedEpochSeconds()`).

## Goals / Non-Goals

**Goals:**
- Rejeitar input inválido (pull params, campos longos, timestamps no futuro) sem quebrar o fluxo normal.
- Reportar o resultado do push por registo, para que um registo inválido não descarte os restantes.

**Non-Goals:**
- Não corrigir a inconsistência de unidades ms/s entre `updated_at` e `deleted_at` (bug pré-existente separado; este change valida cada campo na sua própria escala).
- Não adicionar validação de comprimento no cliente Android.

## Decisions

**D1: Janela de futuro para timestamps**
`MAX_FUTURE_SKEW_SECONDS = 3600` (1 hora), generosa o suficiente para tolerar drift de relógio não sincronizado mas a bloquear valores absurdos (ex. ano 2286). `updated_at` é validado em ms (`> (now_s + 3600) * 1000`), `deleted_at` em s (`> now_s + 3600`). Rejeição devolve `ValueError` (→ status `error` por registo no push; 400 no delete).
- *Alternativa*: janela de 300s alinhada ao auth — rejeitada por poder causar falsos positivos com offset de relógio obsoleto.

**D2: Push por registo com status `error`**
`push()` captura `ValueError` por registo e devolve `PushResult(status="error")`, continuando o lote. O campo `status` já é `str` (sem alteração de modelo). O cliente trata `error` mantendo o registo pendente e emite um aviso, em vez de o marcar como aceite.
- *Alternativa*: transação atómica de todo o lote — rejeitada porque mudaria a semântica de idempotência e o contrato atual.

**D3: Validação do pull via FastAPI + check manual**
`limit` usa `Query(None, ge=1, le=1000)` (cap 1000); `since_id` sem `since` devolve 400 com um check explícito no endpoint.

**D4: Limites de comprimento nos campos**
`nome` 200, `contrato` 100_000, `notas` 100_000 (via `Field(max_length=...)`). Pydantic devolve 422 por defeito em violações.

## Risks / Trade-offs

- [Limite 100_000 ainda permite texto grande] → aceitável; bloqueia o abuso de "megabytes" sem afetar uso legítimo.
- [Rejeição de `updated_at` futuro pode rejeitar relógio muito adiantado] → janela de 1h mitiga; o offset de relógio já é sincronizado via `/api/time`.
- [Status `error` não transporta mensagem] → suficiente para manter pendente; diagnóstico fica no log do servidor.
