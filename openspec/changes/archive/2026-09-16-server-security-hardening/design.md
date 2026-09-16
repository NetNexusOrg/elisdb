## Context

A motivação está no `proposal.md`. Estado atual relevante: o servidor FastAPI não tem nenhum mecanismo de rate limiting, e a verificação de assinatura em `app/auth.py` é custosa (O(n) nas chaves do allowlist quando o header `X-Elisdb-Key` está ausente). Os ficheiros de configuração emitidos (`app/keys.py:80`, via `app/provision.py:46`) são escritos com `open(..., "w")`, herdando o umask (tipicamente `0644`). O servidor corre em Docker com um único worker (`CMD ["uvicorn", "app.main:app", ...]` no `Dockerfile`), o que simplifica o estado em memória.

## Goals / Non-Goals

**Goals:**
- Impedir abuso de CPU/rede com rate limiting por chave e por IP, sem afetar o sync normal.
- Garantir que ficheiros de config com `private_seed` nunca fiquem legíveis por outros utilizadores (`0600`).

**Non-Goals:**
- Não introduzir dependências externas (ex.: SlowAPI, Redis). O rate limiting é implementado com um pequeno rastreador em memória.
- Não alterar contratos de API existentes (só acrescenta o status 429).
- Não tratar o replay cache multi-worker (já documentado como limitação; o servidor é single-worker).

## Decisions

**D1: Rate limiting em memória, por chave e por IP, via middleware HTTP**
Um middleware `@app.middleware("http")` conta pedidos numa janela deslizante/fixa (token bucket simples por `(identificador, janela)`). O identificador é a chave autenticada quando disponível (`request.state.authenticated_key`), senão o IP do cliente (`request.client.host`). Exceder o limite devolve 429. Como o servidor é single-worker, estado em memória é suficiente e evita dependências.
- *Alternativa*: SlowAPI/Redis — rejeitada por adicionar dependência e não ser necessária para um servidor self-hosted single-worker.

**D2: Limites configuráveis por env vars**
`ELISDB_RATE_LIMIT_REQUESTS` (pedidos por janela) e `ELISDB_RATE_LIMIT_WINDOW_SECONDS` (janela), com defaults generosos para não afetar o sync (ex.: 300 pedidos / 60s). Valores inválidos caem no default com aviso de log, em vez de crashar.

**D3: Escrita do config com permissões `0600`**
Em `app/keys.py:issue_and_register`, criar o ficheiro com `os.open(path, O_WRONLY|O_CREAT|O_TRUNC, 0o600)` e escrever via file descriptor, ou escrever normalmente e aplicar `os.chmod(path, 0o600)` de imediato. Aplicar o mesmo no caminho de provisioning (`app/provision.py`), que partilha `issue_and_register`.

**D4: Middleware de rate limiting não lê o body**
O contador roda antes de `call_next` usando apenas headers/estado, por isso não duplica nem agrava o problema de buffering de body (que é tratado como correção direta separada).

## Risks / Trade-offs

- [Estado em memória perde-se no restart] → aceitável; os contadores são efémeros e reiniciam com o servidor.
- [Multi-worker partilha contadores de forma errada] → o servidor é single-worker (Dockerfile); documentar a limitação.
- [Limites demasiado baixos bloqueiam sync legítimo] → defaults generosos e configuráveis; o sync é pontual, não contínuo.
- [IP partilhado (NAT) limita utilizadores legítimos] → mitigado por priorizar a chave autenticada como identificador quando disponível.
