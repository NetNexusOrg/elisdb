# Tasks: Incremental photo push

## 1. Servidor — DTO e merge

- [x] 1.1 Em `app/models.py`, tornar `FotoIn.dataB64` opcional (`str | None = None`)
- [x] 1.2 Em `app/database.py`, transformar `_replace_fotos` em merge: foto com `dataB64` escreve/reescreve o ficheiro e a row; foto sem `dataB64` (referência) mantém o ficheiro/row existente se houver, e é ignorada se não houver

## 2. Servidor — testes

- [x] 2.1 Em `test_main.py`, cenários: push com foto referência mantém o ficheiro/row existente; push com referência sem cópia no servidor é ignorado; push com data escreve/atualiza; edição sem fotos alteradas não re-escreve ficheiros

## 3. App — DTOs

- [x] 3.1 Em `remote/RemoteDto.kt`, tornar `FotoInDto.dataB64` nullable (`String? = null`)

## 4. App — estado de push de fotos

- [x] 4.1 Criar `PhotoPushState` (interface + SharedPreferences) em `sync`: `getHashes(clienteId): Map<String,String>` e `setHashes(clienteId, Map<String,String>)`
- [x] 4.2 Atualizar `AppContainer` para injetar `PhotoPushState` no `SyncCoordinator`

## 5. App — SyncMapper/Coordinator

- [x] 5.1 Em `SyncMapper.toRecord`, quando o `PhotoPushState` tiver hash conhecida igual ao conteúdo atual, enviar a foto como referência (sem `dataB64`); caso contrário enviar com dados
- [x] 5.2 No `SyncCoordinator`, após um push com sucesso (ack), guardar os hashes atuais das fotos do cliente em `PhotoPushState`

## 6. App — testes

- [x] 6.1 Atualizar `SyncFakes`/`SyncCoordinatorTest`/`ApiClientTest` para: primeiro push envia tudo com dados; segundo push (sem alterações) envia referências; edição de campos não-foto não re-envia fotos

## 7. Verificação

- [x] 7.1 Correr `testDebugUnitTest` e os testes pytest do servidor; confirmar que passam
- [x] 7.2 Verificação end-to-end no emulador: editar um cliente com fotos e confirmar (nos logs/testes) que as fotos não são re-enviadas; confirmar que as fotos permanecem no servidor
