# Tasks: Fix client log empty states

## 1. App — propagar erros do fetch

- [x] 1.1 Em `Navigation.kt`, remover o `try/catch { emptyList() }` do `fetchHistory` para que os erros de `fetchClientHistory` sejam propagados (o `runCatching`/`logError` do ViewModel passa a ser alcançável)

## 2. App — nota de alterações pendentes

- [x] 2.1 Em `ClientDetailViewModel`, expor `hasPending` (a partir do `cliente.pending`/`deletePending` do estado local) para o Log
- [x] 2.2 Em `ClientDetailScreen`, no diálogo do Log, quando `logEntries` está vazio: se `hasPending` → "Este cliente tem alterações por sincronizar; o histórico aparecerá após a sincronização."; senão → "Não há histórico registado para este cliente."; quando `logError` → "Não foi possível carregar o histórico do cliente."

## 3. Testes

- [x] 3.1 Atualizar testes do `ClientDetailViewModel` (ou adicionar) para o estado `hasPending` e o caminho de erro do `logError`
- [x] 3.2 Atualizar/adicionar teste de instrumentação do `ClientDetailScreen` para os estados: erro de fetch, histórico vazio com pending, histórico vazio sem pending

## 4. Verificação

- [x] 4.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 4.2 Verificação no emulador: criar um cliente e abrir o Log → mostra a nota de sincronização; sincronizar e abrir o Log → mostra o histórico; com rede/servidor indisponível → mostra o erro de carregamento
