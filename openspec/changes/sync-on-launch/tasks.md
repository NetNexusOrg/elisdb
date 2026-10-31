# Tasks

## 1. Implementação

- [x] 1.1 `ElisdbApp`: lançar o sync inicial no `applicationScope`, depois de criar o `container`.
- [x] 1.2 Extrair `initialSyncIfConfigured(settingsStore, syncNow)` com gate pela `signingKey` e documentar o porquê.
- [x] 1.3 Actualizar o comentário do arranque que ainda diz “synchronization is manual only”.

## 2. Testes

- [x] 2.1 Unit: com chave configurada chama o sync uma vez; sem chave não chama.
- [x] 2.2 Correr `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:connectedDebugAndroidTest`.

## 3. Documentação e fecho

- [x] 3.1 `elisdb-android/docs/uso.md`: dizer que abrir a app sincroniza automaticamente, além de guardar e do botão manual.
- [x] 3.2 `CHANGELOG.md`: novidade do sync inicial.
- [x] 3.3 `openspec validate sync-on-launch --strict` e marcar as caixas.
