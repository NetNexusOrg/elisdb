# Tasks: Fix duplicate merge photo loss

## 1. Servidor — resposta duplicate com canónico

- [x] 1.1 Estender o schema `PushResult` (`app/models.py`) com campos opcionais `client` e `fotos` (tipos `ClienteOut`/`FotoOut` reutilizados do pull)
- [x] 1.2 Em `database.py` (`save_client`), devolver o registo canónico completo (client + fotos) no retorno `duplicate`, sem persistir o registo rejeitado nem alterar o canónico
- [x] 1.3 Em `main.py` (`/api/push`), preencher os novos campos da resposta `duplicate` a partir do retorno de `save_client`
- [x] 1.4 Atualizar `test_main.py`/`test_provision.py`: duplicate devolve `existing_id` + canónico com fotos; o registo rejeitado não é persistido; o canónico fica intacto
- [x] 1.5 Correr os testes do servidor e confirmar que passam

## 2. App — DTO e deserialização

- [x] 2.1 Estender `PushResultDto` (`remote/RemoteDto.kt`) com `client: ClienteOutDto?` e `fotos: List<FotoOutDto>?` opcionais (desserialização tolerante a campos ausentes)
- [x] 2.2 Confirmar que `ApiClient.kt` desserializa a resposta push sem quebrar clientes antigos (campos opcionais)

## 3. App — mergeDuplicate com preservação de fotos

- [x] 3.1 Em `SyncCoordinator.kt`, `mergeDuplicate`: usar `clienteDao.upsert` em vez de `insert` (evitar FK CASCADE)
- [x] 3.2 Na resposta `duplicate` com canónico, materializar as fotos canónicas via `PhotoMaterializer` (dois-fases: ficheiro antes da row; ficheiro antigo só após commit) e associá-las ao canónico
- [x] 3.3 Juntar as fotos canónicas com as do registo local (união) antes do re-push; nenhuma foto do canónico é removida
- [x] 3.4 Garantir que o registo fundido fica `pending=true` para o re-push do conjunto completo na sync seguinte
- [x] 3.5 Atualizar `SyncFakes.kt`/`SyncCoordinatorTest.kt`: cenários novo (canónico local presente e ausente) — união de fotos preservada, re-push com conjunto completo, sem perda no servidor
- [x] 3.6 Correr `testDebugUnitTest` (SyncCoordinatorTest e restantes) e confirmar que passam

## 4. Verificação end-to-end

- [x] 4.1 Reconstruir a app (debug), instalar no emulador, e reproduzir um merge de duplicado com fotos nos dois lados; confirmar que nenhuma foto se perde localmente nem no servidor
- [x] 4.2 Reconstruir e instalar o APK de release assinado (scripts/build-release.sh)