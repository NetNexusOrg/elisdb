## Why

A identidade do dispositivo autenticado é escrita num objeto único e partilhado da aplicação FastAPI (`request.app.state.authenticated_key` em `app/main.py:58`), e lida de volta em `push` (`app/main.py:83`) e em `/api/keys/me` (`app/main.py:155`). Como `app.state` é partilhado por todos os pedidos em execução, dois pedidos concorrentes (ex.: dois dispositivos a sincronizar em paralelo, ou uvicorn com threadpool) sobrepõem-se mutuamente.

O resultado é corrupção de contexto por pedido:
- As entradas de auditoria (quem criou/editou um cliente) ficam atribuídas ao dispositivo errado.
- `/api/keys/me` pode devolver a identidade (key id / user name) do dispositivo errado.

Numa app offline-first multi-dispositivo isto é uma falha de integridade, não meramente cosmética.

## What Changes

- A identidade do dispositivo autenticado passa a ser guardada em `request.state` (por-pedido), e lida de `request.state` em `push` e em `/api/keys/me`.
- O autor da auditoria (actor key id / user name) é obtido exclusivamente de `request.state` no momento do push, nunca de estado partilhado da app.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-auth`: a requirement "Authenticated requests" passa a exigir que a identidade autenticada seja mantida por-pedido (`request.state`) e não em estado partilhado da aplicação.
- `server-sync`: a requirement "Client edit history recorded on push" passa a exigir que o actor seja lido de `request.state`, garantindo que a auditoria não é afetada por concorrência.

## Impact
- Servidor: `app/main.py` (`require_auth` escreve `request.state`; `push` e `/api/keys/me` leem `request.state`).
- Testes: `test_main.py` (adicionar teste de dois pedidos concorrentes que não se sobrepõem; verificar que a auditoria e `/api/keys/me` refletem o dispositivo correto de cada pedido).
