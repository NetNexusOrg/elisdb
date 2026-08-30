## Context

Hoje há dois caminhos assimétricos para guardar fotos (ver `proposal.md` - Why):

- **Galeria** (`ClienteRepositoryImpl.stagePhoto` → `decodeScaledBitmap`): já redimensiona para `MAX_PHOTO_DIM = 1280` e re-encoda JPEG q80.
- **Câmara** (`CameraCapture`): grava o JPEG do CameraX diretamente na resolução total do sensor, sem resize.

A identidade de conteúdo é o hash SHA-256 dos bytes (`photo-content-identity`), calculado no cliente antes do upload. O servidor é agnóstico ao formato: guarda bytes + `mime` e devolve o `mime` armazenado. `minSdk = 24`. A orientação EXIF não é tratada em lado nenhum (bug latente na galeria). O `FilePhotoStore.storeBytes` grava sempre `.jpg`.

## Goals / Non-Goals

**Goals:**
- Um único pipeline de encode partilhado pela câmara e pela galeria.
- Câmara deixa de gravar full-res.
- Orientação EXIF aplicada antes do encode.
- Nenhuma alteração ao protocolo de sync nem ao servidor.

**Non-Goals:**
- Não re-encodar fotos já guardadas/sincronizadas (sem migração retroativa).
- Não mudar a identidade por hash nem a semântica de push/pull.
- Não gerar thumbnails separados (uma só representação por foto).

## Decisions

### 1. Um componente `PhotoEncoder` partilhado

Extrair a lógica que já existe em `decodeScaledBitmap` para um único encoder usado por `stagePhoto` (galeria) e pelo novo passo pós-captura da câmara.

- **Alternativa**: `ResolutionSelector`/`ResolutionStrategy` do CameraX para limitar o sensor. Rejeitada porque não garante paridade exata com a galeria e não resolve EXIF nem o formato WebP.

### 2. WebP lossy via `Bitmap.CompressFormat.WEBP` (não `WEBP_LOSSY`)

`WEBP_LOSSY`/`WEBP_LOSSLESS` são API 30. O enum `WEBP` (API 14+) já produz WebP **lossy** e cobre `minSdk = 24`, evitando gates de versão.

- **Alternativas**: AVIF/HEIF — sem encode nativo estável no Android, HEIF tem licenciamento. JPEG q mais baixo — maior que WebP à mesma qualidade. Rejeitadas.

### 3. Parâmetros: máx 1800px, qualidade ~82

1800px dá nitidez aceitável no viewer full-screen; a qualidade 82 em WebP é ≈ JPEG 88-90 percebido. Substitui o `MAX_PHOTO_DIM = 1280` atual.

### 4. Orientação EXIF com `androidx.exifinterface`

Nova dependência `androidx.exifinterface:exifinterface`. A rotação é aplicada aos pixels antes do encode (o `Bitmap.compress` não grava EXIF). A câmara já roda os pixels (CameraX), mas o caminho unificado aplica a rotação de forma inócua quando a orientação é "normal".

### 5. Decode com `inSampleSize` para a câmara

O JPEG full-res da câmara é decodificado com `inSampleSize` (como já faz a galeria) antes do resize, para limitar o pico de memória (um sensor de 48 MP decodificado sem amostragem ≈ 192 MB).

### 6. MIME/extensão no armazenamento

`Foto.mime` passa a `image/webp` por omissão e o `FilePhotoStore.storeBytes` passa a derivar a extensão do MIME (`.webp`/`.jpg`), em vez de gravar sempre `.jpg`. Isto mantém o pull correto quando o servidor devolve fotos antigas JPEG e novas WebP.

## Risks / Trade-offs

- [OOM ao decodificar JPEG full-res da câmara] → `inSampleSize` antes do resize (decisão 5).
- [Re-encodar fotos existentes mudaria o hash e dispararia re-upload/risco de perda] → fora de escopo; o encode só corre em captura/escolha novas (spec `photo-encoding`).
- [WebP não suporta alpha] → irrelevante para fotos.
- [Limite de dimensão do WebP (16383px)] → irrelevante a 1800px.
- [Rotação da bitmap duplica a memória momentaneamente] → aceitável porque o decode já é amostrado.
- [Fotos antigas da galeria já com orientação errada] → não corrigimos retroativamente; apenas fotos novas ficam corretas.

## Migration Plan

Sem migração de dados: apenas fotos novas são WebP; as JPEG já sincronizadas mantêm bytes, hash e MIME. Rollback é seguro porque o servidor é agnóstico ao MIME e nenhum dado existente é tocado.

## Open Questions

- Deve existir, no futuro, um "backfill" opcional para re-encodar fotos JPEG antigas e recuperar espaço? (decisão adiável, não altera specs nem tasks desta mudança).
