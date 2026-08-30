## Why

O pull devolve todas as fotos como base64 embutidas num único JSON. No primeiro sync isto soma ~30 MB, que a app carrega inteiro em memória e rebenta (OutOfMemoryError no emulador; frágil em qualquer dispositivo à medida que os dados crescem). As fotos já são ficheiros em disco nos dois lados — o base64 é apenas o transporte HTTP. Para ficar coerente, a foto passa a viajar sempre em binário, nos dois sentidos.

## What Changes

- O `pull` deixa de embutir bytes: cada foto passa a ser devolvida como metadados (id, mime, ordem) + hash de conteúdo (SHA-256).
- O `push` deixa de embutir bytes: envia apenas metadados das fotos (id, mime, ordem).
- Novos endpoints autenticados: `GET /api/photos/{id}` (download binário) e `PUT /api/photos/{id}` (upload binário).
- A app descarrega/carrega cada foto individualmente (binário, ~1 MB cada, sem pico de memória) e salta as que já tem com o mesmo hash (skip por identidade de conteúdo).

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-photo-storage`: o pull devolve metadados + hash (sem bytes); endpoints binários de download e upload.
- `sync-incremental-pull`: a entrega de fotos passa de base64 inline para download binário por hash.
- `client-photos`: o envio de fotos alteradas passa de base64 para upload binário.
- `photo-content-identity`: o hash decide se a foto é reenviada; o reenvio é binário, não base64.

## Impact

- Servidor: `app/models.py` (`FotoOut` com `hash`; `FotoIn` sem `dataB64`), `app/database.py` (`_canonical_from_conn` calcula hash; `get_photo`; `write_photo`), `app/main.py` (endpoints `GET`/`PUT /api/photos/{id}`).
- Android: `remote/RemoteDto.kt`, `remote/ApiClient.kt` + `sync/SyncApi.kt` (`downloadPhoto`/`uploadPhoto`), `sync/PhotoStore.kt`/`FilePhotoStore.kt` (`storeBytes`/`readBytes`/`contentHashMatches`), `sync/PhotoMaterializer.kt`, `sync/SyncCoordinator.kt`, `sync/SyncMapper.kt`.
- Testes: `elisdb-server/test_main.py` (asserts de `dataB64` para `hash` + endpoints binários).