## Why

`parseServerConfig` (`settings/ServerConfigDto.kt:22-31`) não valida o esquema do `server_url` importado. Um URL `http://` para um host que não é localhost passa na importação mas falha depois em runtime (o `network_security_config.xml` bloqueia cleartext exceto para `10.0.2.2`/localhost). O utilizador importa um config inválido e só descobre o erro na próxima sync.

## What Changes

- Validar o esquema do `server_url` na importação: aceitar `https://` para qualquer host, ou `http://` apenas para hosts locais (localhost/10.0.2.2/127.0.0.1); rejeitar outros esquemas/hosts com mensagem clara.
- Isto antecipa o erro para o momento da importação.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-key-import`: a requirement "Import a server configuration file" passa a validar o esquema do `server_url` e a rejeitar URLs claramente inválidos (ex.: `http://` para host não-local) no momento da importação.

## Impact
- Android: `settings/ServerConfigDto.kt` (validação de esquema/host). Sem alteração de protocolo.
- Testes: importar config com `http://` para host remoto é rejeitado; `https://` aceite.
