## 1. Registo único do provider

- [x] 1.1 Em `util/Signer.kt`, registar o `EdDSASecurityProvider` uma única vez (guarda com flag/`synchronized` ou `companion object`), em vez de em cada construção
- [x] 1.2 Manter o `Signer` funcional para assinar/verificar como antes

## 2. Verificação

- [x] 2.1 Testar que construir vários `Signer` não duplica o provider na lista estática
- [x] 2.2 Correr testes da app / `./gradlew :app:assembleDebug`
