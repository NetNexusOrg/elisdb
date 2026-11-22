# Tasks

## 1. Servidor: persistência da flag

- [x] 1.1 Adicionar `preferida INTEGER NOT NULL DEFAULT 0` ao schema de `fotos` e a migração idempotente `_migrate_fotos_preferida` em `elisdb-server/elisdb/database.py`; verificar com um teste que abre uma base antiga e confirma que todas as fotos continuam lá e ficam desmarcadas (`python -m pytest tests/test_database.py -k migra`).
- [x] 1.2 Adicionar `preferida: bool | None = None` a `FotoIn` em `elisdb-server/elisdb/models.py`; verificar que um payload antigo (sem o campo) continua a validar (`python -m pytest tests/test_api.py`).
- [x] 1.3 Atualizar `_replace_fotos` para gravar a flag com `COALESCE(excluded.preferida, fotos.preferida, 0)` e normalizar para no máximo uma foto marcada por cliente (primeira por `ordem, id`); verificar com testes de push: marcar, limpar com `false`, preservar com `None` e duplicados.
- [x] 1.4 Incluir `preferida` nas queries e respostas de `pull`/`_canonical_from_conn`; verificar que o pull e a resposta de duplicado devolvem a flag (`python -m pytest tests/test_sync_integrity.py tests/test_api.py`).

## 2. Android: modelo e migração

- [x] 2.1 Adicionar `preferida: Boolean = false` a `Foto` (`data/local/Foto.kt`), subir `AppDatabase` para a versão 13, criar `MIGRATION_12_13` (`ALTER TABLE fotos ADD COLUMN preferida INTEGER NOT NULL DEFAULT 0`) e registá-la em `AppContainer.kt`; verificar que o schema exportado é atualizado e que `AppDatabaseMigrationTest` passa (`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.elisdb.data.AppDatabaseMigrationTest`).
- [x] 2.2 Normalizar a foto preferida em `ClienteRepositoryImpl.save()` (primeira marcada ou, na falta dela, a primeira do draft), sem alterar `ordem`; verificar com teste unitário que: uma marcada fica marcada, nenhuma marcada escolhe a primeira, e várias marcadas deixam só uma.

## 3. Android: edição e lista

- [x] 3.1 Adicionar `onSetProfilePhoto` ao `ClientFormViewModel`, marcar automaticamente a primeira foto adicionada a um cliente sem fotos e reassinalar à primeira restante quando a preferida é removida; verificar com testes em `ClientFormViewModelTest`.
- [x] 3.2 Adicionar a estrela no canto superior esquerdo de cada célula de `PhotoGrid` (`ui/form/ClientFormScreen.kt`), preenchida na preferida e em contorno nas restantes, com `contentDescription` e `testTag`; verificar com teste de UI que tocar na estrela de outra foto move a marca e que só uma está preenchida.
- [x] 3.3 Atualizar `ClientListViewModel` para usar a foto preferida e cair na primeira quando nenhuma está marcada; verificar com teste unitário os três casos (marcada, sem marca com fotos, sem fotos).

## 4. Sync

- [x] 4.1 Adicionar `preferida` a `FotoInDto`/`FotoOutDto` (`Boolean` no push, `Boolean?` no pull), enviá-la em `SyncMapper.toRecord` e incluí-la na chave de idempotência de `SyncCoordinator`; verificar com teste que o payload contém a flag e que mudar só a flag muda a chave.
- [x] 4.2 Aplicar a flag no pull e no merge de duplicados: incluir `preferida` ao construir `Foto` em `Repointed` e, em `Keep`, atualizar a linha só quando o valor recebido é não-nulo e difere do local, sem download nem escrita de ficheiro; verificar com testes em `SyncCoordinatorTest` (flag atualizada, `null` preserva a local, conteúdo inalterado não gera download).

## 5. Verificação integrada

- [x] 5.1 Correr os testes unitários e o lint Android (`./gradlew :app:testDebugUnitTest :app:lintDebug`) e confirmar que não há regressões.
- [x] 5.2 Correr a suite do servidor (`python -m pytest`) e confirmar que os testes existentes continuam verdes.
- [x] 5.3 Validar no emulador: adicionar duas fotos, marcar a segunda como perfil, confirmar a miniatura na lista, remover a preferida e confirmar a reassinalação.
- [x] 5.4 Validar a change com `openspec validate add-client-profile-photo --strict` e confirmar que todos os deltas fecham.

## Workflow follow-up

- Acrescentar a entrada em **Novidades** do `elisdb-android/CHANGELOG.md` na próxima versão, ao preparar a release.
- Arquivar a change depois da revisão e dos testes aprovados.
