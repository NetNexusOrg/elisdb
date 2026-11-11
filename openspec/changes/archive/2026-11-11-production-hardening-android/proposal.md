# Proposal

## Why

A auditoria de produção encontrou lacunas no processo de release Android: o APK
minificado (R8) só era construído depois de criar a tag, a tag podia divergir da
versão publicada, os testes do armazenamento seguro não bloqueavam releases e o
download de atualizações confiava no nome de ficheiro enviado pelo servidor.

## What Changes

- Exigir a construção do APK release (R8) em cada pull request, com uma chave de
  assinatura efémera fornecida pelo CI.
- Fazer os testes do `SettingsStore` (armazenamento encriptado) correr no job
  instrumentado bloqueante, em conjunto com os testes de migração Room.
- Falhar o job de release quando a tag `vX.Y.Z` não corresponder ao
  `app.versionName` de `gradle.properties`.
- Aceitar apenas nomes de ficheiro APK simples no download de atualizações
  (sem separadores de caminho e com sufixo `.apk`).

## Capabilities

### New Capabilities
<!-- nenhuma -->

### Modified Capabilities

- `app-build`: o build release passa a ser verificado antes de existir uma tag e
  os testes do armazenamento seguro passam a ser bloqueantes.
- `client-update`: o nome do APK publicado é validado antes de escrever o
  ficheiro.
- `release-pipeline`: a tag de release tem de corresponder à versão da app.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/remote/ApiClient.kt`
- `elisdb-android/app/src/test/java/com/example/elisdb/remote/ApiClientTest.kt`
- `.github/workflows/android-ci.yml`
- Sem alterações ao servidor, à base de dados, à versão da app ou ao
  `CHANGELOG.md`. A política de cleartext HTTP fica como decisão separada.
