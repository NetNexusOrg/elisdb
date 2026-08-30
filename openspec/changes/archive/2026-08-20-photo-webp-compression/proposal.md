## Why

As fotos tiradas com a câmara são gravadas na resolução total do sensor (tipicamente 2–20 MB cada), porque a câmara só aplica `JPEG 80` sem redimensionar, enquanto as fotos da galeria já são reduzidas a 1280px. Isto gasta armazenamento local, encarece o sync e é inconsistente entre as duas origens. Queremos comprimir todas as fotos antes de guardar — resize + WebP lossy — mantendo qualidade visível.

## What Changes

- Todas as fotos (câmara **e** galeria) passam a ser redimensionadas para um máximo de 1800px no lado maior e re-encodadas para **WebP lossy** (~82%) antes de serem guardadas.
- A captura da câmara deixa de gravar o JPEG full-res diretamente e passa a usar o mesmo pipeline de encode da galeria.
- A orientação EXIF é aplicada antes do re-encodo (corrige rotação errada em fotos da galeria, um bug latente hoje).
- O MIME guardado passa a ser `image/webp` e os ficheiros são gravados com extensão `.webp`.
- **Sem migração**: fotos já guardadas/sincronizadas (JPEG) **não** são re-encodadas. Só novas capturas/escolhas são afetadas, para não alterar os hashes de conteúdo nem disparar re-upload e arriscar perda de dados no servidor.
- Nenhuma alteração no servidor (é agnóstico ao MIME e guarda bytes + `mime`).

## Capabilities

### New Capabilities

- `photo-encoding`: pipeline único de encode (decode com `inSampleSize`, rotação EXIF, redimensionamento para um máximo de 1800px, compressão WebP lossy ~82%) partilhado pela câmara e pela galeria, com regras para não re-encodar fotos existentes.

### Modified Capabilities

- `client-photos`: a captura e a escolha de fotos passam a produzir ficheiros WebP redimensionados em vez de guardar o original/full-res, e a câmara deixa de gravar a resolução total do sensor.

## Impact

- **App Android**:
  - `ui/form/CameraCapture.kt` — captura deixa de gravar full-res direto; resultado passa pelo pipeline de encode.
  - `data/ClienteRepositoryImpl.kt` — `stagePhoto`/`decodeScaledBitmap` passam a produzir WebP; extrair o pipeline para reuso pela câmara.
  - `sync/FilePhotoStore.kt` e `sync/SyncStatus.kt` (`PhotoStore`) — `storeBytes` passa a gravar com extensão baseada no MIME, não sempre `.jpg`.
  - `data/local/Foto.kt` — default de `mime` passa a `image/webp`.
  - Nova dependência `androidx.exifinterface:exifinterface` para a rotação EXIF.
- **Servidor**: sem alterações (guarda bytes e devolve o `mime` armazenado).
- **Sync**: sem alterações ao protocolo; a identidade por hash de conteúdo mantém-se e o re-encodo ocorre antes do cálculo do hash.
