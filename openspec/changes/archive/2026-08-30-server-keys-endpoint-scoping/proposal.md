## Why

`GET /api/keys` (`app/main.py:149-151`) devolve a tabela completa de chaves (chaves públicas + comentários) a QUALQUER dispositivo autenticado. Ou seja, cada dispositivo allowlisted tem leitura de todas as chaves e pode listar/eliminar chaves. Se os dispositivos não são pares totalmente iguais ( modelo self-hosted com vários utilizadores), isto é uma fuga de informação e uma superfície de abuso (um telemóvel comprometido pode enumerar e remover outros).

## What Changes

- A listagem de chaves (`/api/keys`) deixar de estar exposta a dispositivos cliente arbitrários; a gestão de chaves (listar/remover) fica limitada a operador (CLI) ou a um papel/escopo adequado.
- O endpoint `/api/keys/me` (identidade do próprio dispositivo) mantém-se para dispositivos.
- `delete_client` e demais escritas podem continuar permitidas a dispositivos, mas a enumeração da allowlist não.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-key-provisioning`: nova requirement "Device key listing is not exposed" — o servidor SHALL NOT expor a lista completa de chaves a dispositivos cliente autenticados arbitrários; a listagem/remoção de chaves é operação de operador (CLI) ou com escopo adequado.

## Impact
- Servidor: `app/main.py` (`/api/keys` passa a exigir escopo de operador ou é removido como endpoint de dispositivo; `/api/keys/me` mantém-se).
- Android: sem alteração (a app usa `/api/keys/me`, não `/api/keys`).
- Testes: `test_main.py` (dispositivo autenticado não consegue listar todas as chaves; operador via CLI continua a conseguir).
