# Tasks

## 1. Remover o aviso

- [x] 1.1 Tirar o aviso do cartão de sincronização e simplificar `NoticeBanner`.
- [x] 1.2 Remover `serverOutdated` de `SyncManager` e `SyncUiState`; manter `ServerInfo.apiVersion` para diagnóstico.
- [x] 1.3 Remover os testes do aviso e da flag; manter o teste do mapeamento de `api_version`.

## 2. Verificação e documentação

- [x] 2.1 `:app:testDebugUnitTest`, `:app:lintDebug` e `ConfigScreenTest` verdes.
- [x] 2.2 Atualizar `docs/uso.md`, `docs/configuracao.md` e `CHANGELOG.md`.
- [x] 2.3 `openspec validate drop-outdated-warning --strict`.
