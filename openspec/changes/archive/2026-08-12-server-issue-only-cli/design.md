## Context

Hoje a CLI `python -m app.keys` (elisdb-server/app/keys.py) expõe `generate`, `issue`, `add`, `remove` e `list`. O `issue` já é o único a produzir o ficheiro de configuração completo (URL + seed) e a registar a chave; os restantes são caminhos paralelos de gerar/registar/gerir chaves individualmente. Ver proposal.md para motivação. A API HTTP (`GET /api/keys`, `GET /api/keys/me` em main.py) e `Database.add_key/remove_key/list_keys` (database.py) mantêm-se: o `issue` usa `add_key`, os testes de auth usam `remove_key`, e a app usa `GET /api/keys/me` para mostrar o utilizador.

## Goals / Non-Goals

**Goals:**
- CLI `app.keys` com um único comando: `issue <nome> [--server-url <url>] [--output <path>]`.
- Nenhum caminho de registo/generação de chaves fora da emissão do servidor.
- Docs e textos da app atualizados para o fluxo único.

**Non-Goals:**
- Alterar `Database` (métodos `add_key`/`remove_key`/`list_keys` ficam para uso interno, testes e API).
- Alterar a API HTTP (`/api/keys`, `/api/keys/me`) ou a autenticação.
- Remover o suporte interno da allowlist (auth continua a verificar contra a mesma lista).

## Decisions

**1. Remover os ramos do CLI mas manter `generate()` e `issue()` como funções.**
Em `app/keys.py`: os ramos `generate`, `add`, `remove`, `list` de `main()` são eliminados e `_usage()` passa a documentar apenas `issue`. As funções `generate()` e `issue()` permanecem: `generate()` é usada internamente por `issue()` e por testes de auth; `issue()` continua a chamar `db.add_key(...)`. `Database` não muda.
- Alternativa: apagar também `generate()` e mover a lógica para dentro de `issue()` — rejeitada porque `test_issued_key_authenticates_against_allowlist` e `test_only_added_keys_authenticate` importam `generate()` diretamente.

**2. Comandos desconhecidos continuam a dar erro (exit 2).**
Sem ramos para `generate`/`add`/`remove`/`list`, qualquer um deles cai no handler "unknown command". Mantém-se o comportamento atual de `return 2` sem modificar a allowlist — coerente com o cenário "No separate key commands are exposed by the CLI".

**3. Docs: seção única "Emitir o ficheiro de configuração".**
Em `docs/configuracao.md`, `README.md` e `elisdb-android/docs/uso.md`: remover as subsecções `generate`, `add`, `remove`, `list` e substituir por um único bloco `python -m app.keys issue <nome> --server-url <url>` que escreve o `<nome>.config.json` (transportável e importável nas Definições da app). A observabilidade da allowlist passa a referir `GET /api/keys` (autenticado) em vez de `app.keys list`.

**4. Texto de ajuda na app.**
`SettingsScreen.kt` já mostra `python -m app.keys issue <nome> --server-url <url>` (do change arquivado `2026-08-11-issue-config-file`) — texto já correto; só muda se a validação revelar referências a `generate`/`add`.

## Risks / Trade-offs

- **Operadores habituados a `app.keys add` perdem o registo de chaves externas** → O `issue` é o único fluxo documentado; chaves externas deixam de ter caminho CLI (documentado como intencional nos specs).
- **Testes que exercitam comandos removidos** → Atualizar/remover os testes em `test_main.py` que chamam `keys_main` com `generate`/`add`/`remove`/`list`; os testes que usam `Database.add_key`/`remove_key` diretamente mantêm-se.

## Open Questions

Nenhuma.