## 1. Remover uso de seed do build para auth

- [x] 1.1 Em `build.gradle.kts`, remover a injeção de `ELISDB_PRIVATE_KEY` em `BuildConfig` (ou mantê-la mas não a consumir para auth)
- [x] 1.2 No código de signing/`SettingsStore`, garantir que a única fonte de signing key é a chave importada
- [x] 1.3 Se não houver chave importada, o sync reporta configuration fault (não assina com seed embutida)

## 2. Testes e verificação

- [x] 2.1 Confirmar (inspeção) que nenhum caminho de auth consome `BuildConfig.SIGNER_PRIVATE_KEY_B64`
- [x] 2.2 Correr `./gradlew :app:assembleDebug` e testes da app
