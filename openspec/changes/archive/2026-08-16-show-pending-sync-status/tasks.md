# Tasks: Show pending sync status

## 1. Servidor — endpoint de "latest"

- [x] 1.1 Em `app/main.py`, adicionar `GET /api/latest` (autenticado) que devolve `{"latest_updated_at": <int>}`
- [x] 1.2 Em `app/database.py`, adicionar query que devolve o máximo de `clientes.updated_at` e `tombstones.deleted_at` (0 se vazio)
- [x] 1.3 Em `test_main.py`, cenários: endpoint devolve o máximo dos clientes e tombstones; 0 quando não há dados; exige auth

## 2. App — expor estado de pendências

- [x] 2.1 Em `sync/SyncManager.kt`, adicionar observáveis `hasPendingUploads`/`hasPendingDownloads` derivados de um `ClienteDao` e do `PullWatermark` injetados (novos parâmetros opcionais): uploads = `getPending()`/`getPendingDeletions()` não vazio; downloads = `latest > watermark` (atualizado após cada fetch)
- [x] 2.2 Em `sync/SyncApi.kt`/`remote/ApiClient.kt`, adicionar `fetchLatestUpdatedAt()` que chama `GET /api/latest`
- [x] 2.3 Atualizar `AppContainer.kt` para passar `database.clienteDao()` e o `RoomPullWatermark` ao `SyncManager`

## 3. Combinar com o status de sync

- [x] 3.1 Em `sync/SyncManager.kt`, expor um estado combinado que aplica a lógica D1/D3: pendências presentes + status idle/success antigo → estado "pending" (upload/download); sync em progresso → IN_PROGRESS; sucesso recente sem pendências → SUCCESS
- [x] 3.2 Após um pull com sucesso, re-verificar o `latest` para limpar `hasPendingDownloads` (o watermark avança)
- [x] 3.3 Manter os `SyncStatus`/`SyncFault`/`lastResult` atuais para não quebrar consumidores existentes

## 4. UI — Settings

- [x] 4.1 Em `SettingsViewModel.kt`, observar o estado combinado do `SyncManager` e expor `hasPendingUploads`/`hasPendingDownloads` (ou o estado derivado) em `SettingsUiState`; ao abrir as Definições, disparar um fetch do `latest` para avaliar downloads pendentes
- [x] 4.2 Em `SyncStatusBar.kt`/`syncMessage`, adicionar os estados "Sincronização por fazer" e "Há atualizações para receber" (ícones + mensagens), preservando a posição fixa da ação de sync
- [x] 4.3 Atualizar `SettingsScreen`/`SettingsViewModelTest` (instrumentação) para os cenários: pendências de upload e download mostram os estados; após sync bem-sucedido mostra "Sincronizado"; estado sobrevive à navegação

## 5. Testes

- [x] 5.1 Adicionar testes unitários do `SyncManager` para a derivação: uploads pendentes → pending; downloads pendentes → pending; sync OK sem pendências → SUCCESS; durante sync → IN_PROGRESS
- [x] 5.2 Adicionar testes de `SyncStatusBar` para os estados pending (posição e mensagem)

## 6. Verificação

- [x] 6.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 6.2 Verificação no emulador: criar um cliente e confirmar "Sincronização por fazer"; noutro dispositivo criar/editar um cliente, sincronizar no primeiro e confirmar "Há atualizações para receber"; após sync manual confirmar "Sincronizado"
