# Tasks: Log timestamps in 24h

## 1. App — formatação 24h

- [x] 1.1 Em `ui/detalhe/ClientDetailScreen.kt`, no `LogEntries`, substituir `DateFormat.getDateTimeInstance(MEDIUM, SHORT)` por um `SimpleDateFormat` explícito com padrão 24h (ex. `dd/MM/yyyy HH:mm`), mantendo a data legível
- [x] 1.2 Remover/ajustar imports desnecessários (`DateFormat` se deixar de ser usado)

## 2. Testes

- [x] 2.1 Adicionar teste (unitário ou instrumentação do `ClientDetailScreen`) que verifica que o timestamp é apresentado em 24h (sem marcador AM/PM)

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: abrir o Log de um cliente e confirmar que a hora aparece em formato 24h
