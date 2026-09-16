## Why

O servidor não impõe qualquer limite de ritmo de pedidos: qualquer cliente (autenticado ou não, em `/health` e `/api/time`) pode martelar a API, e a verificação de assinatura ed25519 é cara (O(n) nas chaves do allowlist quando o header `X-Elisdb-Key` está ausente), amplificando o abuso. Além disso, as chaves emitidas guardam o `private_seed` em ficheiros de configuração criados com permissões `0644` (legíveis por qualquer utilizador local do container/host).

## What Changes

- Adicionar rate limiting por chave autenticada e por endereço IP, com respostas 429 quando excedido, protegendo `/api/*` sem bloquear a operação normal de sync.
- Escrever os ficheiros de configuração emitidos (`<user>.config.json`) com permissões restritivas (`0600`) para que o `private_seed` não seja legível por outros utilizadores.

## Capabilities

### New Capabilities

- `server-rate-limiting`: o servidor limita a frequência de pedidos (por chave e por IP) e devolve 429 quando o limite é excedido, sem afetar o fluxo normal de sincronização.

### Modified Capabilities

- `server-key-provisioning`: os ficheiros de configuração emitidos (com o `private_seed`) passam a ser criados com permissões restritivas (`0600`).

## Impact

- Servidor: `app/main.py` (middleware/dependency de rate limiting), `app/keys.py` (escrita do ficheiro de config com `0600`), possivelmente `compose.yaml` (exposição de limites via env vars).
- Sem alterações de API (os endpoints mantêm os mesmos contratos; acrescenta-se o status 429).
- Cliente Android: sem alterações obrigatórias; um 429 é tratado como erro transitório de sync.
