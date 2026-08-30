## 1. Marcação de origem das chaves

- [x] 1.1 Em `app/database.py`, adicionar coluna `provisioned` à tabela `keys` no `SCHEMA`, migração `_migrate_keys_provisioned`, e invocá-la em `connect()`
- [x] 1.2 Em `app/database.py`, `add_key` passa a aceitar `provisioned: bool = False` e a persistir o valor; `list_keys` passa a devolver `provisioned`
- [x] 1.3 Em `app/keys.py`, `issue_and_register` propaga o flag `provisioned` para `add_key`

## 2. Revogação no arranque

- [x] 2.1 Em `app/provision.py`, `provision_users` marca as chaves emitidas como provisionadas, revoga chaves `provisioned` cujo `comment` não está na lista e apaga o `.config.json` respetivo; passa a devolver `(provisioned, revoked)`
- [x] 2.2 Em `app/provision.py`, `main()` imprime também os utilizadores revogados

## 3. Testes

- [x] 3.1 Atualizar `test_provision.py` para desempacotar o novo retorno e adicionar testes: (a) remover utilizador do `ELISDB_USERS` revoga a chave provisionada e apaga o config; (b) chave manual não é revogada
- [x] 3.2 Atualizar `test_main.py` (`test_comment_column_migrates_an_existing_db`) para o novo campo `provisioned`
- [x] 3.3 Correr `pytest` no servidor