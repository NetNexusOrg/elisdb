## 1. Refactor em `app/keys.py`

- [x] 1.1 Renomear `_issue_and_register` para `issue_and_register` (público), mantendo a mesma assinatura e comportamento
- [x] 1.2 Atualizar os dois callers (`_run_args` e `_create_interactive`) para o novo nome

## 2. Módulo de provisionação

- [x] 2.1 Criar `app/provision.py` com `provision_users(db, users, server_url, configs_dir)`: por cada utilizador (trim, ignora vazios), se já existe chave com esse comment em `db.list_keys()` salta; senão chama `issue_and_register` com output `configs_dir/<user>.config.json`; devolve lista dos provisionados
- [x] 2.2 Em `app/provision.py`, `main()` lê `ELISDB_USERS` (vírgulas), `ELISDB_SERVER_URL`, `ELISDB_CONFIGS_DIR` (default `dirname(ELISDB_DB)/configs`); instancia `Database`; sem utilizadores → no-op exit 0; com utilizadores mas sem URL → erro + exit 2
- [x] 2.3 `main()` faz `os.makedirs(configs_dir, exist_ok=True)` antes de provisionar

## 3. Entrypoint

- [x] 3.1 Em `docker-entrypoint.sh`, depois do chown e apenas quando `ELISDB_USERS` está definida, correr `su-exec "${PUID}:${PGID}" python -m app.provision`
- [x] 3.2 Se `ELISDB_CONFIGS_DIR` apontar para fora dos diretórios de dados, incluí-lo no loop de `mkdir`/`chown`

## 4. Compose e docs

- [x] 4.1 Em `compose.yaml`, adicionar `ELISDB_USERS` (lista separada por vírgulas, com comentário) e `ELISDB_SERVER_URL` (URL externa que os telemóveis usam) ao environment
- [x] 4.2 Atualizar `docs/execucao.md` (secção Docker): explicar `ELISDB_USERS`/`ELISDB_SERVER_URL`, que as configs ficam em `/app/data/configs` e como as recuperar (`docker cp` ou bind mount)

## 5. Testes e verificação

- [x] 5.1 Criar `test_provision.py` (pytest, tmp DB): provisiona utilizadores novos; idempotência (não reemite quem já tem chave); sem utilizadores → no-op; sem URL → erro
- [x] 5.2 Correr `python -m pytest` em `elisdb-server/` com sucesso
- [x] 5.3 Correr `openspec validate --all` com sucesso