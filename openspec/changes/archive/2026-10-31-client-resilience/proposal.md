# Proposal

## Why

A segunda revisão da app encontrou dois defeitos que ainda perdem dados e quatro fragilidades que já vinham da primeira. O mais grave é o ack do push: o registo é marcado como sincronizado comparando `updatedAt`, que tem resolução de **um segundo** — uma edição feita no mesmo segundo do snapshot é limpa (`pending = false`) e nunca chega ao servidor. Logo a seguir vem o oposto: depois de `sync-integrity`, um registo que fica pendente para sempre (porque o push devolve `error`) segura o watermark indefinidamente, e cada sincronização volta a trazer tudo o que vem depois dele.

## What Changes

- **Revisão local do cliente**: cada gravação incrementa `revision`; o ack faz `UPDATE ... WHERE id = ? AND revision = ?`, pelo que uma edição concorrente deixa o registo pendente em vez de a apagar.
- **Watermark deixa de ficar preso**: um registo pendente há mais de sete dias deixa de segurar o watermark (continua pendente e é reenviado, mas não bloqueia os restantes).
- **Gravar um cliente que já não existe** deixa de anunciar sucesso: a UI mostra a mensagem.
- **Mudar de servidor limpa o estado de sincronização**: watermark, hashes de fotos, offset de relógio e último estado, para os dados de dois servidores não se misturarem.
- **Edições usam o relógio do servidor**: o repositório passa a receber o `clockOffsetStore`.
- **Migração do seed à prova de falha**: grava primeiro a cópia cifrada, só depois limpa o texto simples, e um Keystore indisponível não impede a app de abrir.
- **Timeouts por pedido**: o download do APK e o envio/descarga de fotos deixam de estar limitados aos 30 s globais.
- Ajustes: ordem das fotos na relação, insets do visualizador e do mapa, `_saved` com buffer, ciclo de vida do mapa.

## Capabilities

### New Capabilities

- `client-resilience`: garantias de que nenhuma edição local se perde e de que um registo problemático não paralisa a sincronização.

### Modified Capabilities

<!-- nenhuma: os requisitos existentes continuam verdadeiros; os novos vivem em `client-resilience`. -->

## Impact

- **Android**: `Cliente` (+`revision`), `MIGRATION_8_9`, `ClienteDao.markSynced`, `ClienteRepositoryImpl`, `SyncCoordinator`, `SyncMetaDao` (+`clear`), `PhotoPushState` (+`clearAll`), `ServerConfigViewModel`, `SettingsStore`, `ApiClient`, `ClientFormViewModel`, `OsmMap`, visualizador e mapa em fullscreen.
- **Servidor**: nenhuma alteração — `revision` e o limite do watermark são estado local do cliente.
- **Fora de âmbito**: os baixos da revisão (diálogo de alterações por guardar, remoção do WorkManager, retry concorrente do histórico, APK parcial) e o cleartext global.
