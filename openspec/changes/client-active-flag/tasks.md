# Tasks

## 1. Servidor

- [x] 1.1 `models.py`: `ativo: bool = True` em `ClienteIn` e `ClienteOut`.
- [x] 1.2 `database.py`: coluna `ativo` no `SCHEMA`, migração idempotente com `PRAGMA table_info` e normalização para booleano no insert/update.
- [x] 1.3 `database.py`: incluir `ativo` no cliente canónico, no LWW e em `build_audit_details` (chave `ativo`, valores “Ativo”/“Inativo”).
- [x] 1.4 `main.py`: push/pull a propagar `ativo` (sem endpoint novo).
- [x] 1.5 Testes pytest: default ativo, push inativo, pull devolve estado, payload sem campo, migração de BD legacy, histórico regista ativação/desativação.

## 2. Android — dados

- [x] 2.1 `Cliente.ativo: Boolean = true`; `MIGRATION_9_10` (`ALTER TABLE clientes ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1`); `version = 10` e schema `10.json`.
- [x] 2.2 `ClienteDraft.ativo` e `save` a gravar o campo (com `revision`/`updated_at` como os restantes).
- [x] 2.3 `RemoteDto`: `ativo` em `ClienteInDto`/`ClienteOutDto`/`ClienteComFotosDto` e `SyncMapper` a mapear nos dois sentidos; cliente antigo sem campo assume `true`.

## 3. Android — UI

- [x] 3.1 `ClientFormUiState`/`ClientFormViewModel`: estado `ativo` (default `true`) e envio no `ClienteDraft`.
- [x] 3.2 `ClientFormScreen`: `Switch` “Cliente ativo” com etiqueta e descrição, antes da localização.
- [x] 3.3 `ClientDetailScreen`: chip “Inativo” ao lado do nome quando o cliente está inativo.
- [x] 3.4 `ClientListScreen`/`ClientListViewModel`: `ativo` no `ClientListItem`, etiqueta “Inativo” e conteúdo atenuado no cartão, sem toggle.
- [x] 3.5 `ClientLogScreen`: mostrar “Ativou o cliente” / “Desativou o cliente” a partir de `details.fields`.

## 4. Testes

- [x] 4.1 Unit: view model do formulário (default ativo, desativar ao guardar) e mapeamento do sync.
- [x] 4.2 Instrumentados: migração 9→10 (registo antigo fica ativo), save com `ativo=false`, detalhe e lista com etiqueta de inativo.
- [x] 4.3 `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:connectedDebugAndroidTest` e `pytest` verdes.

## 5. Documentação e fecho

- [x] 5.1 `elisdb-android/docs/uso.md`: explicar o toggle, a etiqueta e o histórico.
- [x] 5.2 `elisdb-server/docs/configuracao.md`: contrato `ativo`, default e ordem de deploy (servidor primeiro).
- [x] 5.3 `CHANGELOG.md`: novidade do estado ativo/inativo.
- [x] 5.4 `openspec validate client-active-flag --strict` e marcar as caixas concluídas.
