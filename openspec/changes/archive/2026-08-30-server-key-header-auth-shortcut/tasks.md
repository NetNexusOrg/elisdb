## 1. Servidor: short-circuit por header

- [x] 1.1 Em `app/auth.py`, `require_auth` aceita o header `X-Elisdb-Key`; verifica só essa chave quando presente e válida, 401 se nomeia chave ausente
- [x] 1.2 Sem o header, mantém o fallback de iterar a allowlist

## 2. Android: enviar o header

- [x] 2.1 Em `remote/ApiClient.kt`/`util/Signer.kt`, incluir `X-Elisdb-Key: <keyId>` nos pedidos assinados (id vindo do `SettingsStore`)

## 3. Testes e verificação

- [x] 3.1 Em `test_main.py`, pedido com `X-Elisdb-Key` correto é aceite; com id errado é rejeitado; sem header continua a funcionar
- [x] 3.2 Correr `pytest` e `./gradlew :app:assembleDebug`
