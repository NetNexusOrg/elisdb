## Context

`require_auth` (`app/main.py:56-59`) verifica a assinatura ed25519 contra a allowlist e, ao sucesso, guarda a identidade em `request.app.state.authenticated_key` (`app/main.py:58`). `push` (`app/main.py:83`) e `/api/keys/me` (`app/main.py:155`) lêem `request.app.state.authenticated_key`. Como `app.state` é uma única instância partilhada por todos os pedidos, em concorrência dois pedidos sobrepõem-se. A auditoria (`server-sync` "Client edit history recorded on push") e `/api/keys/me` dependem desta identidade.

## Goals / Non-Goals

**Goals:**
- Isolar a identidade autenticada por-pedido via `request.state`.
- Garantir que auditoria e `/api/keys/me` refletem o dispositivo correto sob concorrência.

**Non-Goals:**
- Não alterar o esquema de assinatura (já cobre método+path+query+body+timestamp).
- Não introduzir nonce/replay (já coberto em `server-auth` anti-replay).

## Decisions

**D1: Usar `request.state` em vez de `request.app.state`**
`require_auth` escreve `request.state.authenticated_key` (objeto por-pedido do Starlette/FastAPI). `push` e `/api/keys/me` leem `request.state.authenticated_key`. Semântica idêntica para um único pedido; sob concorrência deixa de haver sobreposição.
- *Alternativa*: passar a identidade como parâmetro para as funções handler — rejeitado, exige alterar assinaturas de handlers e quebra o padrão atual de ler de `request`.

**D2: Tipo da identidade**
Guardar um pequeno objeto `{key_id, user_name}` ou o próprio registo da allowlist. `push` usa `key_id`/`user_name` para a auditoria; `/api/keys/me` devolve o `key_id` e o `user_name`.

## Risks / Trade-offs
- [Nenhum comportamento de rede muda] → risco baixo; apenas localização do estado.
- [Testes de concorrência podem ser flaky] → usar dois pedidos com a mesma allowlist mas chaves diferentes e verificar que cada resposta `/api/keys/me` e cada entrada de auditoria aponta para o key correto.

## Migration Plan
- Sem migração de dados/esquema. Apenas mudança de `app.state` → `request.state` em `app/main.py`.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
