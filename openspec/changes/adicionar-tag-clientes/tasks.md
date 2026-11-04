# Tasks

## 1. Servidor — modelo e persistência

- [x] 1.1 Adicionar `tag: str | None = Field(None, max_length=100)` a `ClienteIn` e `ClienteOut` em `elisdb-server/app/models.py` e confirmar que os testes de API continuam a validar o modelo
- [x] 1.2 Adicionar `tag TEXT` ao `SCHEMA`, criar `_migrate_clients_tag()` idempotente e chamá-la em `_apply_schema` antes do rebuild de coordenadas, incluindo `tag` no `CREATE`/`INSERT` desse rebuild
- [x] 1.3 Incluir `tag` no `SELECT`/`UPDATE`/`INSERT` de `save_client`, nos dicionários de auditoria e em `build_audit_details`
- [x] 1.4 Incluir `tag` em `list_clients` e `_canonical_from_conn`
- [x] 1.5 Adicionar testes `pytest` de round-trip, auditoria, limite de 100 caracteres, push/pull e migração de base legada; correr `python -m pytest` em `elisdb-server/`

## 2. Android — dados e sincronização

- [x] 2.1 Adicionar `tag: String? = null` a `Cliente` e `ClienteDraft`; persistir em `ClienteRepositoryImpl.save()` com trim e branco → null
- [x] 2.2 Bump de `AppDatabase.version` para 11, criar `MIGRATION_10_11` (`ALTER TABLE clientes ADD COLUMN tag TEXT`) e registá-la em `AppContainer`
- [x] 2.3 Adicionar `tag` a `ClienteInDto`/`ClienteOutDto`, `SyncMapper.toRecord` e ao mapeamento de pull em `SyncCoordinator`
- [x] 2.4 Atualizar `AppDatabaseMigrationTest` (cadeia + teste 10→11) e os testes unitários de repositório/mapeamento; correr `:app:testDebugUnitTest`

## 3. Android — formulário, detalhe e lista

- [x] 3.1 Adicionar `tag` a `ClientFormUiState`, carregamento, `onTagChange` (limite de 100 caracteres) e `ClienteDraft`; adicionar o campo "Tag" entre Contrato e Nome em `ClientFormScreen`
- [x] 3.2 Mostrar a tag como etiqueta abaixo do nome em `ClientDetailScreen`
- [x] 3.3 Adicionar `tag` a `ClientListItem` e mostrar a etiqueta na linha secundária de `ClientListScreen` (sem alterar `matchesQuery`)
- [x] 3.4 Adicionar o rótulo `"tag" -> "Tag"` em `ClientLogScreen`
- [x] 3.5 Atualizar/criar testes unitários e Compose do formulário, detalhe e lista; correr `./gradlew :app:testDebugUnitTest`

## 4. Filtro por tag

- [x] 4.1 Derivar as tags disponíveis dos clientes guardados (distintas, em maiúsculas e ordenadas) e aplicar o filtro de tag antes da pesquisa livre em `ClientListViewModel`
- [x] 4.2 Listar as tags e "Todas as tags" no menu do ícone de filtros, marcar a seleção e destacar o ícone quando há filtro de tag ativo
- [x] 4.3 Adicionar testes unitários do filtro de tag e um teste Compose que escolhe uma tag e confirma que só aparecem os clientes correspondentes; correr `:app:testDebugUnitTest`

## 5. Filtro por tag no mapa

- [x] 5.1 Adicionar o filtro de tag ao `ClientMapViewModel` e expor as tags disponíveis, independentes do filtro da lista
- [x] 5.2 Mostrar as secções expansíveis Tags e Estado no menu do mapa
- [x] 5.3 Adicionar testes unitários do filtro de tag do mapa e um teste Compose das tags no menu

## 6. Verificação final

- [x] 6.1 Correr `./gradlew assembleDebug lintDebug` e confirmar build verde
- [x] 6.2 Correr `./gradlew connectedDebugAndroidTest` (migração + Compose) num emulador
- [x] 6.3 Validar a change com `openspec validate adicionar-tag-clientes`
- [x] 6.4 Smoke test: criar/editar tag, ver na lista e no detalhe, sincronizar e confirmar persistência após reiniciar a app

## Workflow follow-up

- Sincronizar as specs e arquivar a change após revisão.
