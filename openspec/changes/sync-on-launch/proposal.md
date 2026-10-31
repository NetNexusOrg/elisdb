# Proposal

## Why

Quem abre a app depois de ter estado offline continua a ver dados antigos até
tocar em “Sincronizar”. Uma sincronização inicial no arranque traz alterações
feitas noutros dispositivos e envia o que ficou pendente, sem exigir um gesto
extra.

## What Changes

- Ao abrir a app, é iniciada uma sincronização em segundo plano, uma vez por
  arranque do processo.
- A sincronização inicial só corre quando existe configuração de servidor
  (chave de assinatura); sem ela nada é transferido e não aparece erro.
- Continua a não haver sincronização por temporizador; o sync deixa de ser
  apenas manual e passa a ser acionado também pelo arranque e por guardar um
  cliente.
- Falhas aparecem no estado de sincronização habitual, sem bloquear a app.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-sync`: o requisito que limitava as transferências a ações do
  utilizador passa a incluir o arranque da app como gatilho, mantendo a proibição
  de sincronização periódica.

## Impact

- Android: `ElisdbApp` no arranque, com um gate pela configuração existente.
- Sem alterações no servidor, na base de dados ou no protocolo.
- Sem novo WorkManager nem alarmes: o gatilho é o próprio arranque.
