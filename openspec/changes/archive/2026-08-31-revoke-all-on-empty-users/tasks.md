## 1. Server — provisionamento

- [x] 1.1 Remover guard `if [ -n "${ELISDB_USERS:-}" ]` do `docker-entrypoint.sh`
- [x] 1.2 Remover atalho `if not users: return 0` de `main()` em `provision.py`
- [x] 1.3 Condicionar exigência de `ELISDB_SERVER_URL` a `if users and not server_url`

## 2. Spec

- [x] 2.1 Atualizar cenário "Provisioning with no users listed does nothing" → "Empty ELISDB_USERS revokes all provisioned keys" em `server-key-provisioning/spec.md`
- [x] 2.2 Atualizar descrição do requisito "Startup provisioning from environment variables"

## 3. Documentação

- [x] 3.1 Atualizar `docs/configuracao.md` — descrição de `ELISDB_USERS`
- [x] 3.2 Atualizar `docs/execucao.md` — nota sobre revogação com lista vazia

## 4. Verificação

- [x] 4.1 Testar `provision_users()` com lista vazia (revoga tudo)
- [x] 4.2 Testar `main()` com users mas sem URL (erro 2)
- [x] 4.3 Testar `main()` com users vazios sem URL (OK)

## 5. Deploy VPS01

- [x] 5.1 Sincronizar ficheiros do repo para VPS
- [x] 5.2 Remover ficheiros obsoletos (.venv, testes, etc.)
- [x] 5.3 Reconstruir imagem Docker e reiniciar container
- [x] 5.4 Remover chaves órfãs (Teste, maria, joao, João) da VPS
