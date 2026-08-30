# Tasks: Sync deletion tombstones

## 1. Servidor — tabela e delete

- [x] 1.1 Em `app/database.py`, criar tabela `tombstones (id TEXT PRIMARY KEY, deleted_at INTEGER NOT NULL)` no SCHEMA
- [x] 1.2 Alterar `delete_client` para aceitar `deleted_at` (parâmetro) e escrever o tombstone ao remover o cliente
- [x] 1.3 Em `app/main.py`, `DELETE /api/clients/{id}` aceitar `deleted_at` query param opcional e passá-lo a `delete_client`

## 2. Servidor — push com tombstones

- [x] 2.1 Em `save_client`, honrar tombstone: se `id` tem tombstone e `updated_at <= deleted_at`, devolver `stale`; se `updated_at > deleted_at`, remover o tombstone e prosseguir (ressurreição)
- [x] 2.2 Em `pull_all`, devolver também os tombstones (filtrar por `deleted_at > since` quando `since` dado)

## 3. Servidor — resposta pull

- [x] 3.1 Em `app/main.py`, mudar a resposta do `GET /api/pull` para `{"records": [...], "tombstones": [...], "next_since": ...}` em todos os casos
- [x] 3.2 Em `app/models.py`, adicionar modelo `TombstoneOut` (id, deleted_at)

## 4. Servidor — testes

- [x] 4.1 Em `test_main.py`, cenários: delete escreve tombstone; push de id tombstoned com updated_at antigo → `stale`; push mais recente → ressuscita; pull devolve tombstones; pull incremental filtra tombstones por `since`

## 5. App — DTOs e delete

- [x] 5.1 Em `remote/RemoteDto.kt`, adicionar `TombstoneDto` e `PullResponseDto` (records, tombstones, next_since)
- [x] 5.2 Em `ApiClient.pull`, ler o novo shape (`PullResponseDto`)
- [x] 5.3 Em `ApiClient.delete`, enviar `deleted_at` query param

## 6. App — SyncCoordinator

- [x] 6.1 Em `SyncCoordinator.pull`, aplicar tombstones localmente: apagar registos locais (sem pending) cujo id está num tombstone
- [x] 6.2 No delete, enviar o timestamp do dispositivo (`deleted_at`)

## 7. App — testes

- [x] 7.1 Atualizar `SyncFakes`/`SyncCoordinatorTest`/`ApiClientTest` para o novo shape de pull e para o cenário de tombstone aplicado localmente

## 8. Verificação

- [x] 8.1 Correr `testDebugUnitTest` e os testes pytest do servidor; confirmar que passam
- [x] 8.2 Verificação end-to-end no emulador: apagar um cliente num dispositivo e confirmar que outro dispositivo (pull incremental) remove o registo local
