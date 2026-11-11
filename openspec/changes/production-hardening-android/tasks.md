# Tasks

## 1. Download de atualizações

- [x] 1.1 Validar que o `filename` publicado é um nome simples e termina em `.apk` antes de escrever.
- [x] 1.2 Adicionar teste de regressão para `../evil.apk`.
- [x] 1.3 Correr `:app:testDebugUnitTest`.

## 2. CI de release

- [x] 2.1 Construir o APK release com keystore efémero no job `test`.
- [x] 2.2 Incluir `SettingsStoreTest` no job instrumentado bloqueante.
- [x] 2.3 Falhar o job de release quando a tag não corresponde a `app.versionName`.
- [x] 2.4 Validar o YAML do workflow.
- [x] 2.5 Correr `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:compileDebugAndroidTestKotlin`.

## 3. Fecho

- [x] 3.1 Validar a change com `openspec validate production-hardening-android --strict`.
- [ ] 3.2 Arquivar após revisão e commit aprovado pelo utilizador.

## Workflow

- Commits e push apenas com confirmação explícita do utilizador.
