# Proposal

## Why

A app abria sempre na lista de clientes, mesmo sem nenhuma credencial importada.
Nesse estado podia criar e editar registos locais que nunca chegavam ao servidor,
e só descobria o problema ao tentar sincronizar. Sem um utilizador configurado
também não é possível associar os dados a quem os criou. O arranque deve ser
explícito sobre a configuração em falta em vez de deixar a app a funcionar pela
metade.

## What Changes

- **Gate de arranque**: enquanto faltar o URL do servidor, a chave de assinatura
  ou o nome do utilizador, a app mostra o novo ecrã **Configuração necessária**,
  com uma explicação e o botão **Abrir configuração**. A navegação principal
  (lista, mapa, menu, Sobre e aviso de atualização) não é montada.
- **Configuração acessível e prioritária**: o ecrã de configuração existente
  abre a partir do setup e mantém prioridade depois de guardar a credencial, para
  a sincronização inicial arrancar. Sair da configuração leva à app principal (se
  a configuração estiver completa) ou de volta ao setup.
- **Reatividade**: `AppContainer` passa a expor a configuração atual como
  `StateFlow`; o `ServerConfigViewModel` notifica quando importa ou remove uma
  credencial, para o gate reagir sem reiniciar a app.
- **Credenciais antigas sem nome**: se existirem URL e chave mas faltar o
  `user_name` (ficheiros emitidos antes de o nome ser incluído), a app tenta
  obter o nome ao servidor (`GET /api/keys/me`) e guarda-o. Offline, o setup pede
  para reimportar o ficheiro.
- Sem alterações ao servidor, à API, à base de dados ou à versão da app.

## Capabilities

### New Capabilities

- `client-startup`: comportamento de arranque — exige uma configuração completa
  e mostra o ecrã de setup quando falta.

### Modified Capabilities

(nenhuma)

## Impact

- Android: `AppContainer` (estado da configuração e recuperação do nome),
  `ServerConfigViewModel` (callback de alteração), `Navigation.kt` (gate e
  configuração reutilizável), `ElisdbApp` (recuperação do nome), novo
  `SetupRequiredScreen`, testes unitários e instrumentados.
- Servidor: sem alterações; `/api/keys/me` já existe.
- Versão, CHANGELOG e assinatura: sem alterações.
