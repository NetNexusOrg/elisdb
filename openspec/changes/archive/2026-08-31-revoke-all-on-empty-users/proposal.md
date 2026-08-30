## Why

Quando `ELISDB_USERS` ficava vazio ou era removido do `compose.yaml`, o entrypoint
saltava o provisionamento por completo — nenhuma chave auto-provisionada era
revogada e os ficheiros de configuração permaneciam no disco. Isto contradizia
a expectativa de que "apenas os utilizadores listados em ELISDB_USERS devem ter
acesso". Um operador que removesse todos os utilizadores da variável continuaria
com chaves antigas a funcionar silenciosamente.

## What Changes

- `elisdb-server/docker-entrypoint.sh`: remove o guard `if [ -n "${ELISDB_USERS:-}" ]` —
  o `python -m app.provision` corre sempre no arranque.
- `elisdb-server/app/provision.py`: remove o atalho `if not users: return 0` em `main()`;
  a exigência de `ELISDB_SERVER_URL` passa a ser condicional (`if users and not server_url`).
- `openspec/specs/server-key-provisioning/spec.md`: atualiza o cenário
  "Provisioning with no users listed does nothing" → "Empty ELISDB_USERS revokes
  all provisioned keys".
- `elisdb-server/docs/configuracao.md` e `docs/execucao.md`: documentam o novo
  comportamento.

## Capabilities

### New Capabilities

_(nenhum)_

### Modified Capabilities

- `server-key-provisioning`: o cenário "Provisioning with no users listed does
  nothing" é substituído por "Empty ELISDB_USERS revokes all provisioned keys" —
  quando ELISDB_USERS está vazio ou ausente, todas as chaves auto-provisionadas
  são revogadas e os respetivos ficheiros de configuração removidos.

## Impact

- **Código**: `app/provision.py` (main), `docker-entrypoint.sh`
- **Spec**: `openspec/specs/server-key-provisioning/spec.md`
- **Docs**: `elisdb-server/docs/configuracao.md`, `elisdb-server/docs/execucao.md`
- **Comportamento**: operadores que definam `ELISDB_USERS: ""` ou removam a variável
  verão todas as chaves auto-provisionadas revogadas no próximo arranque. Chaves
  emitidas manualmente via CLI continuam intocadas.
