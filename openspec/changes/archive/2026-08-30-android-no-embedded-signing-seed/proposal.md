## Why

`build.gradle.kts:22-25` injeta `ELISDB_PRIVATE_KEY` no `BuildConfig.SIGNER_PRIVATE_KEY_B64`. Se essa variável de ambiente estiver definida no build, uma seed privada de assinatura (a) é extraível do binário APK e (b) seria partilhada por todas as instalações. É um anti-pattern de segurança: a chave de dispositivo deve ser gerada/importada por dispositivo, nunca embutida.

## What Changes

- Garantir que a build NUNCA embute uma seed de assinatura real; o `BuildConfig` apenas contém uma seed quando explicitamente desativado em prod, e a app recusa usá-la para autenticação real.
- Preferir sempre a importação de chave por dispositivo (`client-key-import`); remover o caminho que consome `BuildConfig.SIGNER_PRIVATE_KEY_B64` para autenticação.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-key-import`: nova requirement "Signing key is never embedded in the build" — a app SHALL NOT ship or use a real signing seed baked into the APK; the signing key SHALL come from per-device import, and any build-time seed SHALL be ignored for authentication.

## Impact
- Android: `build.gradle.kts` (remover injeção de seed real), `SettingsStore`/`Signer` (ignorar seed do BuildConfig para auth).
- Sem alteração de protocolo.
