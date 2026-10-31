# Tasks

## 1. Ack por revisão

- [x] 1.1 `Cliente.revision` + `MIGRATION_8_9` (BD para 9, schema exportado) e incremento no `ClienteRepositoryImpl.save`/`deleteCliente`.
- [x] 1.2 `ClienteDao.markSynced(id, seenRevision)` e uso no `SyncCoordinator` (ramos `stale` e `accepted`), com aviso quando o ack não se aplica.
- [x] 1.3 Testes: edição durante o push preservada, ack aplicado quando nada muda, `FakeClienteDao` atualizado.

## 2. Watermark com limite

- [x] 2.1 `SyncCoordinator.pull`: só seguram o watermark os pendentes alterados nos últimos 7 dias.
- [x] 2.2 Testes: pendente recente segura; pendente antigo deixa avançar e continua pendente.

## 3. Cliente apagado

- [x] 3.1 `ClienteRepositoryImpl.save` falha explicitamente quando o registo desapareceu.
- [x] 3.2 `ClientFormViewModel` mostra a mensagem e não emite `saved`; `_saved` com buffer e `saving` sempre limpo.
- [x] 3.3 Teste do repositório e do view model.

## 4. Estado ao mudar de servidor

- [x] 4.1 `SyncMetaDao.clear()`, `PhotoPushState.clearAll()`.
- [x] 4.2 `ServerConfigViewModel.importKey`/`clearConfig` limpam watermark, hashes e offset.
- [x] 4.3 Testes do view model (estado limpo, registos mantidos).

## 5. Relógio nas edições

- [x] 5.1 `AppContainer` passa `clockOffsetStore` ao `ClienteRepositoryImpl`.
- [x] 5.2 Teste do repositório com offset não nulo.

## 6. Migração do seed

- [x] 6.1 `SettingsStore`: gravar/verificar cifrado antes de limpar o texto simples; falha não propaga.
- [x] 6.2 Teste da migração com falha simulada.

## 7. Timeouts

- [x] 7.1 `ApiClient.downloadRelease` com 10 min; fotos com 2 min.

## 8. Ajustes

- [x] 8.1 Fotos ordenadas por `ordem` na lista, detalhe e visualizador.
- [x] 8.2 `statusBarsPadding()` no visualizador e no mapa em fullscreen.
- [x] 8.3 Ciclo de vida do `OsmMap` com `LifecycleEventObserver`.

## 9. Verificação

- [x] 9.1 `:app:testDebugUnitTest`, `:app:lintDebug`, instrumentados da migração 8→9.
- [x] 9.2 `openspec validate client-resilience --strict`.
