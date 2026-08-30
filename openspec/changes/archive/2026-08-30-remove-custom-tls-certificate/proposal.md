## Why

A app permite importar um certificado/CA HTTPS auto-hospedado para confiar num servidor com certificado self-signed. Essa funcionalidade não interessa e adiciona complexidade desnecessária. Além disso, hoje a importação rejeita URLs `http` para hosts não-locais, obrigando o utilizador a usar HTTPS com certificado público ou a importar um certificado — o utilizador deve poder usar simplesmente HTTP com um domínio ou um endereço IP.

## What Changes

- Remover a funcionalidade de importar e usar um certificado/CA HTTPS auto-hospedado (confiança customizada no cliente TLS).
- O utilizador deixa de ter qualquer ação de importar/remover certificado nas definições; a app passa a validar o certificado do servidor apenas contra o armazenamento de confiança do sistema (comportamento padrão).
- Relaxar a validação do `server_url` na importação da configuração: passa a ser aceite `http` para qualquer host (domínio ou IP), e `https` para qualquer host; apenas esquemas que não sejam `http`/`https` são rejeitados.
- Permitir tráfego em claro (HTTP) para qualquer host na configuração de segurança de rede, de modo a que o servidor possa ser alcançado por domínio ou IP sem TLS.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-key-import`: a validação do `server_url` passa a aceitar `http` para qualquer host (domínio ou IP), não apenas hosts locais.
- `client-tls`: a capability é removida na íntegra (deixa de existir importação/confiança de certificado customizado).

## Impact

- `app/src/main/java/com/example/elisdb/settings/SettingsStore.kt`: remover `caCertPem` de `ServerSettings`, os métodos `caCertPem()`/`saveCaCert()` e a chave persistida.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt`: remover `hasCaCert`, `importCaCert()`, `clearCaCert()` e o campo `message` associado.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt`: remover o cartão "Certificado do servidor (HTTPS auto-hospedado)" e o launcher de importação de certificado.
- `app/src/main/java/com/example/elisdb/remote/ApiClient.kt`: remover o parâmetro `caCertPem`, `buildTrustingSslSocketFactory()` e imports associados.
- `app/src/main/java/com/example/elisdb/remote/ApiClientProvider.kt` e `AppContainer.kt`: deixar de propagar `caCertPem`.
- `app/src/main/java/com/example/elisdb/settings/ServerConfigDto.kt`: aceitar `http`/`https` para qualquer host.
- `app/src/main/res/xml/network_security_config.xml`: permitir cleartext para todos os hosts.
- Testes: ajustar `FakeSettingsStore` e remover/atualizar testes que cubram o certificado e a validação de URL.
- Sem novas dependências.
