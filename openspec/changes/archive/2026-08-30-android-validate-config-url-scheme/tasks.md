## 1. Validar esquema na importação

- [x] 1.1 Em `settings/ServerConfigDto.kt`, `parseServerConfig` valida o esquema/host do `server_url`: `https` aceite; `http` apenas para localhost/127.0.0.1/10.0.2.2
- [x] 1.2 Esquema/host inválido devolve erro de validação (não silent)
- [x] 1.3 A UI recusa o import com mensagem clara quando o esquema é inválido

## 2. Testes e verificação

- [x] 2.1 Testar que `http://` para host remoto é rejeitado na importação; `https://` e `http://localhost` aceites
- [x] 2.2 Correr testes da app / `./gradlew :app:assembleDebug`
