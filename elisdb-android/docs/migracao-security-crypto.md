# Migração do security-crypto para Android Keystore

## Estado

Implementada. O `androidx.security:security-crypto` deixou de ser usado para
guardar a configuração; os valores passam a ser cifrados diretamente com uma
chave AES-256-GCM no Android Keystore.

- Implementação: `settings/KeystoreSettingsStore.kt`.
- Chave: alias `elisdb_settings_aes_v1` no `AndroidKeyStore`
  (`AES/GCM/NoPadding`, 256 bits, IV aleatório por valor).
- Preferências: ficheiro `elisdb_settings_keystore`.
- Formato de cada valor: `v1:<base64(iv)>:<base64(ciphertext)>`.

A dependência `security-crypto` continua apenas para **leitura legacy** durante
a janela de migração; pode ser removida quando não houver instalações antigas.

## Migração

No arranque, o `KeystoreSettingsStore`:

1. lê os valores já guardados no novo formato, se existirem;
2. caso contrário, lê os valores antigos (texto simples primeiro e depois o
   `EncryptedSharedPreferences` original);
3. cifra tudo com a chave do Keystore, grava com `commit = true` e confirma por
   leitura;
4. só depois de a confirmação passar é que remove as cópias antigas.

Qualquer falha (Keystore indisponível, chave invalidada, erro de I/O) mantém os
dados antigos intactos e legíveis. Nesse estado, `settings()` continua a
funcionar em leitura, mas `save()` lança `SecureStorageUnavailable` — nunca
escreve a chave em texto simples.

A migração é idempotente: correr outra vez não altera nem duplica dados.

## Riscos residuais

- **Invalidação da chave do Keystore** (alteração de bloqueio do ecrã, OEM,
  restauro de dispositivo): os valores novos deixam de decifrar. É necessário
  reimportar o ficheiro `<utilizador>.config.json`.
- **Transferência direta entre dispositivos**: `allowBackup=false` desativa o
  backup cloud, mas alguns OEMs permitem D2D; a chave do Keystore não viaja, pelo
  que o destino pede reimportação.
- **Downgrade da app**: versões antigas não leem o novo formato. Não há suporte
  a downgrade; a configuração tem de ser reimportada.
- A chave privada ed25519 continua a ser o dado mais sensível: nunca deve ser
  registada em logs, exports ou relatórios.

## Testes

`app/src/androidTest/.../settings/KeystoreSettingsStoreTest.kt` cobre:

- roundtrip de gravação/leitura;
- preferências novas sem a seed em claro;
- migração de valores antigos em texto simples, com limpeza só após confirmação;
- migração do `EncryptedSharedPreferences` antigo;
- falha do Keystore: leitura legacy mantém-se e `save()` falha fechado.
