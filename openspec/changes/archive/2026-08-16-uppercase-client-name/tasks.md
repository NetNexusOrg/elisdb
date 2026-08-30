# Tasks: Uppercase client name

## 1. App — form input em maiúsculas

- [x] 1.1 Em `ui/form/ClientFormViewModel.kt`, `onNomeChange`: transformar o valor para maiúsculas (`uppercase()`) antes de atualizar o estado
- [x] 1.2 Atualizar `ClientFormViewModelTest.kt`: cenário novo em que o valor escrito em minúsculas/misturado fica em maiúsculas no estado

## 2. App — escrita local (repository)

- [x] 2.1 Em `data/ClienteRepositoryImpl.kt`, `save()`: aplicar `uppercase()` ao nome em ambos os ramos (criação e edição) após o `.trim()`
- [x] 2.2 Atualizar `ClienteRepositoryImplTest.kt`: cenários novos em que o nome guardado fica em maiúsculas (criação e edição)

## 3. App — sync (pull)

- [x] 3.1 Em `sync/SyncCoordinator.kt`, `applyServerItem()`: aplicar `uppercase()` ao nome vindo do servidor antes de guardar localmente
- [x] 3.2 Atualizar `SyncCoordinatorTest.kt`: cenário em que um registo do pull com nome em minúsculas/misturado fica em maiúsculas na DB local

## 4. Servidor — maiúsculas no push/pull

- [x] 4.1 Em `app/models.py`, `normalize_nome()`: adicionar `.upper()` após o colapso de espaços
- [x] 4.2 Atualizar `test_main.py`: cenário novo em que um nome empurrado em minúsculas/misturado é guardado e devolvido em maiúsculas; confirmar que o `duplicate` com `COLLATE NOCASE` continua a funcionar

## 5. Verificação

- [x] 5.1 Correr `testDebugUnitTest` do Android e os testes pytest do servidor; confirmar que passam
- [x] 5.2 Verificação end-to-end no emulador: criar um cliente com nome em minúsculas, confirmar que aparece em maiúsculas no form, lista e detalhe, e que após sync o servidor guarda em maiúsculas