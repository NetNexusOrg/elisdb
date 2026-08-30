## Why

`Signer.init` (`util/Signer.kt:27`) chama `Security.addProvider(EdDSASecurityProvider())` sempre que um `Signer` é construído. Um `Signer` é construído por sync (e por fetch de identidade/histórico), pelo que o provider é adicionado repetidamente à lista estática `Security`, acumulando duplicados ao longo da vida do processo.

## What Changes

- Registar o `EdDSASecurityProvider` uma única vez (lazy singleton ou guarda com flag), em vez de o adicionar em cada construção do `Signer`.

## Capabilities

### New Capabilities
- `client-signing`: nova capability — o app SHALL register the EdDSA security provider exactly once for the process, so repeated signing operations do not accumulate duplicate providers.

### Modified Capabilities

## Impact
- Android: `util/Signer.kt` (registo único do provider). Sem alteração de protocolo nem de comportamento de assinatura.
- Testes: testar que múltiplos `Signer` não duplicam providers.
