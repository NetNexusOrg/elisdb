## Why

Quando o utilizador importa a chave emitida pelo servidor, a app deve mostrar o nome de utilizador associado a essa chave — para o utilizador confirmar que está a usar a identidade certa. O servidor passa a expor essa identidade via `GET /api/keys/me` (proposta `server-key-user-association`); a app precisa de a consultar e apresentar.

## What Changes

- **App:** após (re)importar ou guardar a chave, consulta `GET /api/keys/me` no servidor configurado e mostra o username associado na ecrã de definições.
- **App:** o username é apresentado fiel ao servidor — preenchido quando o servidor o devolve, vazio quando não há identidade associada.
- **App:** estado da UI: `username` populado / vazio (nenhuma identidade) / erro de consulta tratado com falha silenciosa (a consulta é informativa, nunca bloqueia a utilização da chave).
- Não altera o fluxo de import do ficheiro nem o mask de exibição da chave.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `client-key-import`: "Import a server-issued key" — após importar uma chave, a app consulta e apresenta o utilizador associado a essa chave.

## Impact

- `app/src/main/java/com/example/elisdb/remote/ApiClient.kt` — novo método `fetchKeyIdentity()` (GET `/api/keys/me` autenticado) que devolve `{key_id, username}`.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt` — após o import/guardar, dispara a consulta e atualiza o estado com o username (ou ausência dele); nunca bloqueia o save.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — apresentar o nome de utilizador associado junto da chave importada.
- Depende do servidor com o endpoint novo (proposta `server-key-user-association`); enquanto ausente, a consulta falha e a app ignora silenciosamente (nenhuma regressão).
- Testes android para a consulta de identidade.