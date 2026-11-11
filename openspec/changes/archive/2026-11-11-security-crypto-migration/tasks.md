# Tasks

## 1. Implementação

- [x] 1.1 Criar `KeystoreSettingsStore` com AES-256-GCM no Android Keystore.
- [x] 1.2 Migrar valores legados (plaintext e `EncryptedSharedPreferences`) com verificação antes de limpar.
- [x] 1.3 Ligar `AppContainer` ao novo store.
- [x] 1.4 Garantir escrita fail-closed (`SecureStorageUnavailable`).

## 2. Testes

- [x] 2.1 Adicionar `KeystoreSettingsStoreTest` (roundtrip, cifra em repouso, migração plain, migração encrypted, falha do Keystore).
- [x] 2.2 Compilar e correr os testes instrumentados no emulador.

## 3. Documentação e fecho

- [x] 3.1 Reescrever `docs/migracao-security-crypto.md` para o estado implementado.
- [x] 3.2 Atualizar as skills `android-development` e `security-review`.
- [x] 3.3 Validar a change com `openspec validate --strict`.
- [x] 3.4 Arquivar após revisão e commit aprovado pelo utilizador.

> Commits e push apenas com confirmação explícita do utilizador.
