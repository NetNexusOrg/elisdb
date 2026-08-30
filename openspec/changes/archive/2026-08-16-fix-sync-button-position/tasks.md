# Tasks: Fix sync button position

## 1. Layout — botão/ícone de sincronização estável

- [x] 1.1 Em `ui/settings/SyncStatusBar.kt`, restruturar a `Row` para dar à ação de sincronização um slot fixo no fim (trailing), independente da presença de mensagem de estado
- [x] 1.2 Colocar a zona de mensagem (ícone + texto) num segmento próprio, sempre alocado, confinado de modo a nunca empurrar a ação (ex.: texto com peso limitado ao seu segmento)
- [x] 1.3 Garantir que, no estado IDLE, o segmento da mensagem fica vazio e a ação mantém a mesma posição dos restantes estados
- [x] 1.4 Confirmar que o indicador de progresso (spinner) durante IN_PROGRESS aparece exatamente na posição da ação

## 2. Testes

- [x] 2.1 Em `SyncStatusBarTest.kt`, adicionar asserções de posição (não apenas visibilidade) para a ação nos estados IDLE, IN_PROGRESS, SUCCESS e ERROR (ex.: mesmo bordo esquerdo e ação à direita da mensagem)
- [x] 2.2 Correr os testes de instrumentação de `SyncStatusBar` e `SettingsScreen`; confirmar que passam

## 3. Verificação

- [x] 3.1 Verificação no emulador: abrir Definições (IDLE), tocar em sincronizar e confirmar que o botão/ícone não muda de posição durante IN_PROGRESS nem em SUCCESS/ERROR