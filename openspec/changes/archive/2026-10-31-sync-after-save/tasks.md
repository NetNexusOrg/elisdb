# Tasks

## 1. Implementação

- [x] 1.1 `ClientFormViewModel.save`: após sucesso, disparar `repository.syncNow()` em `viewModelScope` sem esperar e só depois emitir `saved`.
- [x] 1.2 Garantir que uma falha de sync não altera a mensagem do formulário nem impede a navegação.

## 2. Testes

- [x] 2.1 Unit: criar cliente dispara um sync; editar cliente dispara um sync; falha de sync não impede `saved`.
- [x] 2.2 Confirmar que o sync não é disparado quando o guardar falha (nome vazio ou erro do repositório).
- [x] 2.3 `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:connectedDebugAndroidTest` verdes.

## 3. Documentação e fecho

- [x] 3.1 `elisdb-android/docs/uso.md`: guardar dispara sincronização em background.
- [x] 3.2 `CHANGELOG.md`: novidade.
- [x] 3.3 `openspec validate sync-after-save --strict` e marcar as caixas.
