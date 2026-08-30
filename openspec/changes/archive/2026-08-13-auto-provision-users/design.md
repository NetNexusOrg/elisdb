## Context

Ver `proposal.md`. Hoje, ligar um telemóvel exige entrar no container e correr
`python -m app.keys issue <nome> --server-url <url>` manualmente. Queremos que o
`compose.yaml` declare os utilizadores (variável `ELISDB_USERS`) e que o arranque
do container os provisione automaticamente, escrevendo os ficheiros
`<user>.config.json` prontos a importar na app. A base já tem o flow de emissão
(`app/keys.py`, `issue`/`_issue_and_register`) e o entrypoint POSIX
(`docker-entrypoint.sh`) que faz chown de `PUID`/`PGID` antes do `su-exec`.

## Goals / Non-Goals

**Goals:**
- Provisionar automaticamente uma chave por utilizador listado em `ELISDB_USERS` no arranque do container.
- Ser **idempotente**: utilizadores que já têm chave confiada não são reemitidos (os telemóveis já ligados continuam a autenticar após reinício).
- Escrever um ficheiro de configuração por utilizador novo numa pasta de configs dentro do volume de dados.
- Correr a provisionação como o utilizador do serviço (`PUID`/`PGID`), não como `root`.
- Reutilizar o flow de emissão existente (sem duplicar lógica de chaves).

**Non-Goals:**
- Não criar uma UI/CLI nova para provisionação (é automática no arranque).
- Não alterar o formato dos ficheiros de configuração nem o protocolo da app.
- Não mudar o comportamento do flow manual `issue` (continua a funcionar como hoje).

## Decisions

**Novo módulo `app/provision.py`:** expõe `provision_users(db, users, server_url, configs_dir)` que, para cada nome em `users` (trim, ignora vazios), verifica se já existe uma chave confiada com esse `comment` (`db.list_keys()`); se existir, salta; senão reutiliza o flow de emissão partilhado para gerar, registar no allowlist com o nome como comment e escrever `configs_dir/<user>.config.json`. Devolve a lista de utilizadores provisionados.

**Refactor mínimo em `app/keys.py`:** tornar `_issue_and_register` público como `issue_and_register(db, user, server_url, output=None)` (mesmo comportamento, usado pelo modo argumento, interativo e provisionação). Evita duplicar a escrita do ficheiro + `db.add_key`.

**Entrada `python -m app.provision`:** `main()` lê `ELISDB_USERS` (separada por vírgulas), `ELISDB_SERVER_URL`, `ELISDB_CONFIGS_DIR` (default `dirname(ELISDB_DB)/configs`), instancia `Database` e chama `provision_users`. Se houver utilizadores mas faltar `ELISDB_SERVER_URL` → erro e exit não-zero. Sem utilizadores → no-op, exit 0.

**Entrypoint `docker-entrypoint.sh`:** depois do chown dos diretórios de dados, e apenas quando `ELISDB_USERS` está definida, correr `su-exec "${PUID}:${PGID}" python -m app.provision`; depois o `exec su-exec ... "$@"` habitual inicia o uvicorn. A pasta de configs vive dentro do volume de dados (já coberto pelo chown); se `ELISDB_CONFIGS_DIR` apontar para outro caminho, o entrypoint inclui-o no loop de `mkdir`/`chown`.

**`compose.yaml`:** adicionar ao `environment`:
- `ELISDB_USERS: "maria,joao"` — lista separada por vírgulas (comentário a explicar).
- `ELISDB_SERVER_URL: "http://<ip-do-host>:8000"` — URL que os telemóveis usam (a mesma de `--server-url`).

**Testes:** novo `test_provision.py` (pytest, com tmp DB) cobrindo: provisiona utilizadores novos, é idempotente (não reemite), sem utilizadores não faz nada, e falha sem `ELISDB_SERVER_URL`.

## Risks / Trade-offs

- [`ELISDB_USERS` mal preenchida (nomes vazios)] → ignora entradas vazias/trim; nunca emite chave com nome vazio.
- [Reemissão em reinícios] → mitigada pela idempotência (verifica comment existente); se o operador remover uma chave pelo menu interativo, o próximo arranque volta a provisioná-la (comportamento desejado: o utilizador volta a ter acesso).
- [URL errada gera configs inutilizáveis] → a provisionação exige `ELISDB_SERVER_URL`; o operador deve pôr a URL externa (IP do host), igual ao fluxo manual.
- [Segurança: chaves privadas em texto no ficheiro de configs] → mesmo comportamento do fluxo manual (`issue` escreve o seed em claro); o volume deve ter permissões restritas.