## 1. Mover identidade para `request.state`

- [x] 1.1 Em `app/main.py`, `require_auth` passa a escrever `request.state.authenticated_key` em vez de `request.app.state.authenticated_key`
- [x] 1.2 Em `app/main.py`, `push` lê a identidade de `request.state.authenticated_key` para popular a auditoria
- [x] 1.3 Em `app/main.py`, `/api/keys/me` lê a identidade de `request.state.authenticated_key`

## 2. Testes e verificação

- [x] 2.1 Em `test_main.py`, adicionar teste em que dois pedidos autenticados por chaves diferentes são processados de forma concertada (ex.: via `anyio`/`asyncio` gather) e verificar que cada `/api/keys/me` devolve o `key_id`/`user_name` do respetivo pedido
- [x] 2.2 Adicionar teste de auditoria: dois pushes concorrentes por chaves diferentes resultam em entradas de auditoria com os `key_id` corretos (sem troca)
- [x] 2.3 Correr `pytest` no servidor
