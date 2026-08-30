## Why

O comando `python -m app.keys` exige hoje que o operador saiba a sintaxe exata (`issue <user> --server-url <url>`) e não oferece forma de consultar ou remover chaves registadas. Várias autenticações → gestão manual por SQL. Um CLI interativo torna a operação quotidiana simples e reduz erros de digitação.

## What Changes

- Correr `python -m app.keys` **sem argumentos** abre um menu interativo com opções numeradas: **criar** (emitir), **listar** e **apagar** autenticações, mais **sair**.
- A criação interativa pede, em sequência: URL do servidor, nome do utilizador e (por omissão) usa `<nome>.config.json` como output; o pedido vale o default da variável `ELISDB_SERVER_URL` quando existir.
- A listagem mostra id, comentário (nome do utilizador), chave pública e data de criação; distingue base de dados vazia.
- A remoção pede o id da chave, confirma antes de apagar e apaga apenas chaves existentes.
- O modo com argumentos (`issue <user> --server-url <url> [--output <path>]`) **mantém-se** para scripting — não-corrompe a compatibilidade. **BREAKING** (lig.: a delta permite list/remove via CLI interativa, que hoje a spec proíbe).

## Capabilities

### New Capabilities
<!-- nenhuma -->

### Modified Capabilities
- `server-auth`: o requisito "Management of trusted keys" muda — expor list & remove via menu interativo (hoje proíbe comandos individuais), mantendo a emissão como único meio de adicionar chaves.
- `server-key-provisioning`: o requisito "Public key registration" ganha o fluxo interativo (pedido sequencial de dados) e a emissão continua a ser o único modo de registo de chaves externas.

## Impact

- `elisdb-server/app/keys.py` — adicionar menus/prompts, listar e remover chaves; reutilizar `Database.remove_key`/`list_keys` (já existentes).
- `elisdb-server/test_main.py` — testes do CLI (assinaturas, prompts, recusas).
- Docs `elisdb-server/docs/` — atualizar a secção de configuração/emissão com o modo interativo.
- Dependências: usar apenas a stdlib (`input()`) — sem dependências novas.