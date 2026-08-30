## 1. Importação de CA/compatibilidade nas definições

- [x] 1.1 Adicionar às definições a ação de importar um certificado/CA do servidor (PEM/DER)
- [x] 1.2 Armazenar o certificado de forma segura (ex.: num Keystore/SharedPreferences encriptados ou `res/raw` gerado) associado ao host configurado

## 2. TrustManager por host

- [x] 2.1 Construir o `OkHttp`/`HttpClient` com um `TrustManager` que inclui o CA do utilizador para o host do `server_url`, mantendo o system store para outros hosts
- [x] 2.2 Quando não há CA configurado, usar validação normal do sistema

## 3. Testes e verificação

- [x] 3.1 Testar que um servidor HTTPS self-signed com o CA importado é aceite
- [x] 3.2 Testar que sem CA (ou CA errado) a ligação é rejeitada
- [x] 3.3 Correr `./gradlew :app:assembleDebug` e testes da app
