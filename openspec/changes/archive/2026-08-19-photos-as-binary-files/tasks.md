## 1. Servidor: modelo e leitura/escrita de fotos

- [x] 1.1 Em `app/models.py`, `FotoOut` passa a `{id, mime, ordem, hash}` e `FotoIn` a `{id, mime, ordem}` (remover `dataB64` de ambos; adicionar `hash` em `FotoOut`)
- [x] 1.2 Em `app/database.py`, `_canonical_from_conn` calcula e devolve `hash` (SHA-256 hex lowercase) em vez de `dataB64`
- [x] 1.3 Em `app/database.py`, `_replace_fotos` passa a registar linhas de metadados (sem escrever bytes) e a ignorar ids sem ficheiro existente
- [x] 1.4 Em `app/database.py`, adicionar `get_photo(foto_id) -> (mime, bytes) | None` e `write_photo(foto_id, bytes)`

## 2. Servidor: endpoints binários

- [x] 2.1 Em `app/main.py`, adicionar `GET /api/photos/{foto_id}` (devolve bytes com media type, ou 404)
- [x] 2.2 Em `app/main.py`, adicionar `PUT /api/photos/{foto_id}` (grava bytes, devolve 204)

## 3. Cliente: DTO e transporte

- [x] 3.1 Em `remote/RemoteDto.kt`, `FotoOutDto` passa a `{id, mime, ordem, hash}` e `FotoInDto` a `{id, mime, ordem}` (remover `dataB64`)
- [x] 3.2 Em `sync/SyncApi.kt`, adicionar `downloadPhoto(id, timestamp): ByteArray` e `uploadPhoto(id, bytes, timestamp)`; implementar em `remote/ApiClient.kt`

## 4. Cliente: store, materialização e push

- [x] 4.1 Em `sync/SyncStatus.kt` (interface `PhotoStore`) e `FilePhotoStore.kt`, adicionar `storeBytes(bytes): String`, `readBytes(uriLocal): ByteArray?` e `contentHashMatches(uriLocal, hash): Boolean`; remover `storeBase64`/`contentMatches`/`loadBase64`
- [x] 4.2 Em `sync/PhotoMaterializer.kt`, `materialize` passa a comparar por hash e a descarregar via `download(id)` quando não corresponde
- [x] 4.3 Em `sync/SyncMapper.kt`, `toRecord` passa a enviar metadados (sem base64)
- [x] 4.4 Em `sync/SyncCoordinator.kt`, no `pushEdits` fazer `PUT` das fotos alteradas (por hash) antes do push; passar `api::downloadPhoto` ao materializer em `applyServerItem` e `mergeDuplicate`

## 5. Testes e verificação

- [x] 5.1 Atualizar `test_main.py`: asserts de `dataB64` passam a `hash`; adicionar testes de `GET`/`PUT /api/photos/{id}` (bytes/mime corretos; 404 para id inexistente)
- [x] 5.2 Correr `pytest` no servidor e `./gradlew :app:assembleDebug` + testes unitários da app
- [x] 5.3 Testar no emulador com o user `opencode`: sync inicial completa sem OOM e lista de clientes com thumbnails