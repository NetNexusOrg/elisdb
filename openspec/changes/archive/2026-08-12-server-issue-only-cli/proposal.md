## Why

Configurar o acesso de um dispositivo exige hoje vários comandos distintos: `generate` para criar só o par de chaves, `add` para registar uma chave pública individual/manual, e `remove`/`list` para gerir a allowlist. Um único comando de emissão — `python -m app.keys issue <nome> --server-url <url>` — já produz o ficheiro completo (URL + seed) e regista a chave no servidor. O objetivo é que **apenas** esse comando seja necessário: retirar do CLI `app.keys` todas as operações que criam ou registam chaves de forma individual.

## What Changes

- **Remover do CLI `python -m app.keys`**:
  - `generate` — cria apenas o par de chaves (seed + pública impressas); já não é necessário, pois `issue` gera e regista tudo dentro da emissão.
  - `add` — registo manual de uma chave pública individual na allowlist; deixa de haver caminho de registro fora da emissão do servidor.
  - `remove` — remoção individual de chaves da allowlist.
  - `list` — listagem manual da allowlist via CLI.
- **Manter como único comando**: `python -m app.keys issue <nome-do-utilizador> --server-url <url>` (e `--output`), que gera o par, regista a chave com o nome do utilizador e escreve o `<nome>.config.json` completo.
- **Manter a API HTTP** (`GET /api/keys`, `GET /api/keys/me`) e as funções de `Database` (`add_key`, `remove_key`, `list_keys`) como estão — o `issue` continua a recorrer a `add_key` internamente; o `Database` continua a suportar operações internas/testes (ex.: revogação usada nos testes de auth, `GET /api/keys/me` usado pela app para mostrar o utilizador).
- **Atualizar documentação** (`docs/configuracao.md`, `README.md`, app `docs/uso.md`, `docs/instalacao.md`, README raiz, texto de ajuda nas Definições da app) para apresentar apenas o fluxo único `issue`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `server-auth`: o requisito "Management of trusted keys" deixa de expor `add`/`remove`/`list` como superfície de CLI; a allowlist passa a ser gerida exclusivamente pela emissão (`issue`), mantendo o `GET /api/keys` e `GET /api/keys/me` como superfície de observação/identidade autenticada. O requisito "Public key allowlist" mantém-se (sem chave por omissão; aquisição apenas via emissão do servidor).
- `server-key-provisioning`: o requisito "Public key registration" deixa de incluir o cenário "Register an externally created public key"; apenas a emissão do servidor adiciona chaves. "Key identity is discoverable by the key holder" e "Trusted-key configuration visibility" mantêm-se via `GET /api/keys`/`me`.

## Impact

- Servidor: `elisdb-server/app/keys.py` (CLI: remover `generate`, `add`, `remove`, `list`; manter `issue`).
- Servidor: `elisdb-server/test_main.py` — remover/ajustar testes que exercitam os comandos removidos (o `Database.add_key`/`remove_key` mantém-se para os testes de auth e para o `issue`).
- Servidor: `elisdb-server/README.md`, `elisdb-server/docs/*.md`.
- App: `elisdb-android/docs/uso.md`, README raiz, e qualquer texto que mencione `generate`/`add`/`list`/`remove` (incluindo o texto de ajuda em `SettingsScreen.kt` se referir comandos removidos).
- Sem alterações na API HTTP nem no modelo de dados.