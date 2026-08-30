## 1. Infraestrutura do menu

- [x] 1.1 Num `app/keys.py`, extrair o processamento de argumentos do `main()` para uma função `_run_args(args, db)` sem alterar o comportamento atual do `issue`
- [x] 1.2 Adicionar helper `_prompt(prompt, default="")` que lê `input()` com trim e devolve o default em linha vazia
- [x] 1.3 Adicionar `_print_key(row)` que imprime id, comment, public_key e created_at (UTC) de uma chave
- [x] 1.4 Adicionar `_run_interactive(db)` que apresenta menu numerado (1 criar, 2 listar, 3 remover, 4 sair), lê a opção, valida (re-prompta em entrada inválida) e despacha

## 2. Ações interativas

- [x] 2.1 Criar interativo: pedir URL do servidor (default da env `ELISDB_SERVER_URL`) e nome do utilizador em sequência; recusar sem URL válido (mensagem já existente) e sem nome; escrever `<nome>.config.json` e `db.add_key` — reutilizando o fluxo do `issue` atual
- [x] 2.2 Listar interativo: `db.list_keys()`, distinguir base vazia ("nenhuma chave") de lista com chaves, imprimir cada linha com `_print_key`
- [x] 2.3 Remover interativo: pedir id, mostrar a chave alvo (`_print_key`) para confirmar, pedir confirmação `s/N`, chamar `db.remove_key`, informar sucesso vs id inexistente

## 3. Dispatch no `main()`

- [x] 3.1 Em `main()`, sem argumentos → `_run_interactive(db)`, devolver 0 ao sair
- [x] 3.2 Manter: com argumentos, só `issue` é aceite; `generate`/`add`/`remove`/`list` continuam `unknown command` (exit 2)

## 4. Testes

- [x] 4.1 Adicionar a `test_main.py` testes de `keys.main([])`-como-interativo com `input`/`print` mockados: menu despacha criar/listar/remover
- [x] 4.2 Testar criação interativa: pedido sequencial de URL e nome, default da env, recusa sem URL e sem nome
- [x] 4.3 Testar listagem vazia vs com chaves e formato das linhas
- [x] 4.4 Testar remoção: confirmação `s/N`, sucesso, id inexistente
- [x] 4.5 Regressão: `issue <user> --server-url <url>` continua a funcionar e `generate`/`add`/`remove`/`list` como argumento continuam a falhar (exit 2)

## 5. Documentação e validação

- [x] 5.1 Atualizar `elisdb-server/docs/configuracao.md` (e referências) com o uso interativo `python -m app.keys`
- [x] 5.2 Correr os testes do servidor (`python -m pytest`) e `openspec validate --all` com sucesso