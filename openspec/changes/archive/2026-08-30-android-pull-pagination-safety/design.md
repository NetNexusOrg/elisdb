## Context

`SyncCoordinator.pull()` (`SyncCoordinator.kt:237-240`) faz uma única chamada `api.pull()` e ignora `next_since`/`nextSinceId` da resposta (`remote/RemoteDto.kt:73-74`). Num full-pull (`watermark == null`) apaga clientes locais não presentes na página (`SyncCoordinator.kt:269-276`). O servidor já devolve o cursor composto `next_since`/`next_since_id` (ver `server-sync` "Incremental pull"), mas o cliente não o consome.

## Goals / Non-Goals

**Goals:**
- Seguir o cursor `next_since` até receber a lista completa antes de reconciliar.
- Nunca apagar locais com base numa página parcial.
- Avançar o watermark com o cursor da última página.

**Non-Goals:**
- Não alterar o protocolo do servidor (já devolve o cursor).
- Não mudar a lógica de tombstones/conflict resolution.

## Decisions

**D1: Loop de paginação no `pull()`**
`pull()` passa a acumular registos numa lista e, enquanto a resposta trouxer `next_since` (e `nextSinceId`), repetir `api.pull(since=next_since, sinceId=nextSinceId, limit=...)`. O `watermark` inicial define o primeiro `since`; as páginas seguintes usam o cursor. Apenas após a página sem cursor é que se reconcilia (e, no full-pull, se faz o sweep de remoção).

**D2: Sweep só após página completa**
O código que hoje apaga locais ausentes (`SyncCoordinator.kt:269-276`) passa a correr SÓ depois do loop terminar com `next_since == null` (página completa). Para pulls incrementais (`watermark != null`) mantém-se o comportamento de não fazer sweep (ver `sync-incremental-pull` "Pull watermark").

**D3: Watermark da última página**
O watermark avança para o `updated_at`/`id` do último registo da página final (ou `next_since` quando presente na última resposta), garantindo que o próximo pull incremental começa do ponto certo.

## Risks / Trade-offs
- [Pull mais demorado sob muitas páginas] → aceitável; garante integridade. O `limit` do servidor é parametrizável.
- [Memória acumula todas as páginas] → para o tamanho deste catálogo é desprezável; se necessário, reconciliar incrementalmente por página mantendo só o sweep para o fim.

## Migration Plan
- Sem migração de dados/esquema. O DTO `PullResponseDto` já traz `nextSinceId`.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
