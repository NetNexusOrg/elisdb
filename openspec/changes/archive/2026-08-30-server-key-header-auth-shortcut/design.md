## Context

`auth.py:27` define `KEY_HEADER = "X-Elisdb-Key"` mas não é usado. `require_auth` (`app/main.py:56-59`) itera a allowlist a verificar a assinatura (O(n) por pedido). Com muitas chaves isto é caro. O cliente conhece o id da sua chave (vem da allowlist), pelo que pode enviá-lo num header para short-circuit.

## Goals / Non-Goals

**Goals:**
- Cliente envia id da chave em `X-Elisdb-Key`; servidor verifica só contra essa chave.
- Fallback para clientes sem o header (compatibilidade).

**Non-Goals:**
- Não mudar o payload assinado (continua método+path+query+body+timestamp).
- Não quebrar clientes atuais (que não enviam o header).

## Decisions

**D1: Header opcional no servidor**
`require_auth` aceita o header `X-Elisdb-Key`; se presente e na allowlist, verifica só essa chave; se presente mas ausente na allowlist → 401; se ausente → comportamento atual (todas as chaves).

**D2: Cliente envia o header**
`ApiClient`/`Signer` incluem `X-Elisdb-Key: <keyId>` em todos os pedidos assinados (o id vem do `SettingsStore`).

## Risks / Trade-offs
- [Header falsificado aponta para chave errada] → o servidor ainda verifica a assinatura contra essa chave; se a assinatura não bate, 401. O header só otimiza, não enfraquece a segurança.
- [Clientes antigos sem header] → fallback mantém compatibilidade.

## Migration Plan
- Sem migração de dados. Cliente e servidor podem ser publicados independentemente (header é opcional).

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
