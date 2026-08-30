## Why

`auth.py:27` define `KEY_HEADER = "X-Elisdb-Key"` mas nunca é usado. `require_auth` (`app/main.py:56-59`) verifica a assinatura contra TODAS as chaves da allowlist (loop O(n) por pedido). Com muitas chaves, cada pedido paga uma verificação ed25519 por chave. Um header com o id/índice da chave permitiria verificar só contra a chave certa.

## What Changes

- O cliente passa a enviar o id da chave num header (`X-Elisdb-Key`); o servidor, se o header vier, verifica apenas contra essa chave em vez de iterar a allowlist.
- Mantém-se o fallback de tentar todas as chaves se o header faltar (compatibilidade com clientes antigos).

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-auth`: a requirement "Authenticated requests" passa a descrever o uso opcional do header `X-Elisdb-Key` para short-circuit da verificação (ainda cobre método+path+query+body+timestamp).

## Impact
- Servidor: `app/auth.py` (aceitar header), `app/main.py` (passar header a `require_auth`).
- Android: `remote/ApiClient.kt`/`util/Signer.kt` passam a enviar o header com o id da chave (pequena alteração de rede; mantém assinatura).
- Testes: `test_main.py` (pedido com header válido aceite; header com id errado rejeitado).
