# Tasks: Handle clock skew

## 1. Servidor — endpoint de tempo

- [x] 1.1 Em `app/main.py`, adicionar `GET /api/time` (sem autenticação) que devolve `{"time": <unix seconds>}`
- [x] 1.2 Em `test_main.py`, adicionar cenário: `GET /api/time` devolve um timestamp Unix e não requer auth

## 2. App — medir e persistir o offset

- [x] 2.1 Criar `ClockOffsetStore` (interface + implementação SharedPreferences) em `settings` (ou `sync`): `getOffsetSeconds(): Long?` e `setOffsetSeconds(Long)`
- [x] 2.2 Adicionar ao `ApiClient`/`SyncApi` um `fetchServerTime()` que chama `GET /api/time` e devolve o timestamp (sem assinatura)
- [x] 2.3 Atualizar `AppContainer` para criar e injetar o `ClockOffsetStore`

## 3. App — normalizar timestamps

- [x] 3.1 No `SyncCoordinator`, antes/depois de cada sync bem-sucedida, obter `fetchServerTime()` e persistir o offset
- [x] 3.2 No `SyncCoordinator` (e no caminho de escrita em `ClienteRepositoryImpl`), carimbar `updated_at` com `device_now + offset` quando o offset existe
- [x] 3.3 Usar o timestamp normalizado nos pedidos autenticados (push/pull/delete) — `(device_now + offset) / 1000`

## 4. App — stale visível

- [x] 4.1 Em `SyncCoordinator.pushEdits`, agregar os nomes dos registos com status `stale` e devolvê-los no outcome
- [x] 4.2 Em `SyncManager.mapOutcome`, renderizar `SuccessWithStatuses` com `stale` não vazio como mensagem visível (ex.: "A tua edição de X foi substituída por uma versão mais recente.")
- [x] 4.3 Atualizar testes unitários do `SyncCoordinator` e `SyncManager` para os cenários de offset e de stale visível

## 5. Verificação

- [x] 5.1 Correr `testDebugUnitTest` do Android e os testes pytest do servidor; confirmar que passam
- [x] 5.2 Verificação no emulador: sincronizar com o servidor e confirmar que (a) o sync continua a funcionar e (b) um registo `stale` apresenta a mensagem ao utilizador (simulável nos testes; no emulador confirmar o fluxo normal)
