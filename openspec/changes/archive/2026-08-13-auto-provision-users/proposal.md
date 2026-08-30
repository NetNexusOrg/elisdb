# Proposal: Auto-provision users from compose variables

## Why

Ligar um telemóvel hoje exige entrar no container e correr manualmente
`python -m app.keys issue <nome> --server-url <url>` e depois copiar o
ficheiro de configuração para fora. Para uma instalação repetível, o operador
quer declarar os utilizadores no `compose.yaml` e que o container, no arranque,
crie as chaves automaticamente — ficando os ficheiros de configuração prontos a
importar na app Android.

## What Changes

- Adicionar `ELISDB_USERS` ao `compose.yaml`: lista de nomes de utilizador
  separada por vírgulas; o container emite uma chave para cada um no arranque.
- Adicionar `ELISDB_SERVER_URL` ao `compose.yaml`: a URL que os telemóveis usam
  para chegar ao servidor (a mesma que hoje se passa a `--server-url`).
- Novo módulo `app/provision.py` invocado pelo entrypoint: emite chaves de forma
  **idempotente** — um utilizador que já tenha chave confiada não é reemitido
  (senão os telemóveis já ligados deixavam de autenticar).
- Os ficheiros `<user>.config.json` são escritos numa pasta de configs dentro do
  volume de dados (`/app/data/configs`), recuperável com `docker cp` ou bind
  mount.
- O entrypoint (`docker-entrypoint.sh`) corre a provisionação antes do uvicorn,
  apenas quando `ELISDB_USERS` está definida.
- Documentação: `docs/execucao.md` e referência de env vars.

## Capabilities

- **New Capabilities**: nenhuma.
- **Modified Capabilities**:
  - `server-auth` — a forma de adicionar chaves confiadas passa a incluir a
    provisionação automática no arranque (para além do flow `issue` manual).
  - `server-key-provisioning` — novo requisito de provisionação automática por
    variáveis de ambiente no arranque do container.

## Impact

- `elisdb-server/app/provision.py` (novo), `docker-entrypoint.sh`, `compose.yaml`.
- Testes de provisionação em `test_main.py` (ou novo ficheiro).
- Documentação de execução em `docs/execucao.md`.