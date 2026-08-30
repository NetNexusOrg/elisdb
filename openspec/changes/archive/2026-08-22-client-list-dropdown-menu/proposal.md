## Why

Na lista de clientes, as ações "criar novo cliente" e "definições" aparecem como dois ícones separados na barra superior, ocupando espaço e poluindo visualmente a barra de título. Faz sentido agrupá-las num menu suspenso (overflow), como já acontece no ecrã de detalhe.

## What Changes

- Na barra superior da lista de clientes, os ícones de "criar novo cliente" e "definições" passam a estar num menu suspenso único (ícone de três pontos), em vez de dois ícones separados.
- O menu apresenta as opções "Criar novo cliente" e "Definições".

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `client-screens`: o requisito "Client list screen" (descrição e cenário "Top bar hosts the screen actions") e o requisito "Add a new client" passam a referir o menu suspenso em vez de ícones diretos.

## Impact

- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt`: substituir os dois `IconButton` (Settings/Add) por um `IconButton` de overflow com `DropdownMenu` + `DropdownMenuItem`s ("Criar novo cliente", "Definições").
- Sem novas dependências (Material3 já inclui `DropdownMenu`).
- Testes: ajustar teste instrumentado de `ClientListScreen` se existir (ex. abertura do ecrã de definições/criação).
