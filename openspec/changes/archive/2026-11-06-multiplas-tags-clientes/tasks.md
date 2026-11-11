# Tasks

## 1. Servidor

- [x] 1.1 Subir `ClienteIn.tag` de 100 para 500 caracteres e adicionar validador que divide por `;`, faz trim/uppercase/dedupe/sort e rejeita >10 tags, >100 chars por tag ou >500 no total
- [x] 1.2 Atualizar os testes de `pytest` para normalização, tag única legada, limites e auditoria; correr `python -m pytest`

## 2. Android — dados e formulário

- [x] 2.1 Trocar `ClienteDraft.tag` por `tags: List<String>` e juntar com `;` em `ClienteRepositoryImpl.save`, com normalização e limites
- [x] 2.2 Adaptar `ClientFormUiState`/`ClientFormViewModel` para `tags`, `onAddTag`/`onRemoveTag` e validação dos limites
- [x] 2.3 Substituir o campo único de Tag em `ClientFormScreen` por um botão **Adicionar tag** que revela o campo + chips removíveis, abaixo das notas
- [x] 2.4 Atualizar os testes unitários do formulário e correr `:app:testDebugUnitTest`

## 3. Android — lista, detalhe, mapa e histórico

- [x] 3.1 Passar `ClientListItem` a `tags: List<String>` e mostrar todas as tags em badges com wrap à direita em `ClientListScreen`
- [x] 3.2 Mostrar todas as tags em chips no `ClientDetailScreen`
- [x] 3.3 Adaptar o filtro da lista e do mapa para derivar as tags do campo `tag` e mostrar clientes que contenham a tag escolhida
- [x] 3.4 Apresentar alterações ao conjunto de tags no histórico (`"tag" -> "Tags"`) e dividir os valores por `;`
- [x] 3.5 Atualizar testes unitários/Compose de lista, detalhe, mapa e filtros

## 4. Documentação

- [x] 4.1 Atualizar `elisdb-android/docs/uso.md` com o formato `A;B`, chips e filtro por tag
- [x] 4.2 Atualizar `elisdb-server/CHANGELOG.md`.

## 5. Verificação final

- [x] 5.1 Correr `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- [x] 5.2 Correr `./gradlew :app:connectedDebugAndroidTest`
- [x] 5.3 Validar a change com `openspec validate multiplas-tags-clientes`
- [x] 5.4 Smoke test no emulador: criar cliente com 3 tags, filtrar por uma, editar/remover tags e confirmar o histórico

## Workflow follow-up

- Sincronizar as specs e arquivar a change após revisão.
- Releases (com confirmação): servidor 1.5.0 primeiro, app 1.12.0 depois.
