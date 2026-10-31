# Tasks

## 1. Servidor

- [x] 1.1 `API_VERSION = 2` em `app/main.py`, devolvido em `/health`.
- [x] 1.2 `/api/latest` devolve `api_version` a par de `latest_updated_at` e `client_count`.
- [x] 1.3 Testes do servidor para os dois endpoints.

## 2. Android

- [x] 2.1 `ServerInfo.apiVersion` (nullable) e `MIN_SERVER_API_VERSION`; mapear `api_version` no `ApiClient`.
- [x] 2.2 `SyncManager.serverOutdated` atualizado em `refreshPendingDownloads` e limpo em `resetState`.
- [x] 2.3 `SyncUiState.serverOutdated` e aviso no cartão de sincronização da Configuração.
- [x] 2.4 Testes: mapeamento no `ApiClient`, flag no `SyncManager`, aviso no ecrã.

## 3. Verificação

- [x] 3.1 `pytest`, `:app:testDebugUnitTest`, `:app:lintDebug`, `ConfigScreenTest` e `openspec validate --specs`.
- [x] 3.2 Documentação (`elisdb-server/docs/configuracao.md`, `docs/uso.md`, `CHANGELOG.md`).
