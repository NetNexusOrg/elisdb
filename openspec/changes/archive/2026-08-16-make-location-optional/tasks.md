# Tasks: Make location optional

## 1. App — modelo de dados e Room

- [x] 1.1 Em `data/local/Cliente.kt`, tornar `latitude` e `longitude` nullable (`Double?`)
- [x] 1.2 Em `data/ClienteRepositoryImpl.kt`, tornar `latitude`/`longitude` de `ClienteDraft` nullable e copiá-los como tal na criação e edição
- [x] 1.3 Em `data/local/AppDatabase.kt`, adicionar migração 6→7 tornando `latitude` e `longitude` nullable (default NULL) e exportar o novo schema
- [x] 1.4 Atualizar `AppDatabaseMigrationTest.kt` com o cenário de migração 6→7

## 2. App — form

- [x] 2.1 Em `ui/form/ClientFormViewModel.kt`, remover o bloqueio de save sem localização (mensagem "Escolhe a localização no mapa.") e passar coordenadas nullable para o `ClienteDraft`
- [x] 2.2 Atualizar `ClientFormViewModelTest.kt`: substituir `save_semLocalizacaoMarcaMensagem` por cenários em que salvar sem localização persiste o cliente (criar e editar) sem coordenadas

## 3. App — ecrã de detalhe

- [x] 3.1 Em `ui/detalhe/ClientDetailScreen.kt`, renderizar o mapa e a ação "Navegar até aqui" apenas quando `latitude`/`longitude` existem; guardar `openInMap()` para coordenadas null
- [x] 3.2 Adicionar teste (instrumentation) de detalhe para um cliente sem localização: sem mapa e sem ação de navegação

## 4. App — sync e DTOs

- [x] 4.1 Em `remote/RemoteDto.kt`, tornar `latitude`/`longitude` nullable em `ClienteInDto` e `ClienteOutDto`
- [x] 4.2 Atualizar `sync/SyncMapper.kt` (`toRecord`) e `sync/SyncCoordinator.kt` (`applyServerItem`) para pass-through de coordenadas nullable
- [x] 4.3 Atualizar testes de sync/DTO (SyncCoordinatorTest, SyncFakes, ApiClientTest, SyncManagerTest) para clientes sem coordenadas no push e no pull

## 5. Servidor

- [x] 5.1 Em `app/models.py`, tornar `latitude`/`longitude` opcionais (`float | None = None`) em `ClienteIn` e `ClienteOut`
- [x] 5.2 Em `app/database.py`, tornar as colunas `latitude`/`longitude` nullable no schema
- [x] 5.3 Em `test_main.py`, adicionar cenários: push sem coordenadas aceite e persistido; pull devolve coordenadas null; registo com coordenadas continua a funcionar

## 6. Verificação

- [x] 6.1 Correr `testDebugUnitTest` (Android) e os testes pytest do servidor; confirmar que passam
- [x] 6.2 Verificação end-to-end no emulador: criar cliente sem localização, confirmar detalhe sem mapa/navegação, sincronizar e confirmar push/pull sem coordenadas no servidor