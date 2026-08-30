## Why

Hoje o provisionamento no arranque (`ELISDB_USERS`) só **adiciona** chaves para utilizadores novos — nunca revoga as de utilizadores que deixaram de estar na lista. Ao mudar `ELISDB_USERS` de `"Ricardo,Joao,Milton,Ulisses,Pedro"` para `"Ricardo,Joao,Ulisses"`, o arranque ignora Milton e Pedro e as respetivas chaves continuam válidas no allowlist, mantendo acesso indevido.

## What Changes

- O arranque passa a revogar (remover do allowlist) as chaves auto-provisionadas cujo utilizador já não consta em `ELISDB_USERS`, e a apagar a respetiva config (`<user>.config.json`).
- As chaves emitidas automaticamente passam a ser marcadas como provisionadas, para se distinguirem das chaves emitidas manualmente via CLI (que **não** são tocadas pela revogação).

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-key-provisioning`: a requirement "Startup provisioning from environment variables" passa a revogar chaves de utilizadores removidos da lista e a distinguir chaves provisionadas de chaves manuais.

## Impact

- Servidor: `app/database.py` (coluna `provisioned` na tabela `keys` + migração; `add_key` aceita `provisioned`; `list_keys` expõe o campo), `app/keys.py` (`issue_and_register` propaga o flag), `app/provision.py` (`provision_users` revoga chaves provisionadas órfãs e devolve também a lista de revogados).
- Testes: `elisdb-server/test_provision.py` (novos testes de revogação + atualização dos existentes), `elisdb-server/test_main.py` (migração da coluna `provisioned`).