## Context

Ver proposta em `proposal.md` e requisitos em `specs/`. O CLI vive em `app/keys.py`: hoje só sabe `issue <user> [--server-url] [--output]`, rejeitando tudo o resto (`unknown command`, exit 2). As operações de dados já existem em `app/database.py` — `add_key`, `list_keys` (id, public_key, comment, created_at), `remove_key` — e há recurso a `ELISDB_SERVER_URL` como fallback do URL. Sem dependências externas no ficheiro (stdlib apenas; `input()`).

## Goals / Non-Goals

**Goals:**
- `python -m app.keys` sem argumentos abre um menu numerado: criar / listar / remover / sair.
- Criar interativamente pede URL do servidor e nome do utilizador em sequência (URL com default da env `ELISDB_SERVER_URL`).
- Listar mostra id, comentário, chave pública e data de criação; base vazia distinguida de lista com chaves.
- Remover pede id e confirma antes de apagar.
- Manter `issue <user> --server-url <url> [--output <path>]` para scripting.

**Non-Goals:**
- Sem comandos de `generate`/`add` de chaves externas (a emissão continua a ser o único meio de adicionar). No modo não interativo, `generate`/`add`/`remove`/`list` continuam rejeitados (exit 2) — apenas o menu interativo expõe list/remove.
- Sem alterações ao servidor HTTP, à auth ou ao banco de dados.
- Sem novas dependências.

## Decisions

**Menu com `input()` e dispatch por número:** como a spec pede escolha por número de opção, o fluxo interativo apresenta opções numeradas, lê `input()`, valida contra `{"1","2","3","4"}` e re-prompta em caso de entrada inválida (mensagem de erro + volta a mostrar o menu). Dados livres (URL, nome, id) usam `input()` direto, com trim. Alternativas consideradas: argparse com subcomandos — rejeitado porque o requisito é um fluxo guiado; biblioteca tipo `questionary` — fora de questão, tolerância zero a dependências novas.

**URL interactivo com default da env:** ao criar, mostra `[default: <url_da_env>]` se `ELISDB_SERVER_URL` existe; linha vazia aceita o default. Se não há env e o operador não preenche, erra com a mesma mensagem de sempre ("a server URL is required") e não produz ficheiro nem regista chave — cumpre o requisito de recusa.

**Remoção com confirmação, sem flags não interactivas:** o menu pede o id (aceita também `remove <id>` para quem quer atalho, mas antes de apagar confirma `s/N`). `Database.remove_key` devolve `bool` — usado para distinguir "apagada" de "id inexistente". Sem modo `--yes`, para nunca apagar sem confirmação.

**Saída reutilizável:`_print_key(row)`** centraliza o formato de linha (id, comment, public_key, created_at em UTC), usado tanto na listagem quanto na confirmação antes de remover — mantém a doc/observabilidade coerente.

## Risks / Trade-offs

- [Testar fluxo interativo é verboso em `test_main.py`] → injetar um objecto de entrada/saída (ou `monkeypatch` de `builtins.input`/`print`) nos testes; manter helpers pequenos e testar o dispatch e as recusas como o núcleo.
- [Regressão no modo argumentos] → manter a entrada `issue` inalterada e adicionar testes de compatibilidade desse caminho.
- [Remoção acidental de chave ativa] → confirmar sempre com `s/N` antes de apagar; listagem antes de pedir o id no próprio fluxo.