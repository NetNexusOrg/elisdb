## Why

O review/debug à app e ao servidor revelou três falhas de integridade e segurança no servidor (e, numa delas, também no cliente Android):

1. A query string dos pedidos não é coberta pela assinatura ed25519 — `since`, `limit` e `deleted_at` podem ser adulterados em trânsito sem invalidar a assinatura.
2. Um id de foto que já pertence a outro cliente é silenciosamente re-atribuído ao novo cliente no upsert, quebrando a regra "fotos pertencem a exatamente um cliente".
3. A paginação do pull usa apenas `updated_at` como cursor; registos com o mesmo `updated_at` (empates) são saltados na página seguinte, perdendo dados.

## What Changes

- **Assinatura da query string**: o servidor passa a incluir a query string no payload assinado; o cliente Android assina a mesma query que envia.
- **Propriedade de fotos imutável**: o servidor rejeita (em vez de re-atribuir) um push cujo id de foto já pertence a outro cliente.
- **Paginação à prova de empates**: o pull passa a usar um cursor composto `(updated_at, id)` para não saltar registos com o mesmo timestamp.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-auth`: a requirement "Authenticated requests" passa a exigir que a assinatura cubra também a query string.
- `server-sync`: a requirement "Record identity" ganha a regra de rejeição de colisão de ids de fotos entre clientes; a requirement "Incremental pull" passa a descrever o cursor composto de paginação.

## Impact

- Servidor: `app/auth.py` (mensagem assinada inclui query), `app/main.py` (`require_auth` usa path+query; endpoint pull aceita cursor composto), `app/database.py` (`_replace_fotos` rejeita colisão cross-cliente; `pull_all` usa keyset `(updated_at, id)`).
- Android: `util/Signer.kt` e `remote/ApiClient.kt` (assinar path+query nos pedidos pull/delete).
- Testes: `elisdb-server/test_main.py` (helper de assinatura passa a assinar a query; novos testes para os três casos).