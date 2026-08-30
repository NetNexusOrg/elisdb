## 1. CLI do servidor

- [x] 1.1 Em `app/keys.py`: eliminar os ramos `generate`, `add`, `remove` e `list` de `main()`; manter `generate()`, `issue()` e o ramo `issue`; atualizar `_usage()` para documentar apenas `python -m app.keys issue <nome> [--server-url <url>] [--output <path>]`.
- [x] 1.2 Confirmar que invocar `python -m app.keys generate|add|remove|list` devolve erro (exit 2) sem modificar a allowlist, e que `python -m app.keys issue <nome> --server-url <url>` continua a escrever o `<nome>.config.json` e a registar a chave.

## 2. Testes do servidor

- [x] 2.1 Em `test_main.py`: remover/ajustar testes que chamam `keys_main` com comandos removidos, mantendo os testes de `issue` (ficheiro escrito, recusa sem URL/nome, registo) e os que usam `Database.add_key`/`remove_key`/`generate()` diretamente para auth.
- [x] 2.2 Acrescentar teste: comando removido devolve 2 e não altera a allowlist.
- [x] 2.3 `python -m pytest` verde.

## 3. Documentação

- [x] 3.1 Atualizar `elisdb-server/docs/configuracao.md`: remover subsecções `generate`/`add`/`remove`/`list`; manter apenas "Emitir um par de chaves para um utilizador" como fluxo único; referir `GET /api/keys` para observabilidade.
- [x] 3.2 Atualizar `elisdb-server/README.md` e `docs/instalacao.md` (e README raiz, se referirem `add`) para o fluxo único `issue`.
- [x] 3.3 Atualizar `elisdb-android/docs/uso.md`: substituir referências a `generate`/`add` pelo fluxo único `issue` + importação do ficheiro.
- [x] 3.4 Confirmar que o texto de ajuda em `SettingsScreen.kt` e os seus testes continuam corretos (referem apenas `python -m app.keys issue <nome> --server-url <url>`).

## 4. Verificação

- [x] 4.1 Testes `python -m pytest` do servidor verdes.
- [x] 4.2 Validação manual: `python -m app.keys issue maria --server-url http://10.0.2.2:8000` gera o config completo; os comandos removidos falham; importação do ficheiro na app configura URL+chave e sincroniza.