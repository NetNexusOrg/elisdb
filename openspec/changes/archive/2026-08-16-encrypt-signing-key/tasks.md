# Tasks: Encrypt signing key

## 1. App — dependência

- [x] 1.1 Em `gradle/libs.versions.toml` e `app/build.gradle.kts`, adicionar `androidx.security:security-crypto`

## 2. App — armazenamento encriptado

- [x] 2.1 Em `settings/SettingsStore.kt`, `PrefsSettingsStore` passa a usar `EncryptedSharedPreferences` (com `MasterKey`/Keystore)
- [x] 2.2 Migrar valores antigos: se o ficheiro plain antigo tiver valores, lê-los e gravá-los no store encriptado; limpar o ficheiro plain

## 3. Testes

- [x] 3.1 Manter `SettingsStoreTest`/`SettingsViewModelTest` a passar (usam a mesma interface)
- [x] 3.2 Adicionar teste de migração: valores plain antigos aparecem no store encriptado e o plain é limpo

## 4. Verificação

- [x] 4.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 4.2 Verificação no emulador: importar uma config, confirmar que continua a funcionar; verificar que o ficheiro de prefs não contém a chave em texto simples
