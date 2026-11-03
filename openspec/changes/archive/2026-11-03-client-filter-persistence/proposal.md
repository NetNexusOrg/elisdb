# Proposal

## Why

Os filtros da lista de clientes, a ordenação da lista e o filtro do mapa de clientes vivem apenas em memória. Ao reiniciar a app, voltam sempre aos valores por omissão, obrigando o utilizador a repetir a mesma escolha.

## What Changes

- Adicionar um armazenamento de preferências de UI (`ClientViewPreferences`) baseado em `SharedPreferences`, sem base de dados nem dependências novas.
- Memorizar o filtro de estado da lista de clientes.
- Memorizar a ordenação da lista de clientes.
- Memorizar o filtro do mapa de clientes.
- Manter os filtros da lista e do mapa independentes.
- Manter a pesquisa da lista transitória (não é memorizada).
- Usar os valores atuais como omissão quando ainda não existe preferência guardada.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `client-screens`: a lista de clientes passa a restaurar o filtro de estado e a ordenação guardados.
- `client-map`: o mapa de clientes passa a restaurar o filtro guardado.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/cliente/ClientViewPreferences.kt` (novo)
- `ClientListViewModel.kt`, `ClientMapViewModel.kt`
- `AppContainer.kt`, `Navigation.kt`
- Testes unitários dos dois ViewModels
- Sem alterações ao servidor, base de dados, sincronização ou API.
