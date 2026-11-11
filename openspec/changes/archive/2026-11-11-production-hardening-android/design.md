# Design

## Context

O CI Android só construía o APK release em tags, os testes do armazenamento
seguro corriam num job não bloqueante, e o download de atualizações usava
diretamente o `filename` publicado pelo servidor. Estas lacunas são corrigidas
sem alterar a arquitetura da app.

## Decisions

### Release build em pull requests

O job `test` gera um keystore efémero com `keytool` e exporta `KEYSTORE_PATH`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`, que o `build.gradle.kts` já
aceita. Assim o R8 corre em cada PR sem usar a chave real de release.

### Armazenamento seguro como gate

`SettingsStoreTest` junta-se a `AppDatabaseMigrationTest` no job instrumentado
bloqueante. O job `instrumented-suite` continua não bloqueante para os restantes
testes de UI.

### Validação da tag

O job de release compara `${GITHUB_REF_NAME#v}` com `app.versionName` lido de
`gradle.properties` e falha imediatamente em caso de divergência.

### Nome do APK

`downloadRelease` exige que o nome seja um único segmento (`filename` igual ao
seu basename, diferente de `.`/`..`) e termina em `.apk`. Rejeitar em vez de
sanitizar evita ambiguidade e escrita fora de `cacheDir/updates`.

## Risks / Trade-offs

- O build release em PR aumenta o tempo de CI; é aceitável para apanhar falhas
  de R8 antes da tag.
- A política de cleartext HTTP e a encriptação de PII em repouso ficam fora
  desta change, por exigirem uma decisão de produto/operação.
