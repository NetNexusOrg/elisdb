# Proposal

## Why

Ficam riscos residuais de baixa/média severidade que não bloqueiam o release, mas que reduzem a defesa em profundidade e a qualidade operacional: cleartext global, fallback silencioso para preferências em texto simples, upload de fotos com todos os bytes em memória, falta de validação de fotos descarregadas, orientações EXIF espelhadas, histórico sem paginação, auditoria sem deletes, pipeline sem smoke de sync e ausência de backup documentado. Esta change fecha esses flancos sem alterar o modelo funcional.

## What Changes

- O Android restringe cleartext aos hosts privados já aceites pela política de URL e deixa de cair silenciosamente para armazenamento em texto simples quando o Keystore falha; a falha é explícita e acionável.
- O push de fotos lê e envia uma foto de cada vez, em vez de carregar todos os bytes de um cliente para memória.
- As fotos descarregadas são validadas contra o MIME declarado (magic bytes) antes de serem materializadas.
- A orientação EXIF espelhada é aplicada corretamente na codificação.
- O histórico de edições passa a ser paginado e a auditoria regista também as deleções.
- O cliente e o servidor suportam uma chave de idempotência no push, para que um retry após timeout não repita uploads/persistências já aplicados.
- O pipeline de release passa a exigir um smoke test de sync (push/pull/delete) além da migração Room.
- É fornecido um procedimento/script de backup e restauro consistente de SQLite + fotos, com verificação.
- Corrige-se a documentação que refere osmdroid quando o código usa MapLibre.

## Capabilities

### New Capabilities

- `client-network-security`: política de rede do Android (cleartext restrito, validação de endpoints e postura fail-closed).
- `server-backup`: backup e restauro consistentes do estado do servidor (SQLite + fotos), com verificação.

### Modified Capabilities

- `client-settings`: armazenamento da chave falha de forma explícita em vez de usar texto simples.
- `photo-encoding`: orientações EXIF espelhadas são aplicadas.
- `photo-content-identity`: bytes descarregados são validados contra o MIME declarado.
- `client-sync`: upload de fotos em streaming e idempotência de push.
- `client-sync-state`: editar um registo marcado para deleção não o deixa num estado impossível.
- `server-sync`: histórico paginado, auditoria de deletes e idempotência de push.
- `release-pipeline`: smoke test de sync como gate de release.

## Impact

- Android: `SettingsStore`, `ApiClient`/`SyncCoordinator`, `PhotoEncoder`, `PhotoMaterializer`, `ClienteRepositoryImpl`, CI e testes.
- Servidor: `app/database.py`, `app/main.py`, scripts de backup, documentação e testes.
- Operação: nova rotina de backup; CI mais lenta no gate de release.
- API: campo aditivo de idempotência e paginação do histórico; compatível para clientes antigos.
