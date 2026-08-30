## Why

Cada chave emitida pelo servidor deve estar sempre associada a um utilizador nomeado, e a app precisa de saber qual. Hoje o `issue <user-name>` já guarda o `comment`, mas o `issue`/`add` aceitam nomes vazios e o servidor não expõe a associação à app (só `GET /api/keys`, a lista de todas as chaves).

## What Changes

- **Servidor (repo `../elisdb-server`):** o comando de emissão passa a configurar de forma obrigatória o nome de utilizador ao emitir uma chave.
- **Servidor:** novo endpoint autenticado `GET /api/keys/me` que devolve o utilizador associado à chave que assinou o pedido.
- **Servidor:** `comment` vazio é recusado na emissão (a chave fica sempre nomeada).
- Não altera o formato da assinatura nem o `keys.py issue <user-name>` existente.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `server-key-provisioning`: "Public key registration" — emissão passa a exigir um nome de utilizador, e a chave emitida fica sempre associada a um utilizador na allowlist; adiciona a consulta da identidade da chave (username do portador).

## Impact

- **Servidor** (`../elisdb-server`):
  - `app/main.py` — novo endpoint `GET /api/keys/me` (autenticado, devolve a identidade da chave que assinou).
  - `app/auth.py` ou `app/main.py` — resolver qual `key_id`/`comment` corresponde à chave verificada (atualmente `require_auth` só devolve *sim/não*).
  - `app/database.py` — acesso à chave por `public_key` para a consulta `me`.
  - `app/keys.py` — tornar o username obrigatório na emissão.
- **App** (consumo futuro): fica disponível a base para a app mostrar o utilizador; a UI é tratada na proposta `app-show-imported-key-user`.
- A app não assina com nada de novo; o endpoint reutiliza a autenticação ed25519 existente.