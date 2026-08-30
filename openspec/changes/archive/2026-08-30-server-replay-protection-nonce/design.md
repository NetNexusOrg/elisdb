## Context

`auth.py` valida apenas a janela de timestamp (`TIMESTAMP_WINDOW_SECONDS = 300`) sem nonce nem cache. Uma assinatura capturada pode ser repetida dentro de 5 min. A assinatura cobre método+path+query+body+timestamp; não inclui um nonce.

## Goals / Non-Goals

**Goals:**
- Rejeitar replay dentro da janela de timestamp via nonce de curto TTL ou cache de assinaturas consumidas.

**Non-Goals:**
- Não encurtar a janela de timestamp (mantém tolerância a clock skew; já existe `handle-clock-skew` no cliente).
- Não exigir mudança no cliente (o nonce pode ser opcional no servidor; cliente pode passar a enviar um no futuro).

## Decisions

**D1: Cache de assinaturas consumidas (sem mudar o cliente)**
Manter em memória (ou Redis, se houver) um conjunto de assinaturas já vistas com TTL = janela de timestamp. Ao validar, se a assinatura já existe no cache → replay, rejeitar. Caso contrário, aceitar e inserir. Chave = hash da assinatura+timestamp. Como a assinatura é única por pedido (body+timestamp), dois pedidos diferentes têm assinaturas diferentes e não colidem.
- *Alternativa*: exigir nonce do cliente no header — rejeitada por agora, quebra clientes atuais; pode ser fase 2.

**D2: TTL do cache**
TTL igual à janela de timestamp (ex.: 300s). Em processo single-process o cache em memória basta; em multi-worker partilhado seria necessário backend comum (notar em doc).

## Risks / Trade-offs
- [Cache em memória perdido no restart] → aceitável; a janela de timestamp continua a proteger o essencial.
- [Multi-worker] → se o servidor correr com múltiplos workers, o cache deve ser partilhado; documentar ou usar backend comum.

## Migration Plan
- Sem migração. Apenas lógica de auth.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
